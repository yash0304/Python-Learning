package com.yash.gyaanly.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yash.gyaanly.GyaanlyApp
import com.yash.gyaanly.data.Book
import com.yash.gyaanly.data.Lesson
import com.yash.gyaanly.data.Progress
import com.yash.gyaanly.data.ProgressCodec
import com.yash.gyaanly.data.Track
import com.yash.gyaanly.data.TrackContent
import com.yash.gyaanly.domain.DailyPlanner
import com.yash.gyaanly.notify.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class TodayState(
    /** Today's lesson per track, in track order. */
    val plan: List<Pair<Track, Lesson>> = emptyList(),
    val streak: Int = 0,
    val progress: Progress = Progress(),
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GyaanlyApp
    private val content = app.content
    private val store = app.progress

    val tracks: List<Track> = content.tracks

    val progress: StateFlow<Progress> = store.state

    val today: StateFlow<TodayState> = store.state
        .map { p -> TodayState(planFor(p), DailyPlanner.streak(p.activeDays, LocalDate.now()), p) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TodayState())

    init {
        refreshPlan()
    }

    fun pack(key: String): TrackContent? = content.pack(key)

    fun lesson(id: String): Lesson? = content.lesson(id)

    fun book(key: String): Book? = content.book(key)

    fun bookEpub(key: String): ByteArray? = content.bookEpub(key)

    /** Fixes today's lesson per track so it doesn't change after you complete it. */
    fun refreshPlan() {
        val todayStr = LocalDate.now().toString()
        store.update { p ->
            val keep = if (p.planDate == todayStr) p.plan else emptyMap()
            val plan = tracks.associate { track ->
                val existing = keep[track.key]?.takeIf { content.lesson(it) != null }
                track.key to (existing ?: pickFor(track.key, p, p.completed)?.id.orEmpty())
            }
            p.copy(planDate = todayStr, plan = plan)
        }
    }

    private fun planFor(p: Progress): List<Pair<Track, Lesson>> =
        tracks.mapNotNull { t -> p.plan[t.key]?.let(content::lesson)?.let { t to it } }

    private fun pickFor(key: String, p: Progress, exclude: Set<String>): Lesson? {
        val pack = content.pack(key) ?: return null
        val minLevel = if (pack.track.levelPicker) p.levels[key] else null
        return DailyPlanner.pick(pack.lessons, pack.track.levels, exclude, LocalDate.now(), minLevel)
    }

    /** Records completion; returns the XP earned by this attempt. */
    fun complete(lessonId: String, correct: Int, total: Int): Int {
        val todayStr = LocalDate.now().toString()
        val before = store.state.value.xp
        store.update { p ->
            val scores = if (total > 0) {
                val best = p.quizScores[lessonId]?.substringBefore("/")?.toIntOrNull() ?: -1
                if (correct > best) p.quizScores + (lessonId to "$correct/$total") else p.quizScores
            } else p.quizScores
            p.copy(completed = p.completed + lessonId, activeDays = p.activeDays + todayStr, quizScores = scores)
        }
        return store.state.value.xp - before
    }

    /** Swap today's lesson for a track with the next unfinished one. */
    fun nextLesson(key: String) {
        store.update { p ->
            val current = p.plan[key]
            val next = pickFor(key, p, p.completed + listOfNotNull(current))
            p.copy(plan = p.plan + (key to (next?.id ?: current.orEmpty())))
        }
    }

    fun setLevel(key: String, level: String) {
        store.update { p ->
            val updated = p.copy(levels = p.levels + (key to level))
            // Re-pick only this track; other tracks keep today's lesson.
            updated.copy(plan = updated.plan + (key to pickFor(key, updated, updated.completed)?.id.orEmpty()))
        }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        store.update { it.copy(reminderEnabled = enabled, reminderHour = hour, reminderMinute = minute) }
        if (enabled) ReminderScheduler.schedule(app, hour, minute) else ReminderScheduler.cancel(app)
    }

    fun exportProgress(): String = ProgressCodec.encode(store.state.value)

    /** Replaces progress with a backup. Returns false if the file isn't a valid backup. */
    fun importProgress(json: String): Boolean {
        val restored = runCatching { ProgressCodec.decode(json) }.getOrElse { return false }
        store.update { restored }
        if (restored.reminderEnabled) ReminderScheduler.schedule(app, restored.reminderHour, restored.reminderMinute)
        refreshPlan()
        return true
    }

    fun resetProgress() {
        store.update {
            Progress(levels = it.levels, reminderEnabled = it.reminderEnabled, reminderHour = it.reminderHour, reminderMinute = it.reminderMinute)
        }
        refreshPlan()
    }
}

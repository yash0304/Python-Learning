package com.yash.dailyedge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yash.dailyedge.DailyEdgeApp
import com.yash.dailyedge.data.Lesson
import com.yash.dailyedge.data.Progress
import com.yash.dailyedge.data.Track
import com.yash.dailyedge.data.TrackContent
import com.yash.dailyedge.domain.DailyPlanner
import com.yash.dailyedge.notify.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class TodayState(
    val plan: Map<Track, Lesson> = emptyMap(),
    val streak: Int = 0,
    val progress: Progress = Progress(),
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DailyEdgeApp
    private val content = app.content
    private val store = app.progress

    val progress: StateFlow<Progress> = store.state

    val today: StateFlow<TodayState> = store.state
        .map { p -> TodayState(planFor(p), DailyPlanner.streak(p.activeDays, LocalDate.now()), p) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TodayState())

    init {
        refreshPlan()
    }

    fun pack(track: Track): TrackContent = content.pack(track)

    fun lesson(id: String): Lesson? = content.lesson(id)

    /** Fixes today's lesson per track so it doesn't change after you complete it. */
    fun refreshPlan() {
        val todayStr = LocalDate.now().toString()
        store.update { p ->
            val keep = if (p.planDate == todayStr) p.plan else emptyMap()
            val plan = Track.entries.associate { track ->
                val existing = keep[track.key]?.takeIf { content.lesson(it) != null }
                track.key to (existing ?: pickFor(track, p)?.id.orEmpty())
            }
            p.copy(planDate = todayStr, plan = plan)
        }
    }

    private fun planFor(p: Progress): Map<Track, Lesson> =
        Track.entries.mapNotNull { t -> p.plan[t.key]?.let(content::lesson)?.let { t to it } }.toMap()

    private fun pickFor(track: Track, p: Progress): Lesson? {
        val pack = content.pack(track)
        val minLevel = if (track == Track.ENGLISH) p.englishLevel else null
        return DailyPlanner.pick(pack.lessons, pack.levels, p.completed, LocalDate.now(), minLevel)
    }

    fun complete(lessonId: String, correct: Int, total: Int) {
        val todayStr = LocalDate.now().toString()
        store.update { p ->
            val scores = if (total > 0) {
                val best = p.quizScores[lessonId]?.substringBefore("/")?.toIntOrNull() ?: -1
                if (correct > best) p.quizScores + (lessonId to "$correct/$total") else p.quizScores
            } else p.quizScores
            p.copy(completed = p.completed + lessonId, activeDays = p.activeDays + todayStr, quizScores = scores)
        }
    }

    /** Skip today's lesson for a track and pick the next unfinished one. */
    fun nextLesson(track: Track) {
        store.update { p ->
            val current = p.plan[track.key]
            val pack = content.pack(track)
            val minLevel = if (track == Track.ENGLISH) p.englishLevel else null
            val next = DailyPlanner.pick(
                pack.lessons, pack.levels, p.completed + listOfNotNull(current), LocalDate.now(), minLevel,
            )
            p.copy(plan = p.plan + (track.key to (next?.id ?: current.orEmpty())))
        }
    }

    fun setEnglishLevel(level: String) {
        store.update { p -> p.copy(englishLevel = level, plan = p.plan - Track.ENGLISH.key) }
        // Re-pick only English; other tracks keep today's lesson.
        store.update { p ->
            p.copy(plan = p.plan + (Track.ENGLISH.key to pickFor(Track.ENGLISH, p)?.id.orEmpty()))
        }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        store.update { it.copy(reminderEnabled = enabled, reminderHour = hour, reminderMinute = minute) }
        if (enabled) ReminderScheduler.schedule(app, hour, minute) else ReminderScheduler.cancel(app)
    }

    fun resetProgress() {
        store.update { Progress(englishLevel = it.englishLevel, reminderEnabled = it.reminderEnabled, reminderHour = it.reminderHour, reminderMinute = it.reminderMinute) }
        refreshPlan()
    }
}

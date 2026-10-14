package com.yash.gyaanly.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small SharedPreferences-backed store exposing progress as a StateFlow. */
class ProgressStore(context: Context) {

    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Progress> = _state.asStateFlow()

    fun update(transform: (Progress) -> Progress) {
        val next = transform(_state.value)
        save(next)
        _state.value = next
    }

    private fun load(): Progress {
        val levels = decodeMap(prefs.getString(KEY_LEVELS, "")!!).toMutableMap()
        // Version 1 stored only the English level.
        prefs.getString(LEGACY_ENGLISH_LEVEL, null)?.let { levels.putIfAbsent("english", it) }
        return Progress(
            completed = prefs.getStringSet(KEY_COMPLETED, emptySet())!!.toSet(),
            activeDays = prefs.getStringSet(KEY_DAYS, emptySet())!!.toSet(),
            quizScores = decodeMap(prefs.getString(KEY_SCORES, "")!!),
            planDate = prefs.getString(KEY_PLAN_DATE, "")!!,
            plan = decodeMap(prefs.getString(KEY_PLAN, "")!!),
            levels = levels,
            reminderEnabled = prefs.getBoolean(KEY_REMINDER, false),
            reminderHour = prefs.getInt(KEY_HOUR, 8),
            reminderMinute = prefs.getInt(KEY_MINUTE, 0),
        )
    }

    private fun save(p: Progress) {
        prefs.edit()
            .putStringSet(KEY_COMPLETED, p.completed)
            .putStringSet(KEY_DAYS, p.activeDays)
            .putString(KEY_SCORES, encodeMap(p.quizScores))
            .putString(KEY_PLAN_DATE, p.planDate)
            .putString(KEY_PLAN, encodeMap(p.plan))
            .putString(KEY_LEVELS, encodeMap(p.levels))
            .remove(LEGACY_ENGLISH_LEVEL)
            .putBoolean(KEY_REMINDER, p.reminderEnabled)
            .putInt(KEY_HOUR, p.reminderHour)
            .putInt(KEY_MINUTE, p.reminderMinute)
            .apply()
    }

    // Keys and values are ids/levels/scores without '=' or ';', so a flat encoding is enough.
    private fun encodeMap(map: Map<String, String>) = map.entries.joinToString(";") { "${it.key}=${it.value}" }

    private fun decodeMap(raw: String): Map<String, String> =
        raw.split(";").filter { "=" in it }.associate { it.substringBefore("=") to it.substringAfter("=") }

    private companion object {
        const val KEY_COMPLETED = "completed"
        const val KEY_DAYS = "active_days"
        const val KEY_SCORES = "quiz_scores"
        const val KEY_PLAN_DATE = "plan_date"
        const val KEY_PLAN = "plan"
        const val KEY_LEVELS = "levels"
        const val LEGACY_ENGLISH_LEVEL = "english_level"
        const val KEY_REMINDER = "reminder_enabled"
        const val KEY_HOUR = "reminder_hour"
        const val KEY_MINUTE = "reminder_minute"
    }
}

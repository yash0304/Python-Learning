package com.yash.gyaanly.domain

import com.yash.gyaanly.data.Lesson
import java.time.LocalDate

object DailyPlanner {

    /**
     * Picks the lesson to study next for one track: the first unfinished lesson at or above
     * [minLevel] (used for the English CEFR level), then any unfinished lesson, and once
     * everything is done it cycles through the pack for revision, one lesson per day.
     */
    fun pick(
        lessons: List<Lesson>,
        levels: List<String>,
        completed: Set<String>,
        today: LocalDate,
        minLevel: String? = null,
    ): Lesson? {
        if (lessons.isEmpty()) return null
        val minIndex = minLevel?.let { levels.indexOf(it) }?.coerceAtLeast(0) ?: 0
        val open = lessons.filter { it.id !in completed }
        return open.firstOrNull { levels.indexOf(it.level) >= minIndex }
            ?: open.firstOrNull()
            ?: lessons[(today.toEpochDay() % lessons.size).toInt()]
    }

    /** Consecutive active days ending today, or yesterday if today has no activity yet. */
    fun streak(activeDays: Set<String>, today: LocalDate): Int {
        var day = if (today.toString() in activeDays) today else today.minusDays(1)
        var count = 0
        while (day.toString() in activeDays) {
            count++
            day = day.minusDays(1)
        }
        return count
    }
}

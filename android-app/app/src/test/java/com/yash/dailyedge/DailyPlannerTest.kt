package com.yash.dailyedge

import com.yash.dailyedge.data.Lesson
import com.yash.dailyedge.data.Track
import com.yash.dailyedge.domain.DailyPlanner
import com.yash.dailyedge.domain.SpeechMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyPlannerTest {

    private fun lesson(id: String, level: String) = Lesson(
        id, Track.ENGLISH, level, id, "", emptyList(), null, null, null, emptyList(), null, null, emptyList(),
    )

    private val levels = listOf("A1", "A2", "B1")
    private val lessons = listOf(lesson("a1-1", "A1"), lesson("a1-2", "A1"), lesson("a2-1", "A2"), lesson("b1-1", "B1"))
    private val today = LocalDate.of(2026, 10, 8)

    @Test
    fun picksFirstUnfinished() {
        assertEquals("a1-2", DailyPlanner.pick(lessons, levels, setOf("a1-1"), today)?.id)
    }

    @Test
    fun respectsMinimumLevel() {
        assertEquals("a2-1", DailyPlanner.pick(lessons, levels, emptySet(), today, minLevel = "A2")?.id)
    }

    @Test
    fun fallsBackToLowerLevelsWhenHigherAreDone() {
        assertEquals("a1-1", DailyPlanner.pick(lessons, levels, setOf("a2-1", "b1-1"), today, minLevel = "A2")?.id)
    }

    @Test
    fun cyclesForRevisionWhenAllDone() {
        val all = lessons.map { it.id }.toSet()
        assertTrue(DailyPlanner.pick(lessons, levels, all, today)!!.id in all)
    }

    @Test
    fun streakCountsConsecutiveDays() {
        val days = setOf("2026-10-08", "2026-10-07", "2026-10-06", "2026-10-04")
        assertEquals(3, DailyPlanner.streak(days, today))
        // Not studied yet today: yesterday's streak still counts.
        assertEquals(2, DailyPlanner.streak(setOf("2026-10-07", "2026-10-06"), today))
        assertEquals(0, DailyPlanner.streak(setOf("2026-10-05"), today))
    }

    @Test
    fun speechMatchScoresAndReportsMissedWords() {
        val perfect = SpeechMatch.compare("Could you share the agenda?", "could you share the agenda")
        assertEquals(100, perfect.score)
        val partial = SpeechMatch.compare("Could you share the agenda?", "could you share agenda")
        assertTrue(partial.score in 80..99)
        assertEquals(listOf("the"), partial.missed)
    }
}

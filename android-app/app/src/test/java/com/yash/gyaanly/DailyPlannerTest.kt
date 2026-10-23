package com.yash.gyaanly

import com.yash.gyaanly.data.Lesson
import com.yash.gyaanly.data.Track
import com.yash.gyaanly.domain.DailyPlanner
import com.yash.gyaanly.domain.SpeechMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyPlannerTest {

    private val levels = listOf("A1", "A2", "B1")
    private val track = Track("english", "English", "", "🗣️", 0xFF000000, 1, true, emptyList(), levels)

    private fun lesson(id: String, level: String) = Lesson(
        id, track, level, id, "", emptyList(), null, null, null, emptyList(), "en-US", null, null, emptyList(),
    )

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
    fun shufflePicksOnlyUnfinishedAndIsStableForASeed() {
        val done = setOf("a1-1", "a2-1")
        repeat(20) { seed ->
            val pick = DailyPlanner.pick(lessons, levels, done, today, shuffleSeed = seed.toLong())!!
            assertTrue(pick.id !in done)
            assertEquals(pick, DailyPlanner.pick(lessons, levels, done, today, shuffleSeed = seed.toLong()))
        }
        val picks = (0L until 50L).map { DailyPlanner.pick(lessons, levels, emptySet(), today, shuffleSeed = it)!!.id }.toSet()
        assertTrue("shuffle should vary the topic", picks.size > 1)
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

    @Test
    fun speechMatchKeepsNonLatinScripts() {
        val korean = SpeechMatch.compare("안녕하세요, 저는 야시입니다.", "안녕하세요 저는 야시입니다")
        assertEquals(100, korean.score)
        val wrongKorean = SpeechMatch.compare("감사합니다", "안녕하세요")
        assertEquals(0, wrongKorean.score)
        assertEquals(listOf("감사합니다"), wrongKorean.missed)

        val german = SpeechMatch.compare("Schöne Grüße aus München!", "schöne grüße aus münchen")
        assertEquals(100, german.score)
        assertEquals(listOf("straße"), SpeechMatch.compare("Die Straße ist lang.", "die strasse ist lang").missed)
    }
}

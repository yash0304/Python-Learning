package com.yash.dailyedge

import com.yash.dailyedge.data.ContentParser
import com.yash.dailyedge.data.Track
import com.yash.dailyedge.domain.Calculators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Validates the bundled lesson packs so broken content never ships. */
class ContentTest {

    private val packs = Track.entries.map { track ->
        ContentParser.parse(File("src/main/assets/content/${track.key}.json").readText())
    }

    @Test
    fun everyTrackHasEnoughLessons() {
        packs.forEach { assertTrue("${it.track} has only ${it.lessons.size} lessons", it.lessons.size >= 30) }
    }

    @Test
    fun idsAreUnique() {
        val ids = packs.flatMap { p -> p.lessons.map { it.id } }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun lessonsAreWellFormed() {
        packs.forEach { pack ->
            pack.lessons.forEach { l ->
                assertTrue("${l.id}: unknown level ${l.level}", l.level in pack.levels)
                assertTrue("${l.id}: no sections", l.sections.isNotEmpty())
                assertTrue("${l.id}: no quiz", l.quiz.isNotEmpty())
                l.quiz.forEach { q ->
                    assertEquals("${l.id}: quiz needs 4 options", 4, q.options.size)
                    assertTrue("${l.id}: bad answer index", q.answerIndex in q.options.indices)
                }
                l.calculatorId?.let { assertTrue("${l.id}: unknown calculator $it", Calculators.byId(it) != null) }
            }
        }
    }

    @Test
    fun trackSpecificBlocksArePresent() {
        packs.first { it.track == Track.ENGLISH }.lessons.forEach { assertTrue("${it.id}: no speaking drill", it.speak.isNotEmpty()) }
        packs.first { it.track == Track.AI_EDGE }.lessons.forEach { assertTrue("${it.id}: no action", it.action != null) }
        packs.first { it.track == Track.FINANCE }.lessons.forEach {
            assertTrue("${it.id}: no key term", it.keyTerm != null)
            assertTrue("${it.id}: no calculation", it.calculation != null)
        }
    }
}

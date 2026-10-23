package com.yash.gyaanly

import com.yash.gyaanly.data.ContentParser
import com.yash.gyaanly.domain.Calculators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Validates every bundled lesson pack (including ones added later) so broken content never ships. */
class ContentTest {

    private val packs = File("src/main/assets/content").listFiles { f -> f.extension == "json" }!!
        .sortedBy { it.name }
        .map { file -> file.name to ContentParser.parse(file.readText()) }

    private fun pack(key: String) = packs.map { it.second }.firstOrNull { it.track.key == key }

    @Test
    fun trackKeysAndLessonIdsAreUnique() {
        val keys = packs.map { it.second.track.key }
        assertEquals("duplicate track keys", keys.size, keys.toSet().size)
        val ids = packs.flatMap { p -> p.second.lessons.map { it.id } }
        val dupes = ids.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("duplicate lesson ids: $dupes", dupes.isEmpty())
    }

    @Test
    fun everyPackIsWellFormed() {
        packs.forEach { (file, pack) ->
            assertTrue("$file: no levels", pack.track.levels.isNotEmpty())
            assertTrue("$file: no lessons", pack.lessons.isNotEmpty())
            pack.lessons.forEach { l ->
                assertTrue("${l.id}: unknown level ${l.level}", l.level in pack.track.levels)
                assertTrue("${l.id}: no sections", l.sections.isNotEmpty())
                assertTrue("${l.id}: no quiz", l.quiz.isNotEmpty())
                l.quiz.forEach { q ->
                    assertEquals("${l.id}: '${q.question}' needs 4 options", 4, q.options.size)
                    assertEquals("${l.id}: '${q.question}' has duplicate options", 4, q.options.toSet().size)
                    assertTrue("${l.id}: bad answer index", q.answerIndex in q.options.indices)
                }
                l.link?.let { assertTrue("${l.id}: link must be https", it.url.startsWith("https://")) }
                l.calculatorId?.let { assertTrue("${l.id}: unknown calculator $it", Calculators.byId(it) != null) }
            }
        }
    }

    @Test
    fun builtInPathsAreExtensive() {
        listOf("ai", "people", "english", "finance", "india", "spanish", "mind", "fss").forEach { key ->
            val p = pack(key) ?: error("missing built-in pack $key")
            assertTrue("$key has only ${p.lessons.size} lessons", p.lessons.size >= 100)
            p.lessons.forEach { assertTrue("${it.id}: needs 3 quiz questions", it.quiz.size >= 3) }
        }
    }

    @Test
    fun trackSpecificBlocksArePresent() {
        listOf("english", "people").forEach { key ->
            pack(key)!!.lessons.forEach { assertTrue("${it.id}: no speaking drill", it.speak.isNotEmpty()) }
        }
        pack("people")!!.lessons.forEach { assertEquals("${it.id}: needs 4 sections", 4, it.sections.size) }
        listOf("ai", "people", "india").forEach { key ->
            pack(key)!!.lessons.forEach { assertTrue("${it.id}: no action", it.action != null) }
        }
        val india = pack("india")!!
        assertEquals("200 everyday India problems", 200, india.lessons.size)
        india.lessons.forEach { assertEquals("${it.id}: needs 4 sections", 4, it.sections.size) }
        mapOf("spanish" to "es-ES").forEach { (key, lang) ->
            pack(key)?.lessons?.forEach {
                assertTrue("${it.id}: no speaking drill", it.speak.isNotEmpty())
                assertEquals("${it.id}: wrong speech language", lang, it.speakLang)
            }
        }
        val spanish = pack("spanish")!!
        assertEquals("Spanish is 12 units", 12, spanish.track.levels.size)
        spanish.track.levels.forEach { u -> assertEquals("$u needs 10 lessons", 10, spanish.lessons.count { it.level == u }) }
        val mind = pack("mind")!!
        assertTrue("Mind & Words shows a random topic", mind.track.shuffle)
        assertEquals("100 mind topics", 100, mind.lessons.size)
        mind.lessons.forEach {
            assertEquals("${it.id}: needs 4 sections", 4, it.sections.size)
            assertTrue("${it.id}: no key term", it.keyTerm != null)
            assertTrue("${it.id}: no action", it.action != null)
        }
        val fss = pack("fss")!!
        assertEquals("11 shared-services teams", 11, fss.track.levels.size)
        fss.track.levels.forEach { t -> assertEquals("$t needs 10 lessons", 10, fss.lessons.count { it.level == t }) }
        assertEquals("2026-12-31", fss.track.deadline)
        fss.lessons.forEach {
            assertTrue("${it.id}: no key term", it.keyTerm != null)
            assertTrue("${it.id}: no action", it.action != null)
        }
        val interview = pack("english")!!.lessons.filter { it.level == "Interview Prep" }
        assertEquals("15-day interview course", 15, interview.size)
        pack("finance")!!.lessons.forEach {
            assertTrue("${it.id}: no key term", it.keyTerm != null)
            assertTrue("${it.id}: no calculation", it.calculation != null)
        }
    }
}

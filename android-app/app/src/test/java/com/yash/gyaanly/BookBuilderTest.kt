package com.yash.gyaanly

import com.yash.gyaanly.data.ContentParser
import com.yash.gyaanly.data.EpubWriter
import com.yash.gyaanly.domain.BookBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/** Every course can become an e-book, a shareable text and a read-aloud script. */
class BookBuilderTest {

    private val packs = File("src/main/assets/content").listFiles { f -> f.extension == "json" }!!
        .map { ContentParser.parse(it.readText()) }

    @Test
    fun everyCourseBuildsACompleteBook() {
        packs.forEach { pack ->
            val book = BookBuilder.fromTrack(pack)
            assertEquals(pack.track.levels.count { l -> pack.lessons.any { it.level == l } }, book.chapters.size)
            val headings = book.chapters.flatMap { c -> c.blocks.filter { it.type == "h" }.map { it.text } }
            pack.lessons.forEach { l -> assertTrue("${l.id} missing", headings.any { it.endsWith(l.title) }) }
        }
    }

    @Test
    fun singleTopicBookHasOneChapter() {
        val pack = packs.first { it.track.key == "fss" }
        val book = BookBuilder.fromTrack(pack, listOf("AP"))
        assertEquals(1, book.chapters.size)
        assertEquals("fss-ap", book.key)
    }

    @Test
    fun epubHasStoredMimetypeFirstAndEveryChapter() {
        packs.forEach { pack ->
            val book = BookBuilder.fromTrack(pack)
            val bytes = ByteArrayOutputStream().also { EpubWriter.write(book, it) }.toByteArray()
            System.getenv("EPUB_OUT")?.let { dir -> File(dir, "${book.key}.epub").writeBytes(bytes) }
            val names = mutableListOf<String>()
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                val first = zip.nextEntry!!
                assertEquals("mimetype", first.name)
                assertEquals(ZipEntry.STORED, first.method)
                assertEquals("application/epub+zip", zip.readBytes().decodeToString())
                generateSequence { zip.nextEntry }.forEach { names += it.name }
            }
            assertTrue("OEBPS/content.opf" in names)
            assertEquals(book.chapters.size, names.count { it.matches(Regex("OEBPS/c\\d+\\.xhtml")) })
        }
    }

    @Test
    fun spanishIsReadInASpanishVoiceAndExplanationsInEnglish() {
        val lesson = packs.first { it.track.key == "spanish" }.lessons.first { it.example != null }
        val speech = BookBuilder.speech(lesson, "en-IN")
        assertTrue(speech.any { it.lang == "es-ES" })
        assertTrue(speech.any { it.lang == "en-IN" })
        assertTrue(speech.none { it.text.startsWith("•") || it.text.isBlank() })
    }

    @Test
    fun sharedTextContainsTheWholeLesson() {
        val lesson = packs.first { it.track.key == "people" }.lessons.first()
        val text = BookBuilder.plainText(lesson)
        assertTrue(text.contains(lesson.title))
        lesson.sections.forEach { assertTrue(text.contains(it.heading.uppercase())) }
        assertTrue(text.contains(lesson.action!!))
    }
}

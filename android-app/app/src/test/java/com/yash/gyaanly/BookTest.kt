package com.yash.gyaanly

import com.yash.gyaanly.data.ContentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

/** Every track that offers a "book" tool must ship a readable book and its EPUB. */
class BookTest {

    private val knownBlocks = setOf("h", "h3", "p", "em", "q", "li")

    private val bookTracks = File("src/main/assets/content").listFiles { f -> f.extension == "json" }!!
        .map { ContentParser.parse(it.readText()) }
        .filter { "book" in it.track.tools }

    @Test
    fun peoplePathHasABook() {
        assertTrue(bookTracks.any { it.track.key == "people" })
    }

    @Test
    fun booksAreWellFormedAndCoverEveryLevel() {
        bookTracks.forEach { pack ->
            val key = pack.track.key
            val book = ContentParser.parseBook(File("src/main/assets/books/$key.json").readText())
            assertEquals(key, book.key)
            assertTrue("$key: no author", book.author.isNotBlank())
            book.chapters.forEach { c ->
                assertTrue("${c.title}: empty chapter", c.blocks.isNotEmpty())
                c.blocks.forEach { assertTrue("${c.title}: unknown block ${it.type}", it.type in knownBlocks) }
            }
            pack.track.levels.forEach { level ->
                assertTrue("$key: no chapter for $level", book.chapters.any { it.title.endsWith(level) })
            }
            val headings = book.chapters.flatMap { c -> c.blocks.filter { it.type == "h" }.map { it.text } }
            pack.lessons.forEach { l -> assertTrue("$key: ${l.id} missing from the book", headings.any { it.endsWith(l.title) }) }
        }
    }

    @Test
    fun epubIsBundledWithMimetypeFirst() {
        bookTracks.forEach { pack ->
            ZipFile(File("src/main/assets/books/${pack.track.key}.epub")).use { zip ->
                val first = zip.entries().nextElement()
                assertEquals("mimetype", first.name)
                assertEquals("application/epub+zip", zip.getInputStream(first).bufferedReader().readText())
                assertTrue(zip.getEntry("OEBPS/content.opf") != null)
            }
        }
    }
}

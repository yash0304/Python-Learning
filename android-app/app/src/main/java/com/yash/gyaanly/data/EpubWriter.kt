package com.yash.gyaanly.data

import java.io.OutputStream
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes a [Book] as a minimal, valid EPUB 3 file (opens in Google Play Books, Kindle, Apple Books…). */
object EpubWriter {

    fun write(book: Book, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            // The spec requires "mimetype" first and uncompressed.
            val mime = "application/epub+zip".toByteArray()
            zip.putNextEntry(ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mime.size.toLong()
                compressedSize = mime.size.toLong()
                crc = CRC32().apply { update(mime) }.value
            })
            zip.write(mime)
            zip.closeEntry()

            val files = linkedMapOf<String, String>()
            files["title.xhtml"] = page(book.title, """<section class="title" epub:type="titlepage">
<h1>${esc(book.title)}</h1>
<p>${esc(book.subtitle)}</p>
<p><strong>${esc(book.author)}</strong></p>
<p class="small">${esc(book.notice)}</p>
</section>""")
            val nav = StringBuilder()
            book.chapters.forEachIndexed { i, c ->
                val name = "c%02d.xhtml".format(i + 1)
                files[name] = page(c.title, "<section epub:type=\"chapter\">\n<h1>${esc(c.title)}</h1>\n${render(c.blocks)}\n</section>")
                nav.append("<li><a href=\"$name\">${esc(c.title)}</a></li>\n")
            }
            files["nav.xhtml"] = page("Contents", "<nav epub:type=\"toc\" id=\"toc\"><h1>Contents</h1><ol>\n<li><a href=\"title.xhtml\">Title page</a></li>\n$nav</ol></nav>")

            put(zip, "META-INF/container.xml", """<?xml version="1.0" encoding="utf-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
<rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
</container>""")
            put(zip, "OEBPS/content.opf", opf(book, files.keys))
            put(zip, "OEBPS/style.css", CSS)
            files.forEach { (name, body) -> put(zip, "OEBPS/$name", body) }
        }
    }

    private fun opf(book: Book, names: Collection<String>): String {
        val id = UUID.nameUUIDFromBytes("gyaanly/${book.key}".toByteArray())
        val items = names.joinToString("\n") { n ->
            val props = if (n == "nav.xhtml") " properties=\"nav\"" else ""
            "<item id=\"${n.removeSuffix(".xhtml")}\" href=\"$n\" media-type=\"application/xhtml+xml\"$props/>"
        }
        val spine = (listOf("title", "nav") + names.filter { it.startsWith("c") }.map { it.removeSuffix(".xhtml") })
            .joinToString("\n") { "<itemref idref=\"$it\"/>" }
        return """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="bookid" xml:lang="en">
<metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
<dc:identifier id="bookid">urn:uuid:$id</dc:identifier>
<dc:title>${esc(book.title)}</dc:title>
<dc:creator>${esc(book.author)}</dc:creator>
<dc:language>en</dc:language>
<dc:description>${esc(book.subtitle)}</dc:description>
<meta property="dcterms:modified">2026-01-01T00:00:00Z</meta>
</metadata>
<manifest>
<item id="css" href="style.css" media-type="text/css"/>
$items
</manifest>
<spine>
$spine
</spine>
</package>
"""
    }

    private fun render(blocks: List<BookBlock>): String = buildString {
        var inList = false
        blocks.forEach { b ->
            if (b.type == "li" && !inList) { append("<ul>\n"); inList = true }
            if (b.type != "li" && inList) { append("</ul>\n"); inList = false }
            val x = esc(b.text)
            append(
                when (b.type) {
                    "h" -> "<h2>$x</h2>"
                    "h3" -> "<h3>$x</h3>"
                    "em" -> "<p class=\"lead\"><em>$x</em></p>"
                    "q" -> "<blockquote><p>$x</p></blockquote>"
                    "li" -> "<li>$x</li>"
                    else -> "<p>$x</p>"
                },
            )
            append('\n')
        }
        if (inList) append("</ul>\n")
    }

    private fun page(title: String, body: String) = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" lang="en" xml:lang="en">
<head><meta charset="utf-8"/><title>${esc(title)}</title><link rel="stylesheet" type="text/css" href="style.css"/></head>
<body>
$body
</body>
</html>
"""

    private fun put(zip: ZipOutputStream, name: String, text: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(text.toByteArray())
        zip.closeEntry()
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private const val CSS = """body { font-family: serif; line-height: 1.5; }
h1 { text-align: center; margin-top: 2em; }
h3 { margin-bottom: 0.2em; color: #A61E4D; }
p.lead { color: #555; }
blockquote { margin: 0.4em 1em; padding-left: 0.8em; border-left: 3px solid #C2255C; }
.title { text-align: center; }
.small { font-size: 0.85em; color: #555; }
"""
}

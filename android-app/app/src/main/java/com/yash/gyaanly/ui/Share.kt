package com.yash.gyaanly.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.yash.gyaanly.data.Book
import com.yash.gyaanly.data.EpubWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Android share sheet helpers: plain text for a lesson, an EPUB file for a book. */
object Share {

    fun text(context: Context, subject: String, text: String) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, "Share lesson"))
    }

    /** Writes the book to cache and opens the share sheet (WhatsApp, Drive, Gmail, Play Books…). */
    suspend fun epub(context: Context, book: Book) {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            File(dir, fileName(book)).also { f -> f.outputStream().use { EpubWriter.write(book, it) } }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/epub+zip")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, book.title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Share e-book"))
    }

    fun fileName(book: Book) = book.key.lowercase().replace(Regex("[^a-z0-9-]+"), "-") + ".epub"
}

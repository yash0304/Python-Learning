package com.yash.gyaanly.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yash.gyaanly.data.BookBlock
import com.yash.gyaanly.data.EpubWriter
import com.yash.gyaanly.domain.BookBuilder
import com.yash.gyaanly.speech.EXPLAIN_LANG
import com.yash.gyaanly.speech.Speaker
import com.yash.gyaanly.ui.AppViewModel
import com.yash.gyaanly.ui.Share
import com.yash.gyaanly.ui.theme.accent
import com.yash.gyaanly.ui.theme.gradient
import com.yash.gyaanly.ui.theme.soft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Reads a course (or one topic) as an e-book: listen chapter by chapter, share or save it as EPUB. */
@Composable
fun BookScreen(vm: AppViewModel, trackKey: String, level: String?, onBack: () -> Unit) {
    val pack = vm.pack(trackKey)
    val book = remember(trackKey, level) { vm.book(trackKey, level) }
    if (pack == null || book == null) { BackBar("Book not found", onBack); return }
    val color = pack.track.accent
    val context = LocalContext.current
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val speaker = remember { Speaker(context) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    var reading by remember { mutableStateOf(-1) }

    val saveEpub = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/epub+zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)!!.use { EpubWriter.write(book, it) } }.isSuccess
            }
            Toast.makeText(context, if (ok) "EPUB saved ✅" else "Couldn't save the EPUB", Toast.LENGTH_SHORT).show()
        }
    }

    // Lesson-based speech keeps Spanish lines in a Spanish voice; hand-written chapters are read as they are.
    fun chapterSpeech(index: Int): List<BookBuilder.Speech> {
        val chapter = book.chapters[index]
        val lessons = pack.lessons.filter { it.level == chapter.title.substringAfter(": ") }
        return if (lessons.isEmpty()) BookBuilder.speech(chapter, EXPLAIN_LANG)
        else listOf(BookBuilder.Speech(chapter.title, EXPLAIN_LANG)) + lessons.flatMap { BookBuilder.speech(it, EXPLAIN_LANG) }
    }

    // Flatten into list rows so long chapters scroll lazily; remember where each chapter starts.
    val rows = remember(book) {
        buildList {
            book.chapters.forEach { c ->
                add(BookBlock("chapter", c.title))
                addAll(c.blocks)
            }
        }
    }
    val chapterRows = remember(rows) { rows.withIndex().filter { it.value.type == "chapter" }.map { it.index } }
    val header = 2 // cover card + chapter chips

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { BackBar("📖 ${book.title}", onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            state = list,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                GradientCard(pack.track.gradient) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(book.title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text(book.subtitle, color = Color.White.copy(alpha = 0.92f))
                        Text(book.author, color = Color.White, style = MaterialTheme.typography.labelLarge)
                        Text(book.notice, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        PopButton("📤  Share e-book", Color.White, onClick = { scope.launch { Share.epub(context, book) } }, contentColor = color)
                        PopButton("⬇️  Save EPUB to phone", Color.White.copy(alpha = 0.85f), onClick = { saveEpub.launch(Share.fileName(book)) }, contentColor = color)
                    }
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    book.chapters.forEachIndexed { i, c ->
                        AssistChip(
                            onClick = { scope.launch { list.animateScrollToItem(header + chapterRows[i]) } },
                            label = { Text(c.title.substringBefore(":"), fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }
            items(rows.size) { i ->
                val chapter = chapterRows.indexOf(i)
                if (chapter >= 0) {
                    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(rows[i].text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
                        TextButton(onClick = {
                            if (reading == chapter) {
                                speaker.stop()
                            } else if (speaker.read(chapterSpeech(chapter)) { reading = -1 }) {
                                reading = chapter
                            } else {
                                Toast.makeText(context, "Text-to-speech isn't ready yet", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text(if (reading == chapter) "⏹  Stop listening" else "🔊  Listen to this chapter", color = color, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    BookRow(rows[i], color)
                }
            }
        }
    }
}

@Composable
private fun BookRow(block: BookBlock, color: Color) {
    val type = MaterialTheme.typography
    when (block.type) {
        "chapter" -> Text(block.text, style = type.headlineSmall, fontWeight = FontWeight.Black, color = color, modifier = Modifier.padding(top = 24.dp))
        "h" -> Text(block.text, style = type.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp))
        "h3" -> Text(block.text, style = type.titleSmall, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(top = 6.dp))
        "em" -> Text(block.text, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
        "q" -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(color.soft(0.08f), MaterialTheme.shapes.small)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(color))
            Text(block.text, modifier = Modifier.padding(10.dp), fontStyle = FontStyle.Italic)
        }
        "li" -> Row(verticalAlignment = Alignment.Top) {
            Text("•  ", color = color, fontWeight = FontWeight.Black)
            Text(block.text)
        }
        else -> Text(block.text, style = type.bodyLarge)
    }
}

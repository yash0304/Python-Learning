package com.yash.gyaanly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.gyaanly.ui.AppViewModel
import com.yash.gyaanly.ui.theme.Pop
import com.yash.gyaanly.ui.theme.accent
import com.yash.gyaanly.ui.theme.gradient
import com.yash.gyaanly.ui.theme.soft

@Composable
fun TrackScreen(
    vm: AppViewModel,
    trackKey: String,
    onBack: () -> Unit,
    openLesson: (String) -> Unit,
    openTool: (String) -> Unit,
) {
    val pack = vm.pack(trackKey) ?: run { BackBar("Not found", onBack); return }
    val track = pack.track
    val progress by vm.progress.collectAsStateWithLifecycle()
    val done = pack.lessons.count { it.id in progress.completed }
    val color = track.accent

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { BackBar(track.title, onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GradientCard(track.gradient) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(track.emoji, style = MaterialTheme.typography.displaySmall)
                        Text(track.title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text(track.tagline, color = Color.White.copy(alpha = 0.92f))
                        Text("$done / ${pack.lessons.size} lessons completed", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        FatProgress(
                            if (pack.lessons.isEmpty()) 0f else done / pack.lessons.size.toFloat(),
                            color = Color.White,
                            track = Color.White.copy(alpha = 0.3f),
                        )
                    }
                }
            }
            if (track.levelPicker) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🎚️  My level: today's lesson starts here", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val chosen = progress.levels[track.key] ?: track.levels.firstOrNull()
                            track.levels.forEach { level ->
                                FilterChip(
                                    selected = chosen == level,
                                    onClick = { vm.setLevel(track.key, level) },
                                    label = { Text(level, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = color,
                                        selectedLabelColor = Color.White,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            if ("book" in track.tools) {
                item { PopButton("📖  Read the e-book", color, onClick = { openTool("book") }) }
            }
            if ("calculators" in track.tools) {
                item { PopButton("🧮  Open finance calculators", Pop.Sun, onClick = { openTool("calculators") }, contentColor = Color(0xFF3B2600)) }
            }
            track.levels.forEach { level ->
                val lessons = pack.lessons.filter { it.level == level }
                if (lessons.isEmpty()) return@forEach
                val levelDone = lessons.count { it.id in progress.completed }
                item(key = "h-$level") {
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Chip(level.uppercase(), color, filled = true)
                        Spacer(Modifier.width(10.dp))
                        Text("$levelDone / ${lessons.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                itemsIndexed(lessons, key = { _, l -> l.id }) { index, lesson ->
                    val isDone = lesson.id in progress.completed
                    // Zig-zag the nodes a little to feel like a path.
                    val indent = listOf(0, 24, 48, 24)[index % 4].dp
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = indent)
                            .clickable { openLesson(lesson.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .background(if (isDone) color else MaterialTheme.colorScheme.surface, CircleShape)
                                .border(3.dp, color, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (isDone) "★" else "${index + 1}",
                                color = if (isDone) Color.White else color,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isDone) color.soft(0.10f) else MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            shadowElevation = 1.dp,
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(lesson.title, fontWeight = FontWeight.Bold)
                                    Text(lesson.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                progress.quizScores[lesson.id]?.let { Chip(it, color) }
                            }
                        }
                    }
                }
            }
        }
    }
}

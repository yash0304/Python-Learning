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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.gyaanly.domain.DailyPlanner
import com.yash.gyaanly.ui.AppViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.yash.gyaanly.ui.theme.Pop
import com.yash.gyaanly.ui.theme.accent
import com.yash.gyaanly.ui.theme.gradient
import com.yash.gyaanly.ui.theme.soft
import kotlinx.coroutines.launch

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
    val levels = vm.visibleLevels(track, progress)
    val deadline = track.deadline?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Row index of each level's header, so tapping a level chip can jump straight to its lessons.
    val headerIndex = buildMap {
        var index = 1 + (if (track.levelPicker) 1 else 0) + (if (deadline != null) 1 else 0) +
            1 + (if ("calculators" in track.tools) 1 else 0)
        levels.forEach { level ->
            val count = pack.lessons.count { it.level == level }
            if (count > 0) { put(level, index); index += 1 + count }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { BackBar(track.title, onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            state = list,
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
                        Text("🎚️  Tap a level to jump to it. Today's lesson starts there too.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val chosen = progress.levels[track.key] ?: track.levels.firstOrNull()
                            levels.forEach { level ->
                                FilterChip(
                                    selected = chosen == level,
                                    onClick = {
                                        vm.setLevel(track.key, level)
                                        headerIndex[level]?.let { scope.launch { list.animateScrollToItem(it) } }
                                    },
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
            if (deadline != null) {
                item {
                    val visible = pack.lessons.filter { progress.showsLevel(track.key, it.level) }
                    val left = visible.count { it.id !in progress.completed }
                    val today = LocalDate.now()
                    val pace = DailyPlanner.pacePerDay(left, today, deadline)
                    val date = deadline.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
                    InfoCard("Finish by $date", "🎯", color) {
                        Text(
                            when {
                                left == 0 -> "All done. Brilliant work! 🎉"
                                pace == null -> "The target date has passed; $left lessons left. Keep going!"
                                else -> "$left lessons left, ${deadline.toEpochDay() - today.toEpochDay() + 1} days to go: do $pace a day to finish on time."
                            },
                        )
                    }
                }
            }
            item { PopButton("📖  Read, listen or share as e-book", color, onClick = { openTool("book") }) }
            if ("calculators" in track.tools) {
                item { PopButton("🧮  Open finance calculators", Pop.Sun, onClick = { openTool("calculators") }, contentColor = Color(0xFF3B2600)) }
            }
            levels.forEach { level ->
                val lessons = pack.lessons.filter { it.level == level }
                if (lessons.isEmpty()) return@forEach
                val levelDone = lessons.count { it.id in progress.completed }
                item(key = "h-$level") {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f, fill = false)) { Chip(level.uppercase(), color, filled = true) }
                        Spacer(Modifier.width(10.dp))
                        Text("$levelDone / ${lessons.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { openTool("book:$level") }) { Text("📖 Book", color = color, fontWeight = FontWeight.Bold) }
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

package com.yash.dailyedge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.dailyedge.data.Track
import com.yash.dailyedge.ui.AppViewModel
import com.yash.dailyedge.ui.theme.color

@Composable
fun TrackScreen(vm: AppViewModel, track: Track, openLesson: (String) -> Unit, openCalculators: () -> Unit) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val pack = vm.pack(track)
    val done = pack.lessons.count { it.id in progress.completed }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(track.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = track.color)
                Text(track.tagline, style = MaterialTheme.typography.bodyMedium)
                Text("$done / ${pack.lessons.size} lessons completed", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (track == Track.ENGLISH) {
            item {
                Column {
                    Text("My level (today's lesson starts here)", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pack.levels.forEach { level ->
                            FilterChip(
                                selected = progress.englishLevel == level,
                                onClick = { vm.setEnglishLevel(level) },
                                label = { Text(level) },
                            )
                        }
                    }
                }
            }
        }
        if (track == Track.FINANCE) {
            item {
                OutlinedButton(onClick = openCalculators, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Calculate, contentDescription = null)
                    Text("  Finance calculators")
                }
            }
        }
        pack.levels.forEach { level ->
            val lessons = pack.lessons.filter { it.level == level }
            if (lessons.isEmpty()) return@forEach
            item(key = "h-$level") {
                Text(level, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            items(lessons, key = { it.id }) { lesson ->
                val isDone = lesson.id in progress.completed
                Card(Modifier.fillMaxWidth().clickable { openLesson(lesson.id) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = if (isDone) "Done" else "Not done",
                            tint = if (isDone) Color(0xFF16A34A) else MaterialTheme.colorScheme.outline,
                        )
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(lesson.title, fontWeight = FontWeight.SemiBold)
                            Text(lesson.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                        progress.quizScores[lesson.id]?.let { Chip(it, track.color) }
                    }
                }
            }
        }
    }
}

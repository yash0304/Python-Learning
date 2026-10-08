package com.yash.dailyedge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(vm: AppViewModel, openLesson: (String) -> Unit, openTrack: (Track) -> Unit) {
    val state by vm.today.collectAsStateWithLifecycle()
    val doneToday = state.plan.values.count { it.id in state.progress.completed }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Your Daily Edge", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF97316))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${state.streak}-day streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$doneToday of ${state.plan.size} done today", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { if (state.plan.isEmpty()) 0f else doneToday / state.plan.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }

        Track.entries.forEach { track ->
            val lesson = state.plan[track] ?: return@forEach
            val done = lesson.id in state.progress.completed
            Card(
                modifier = Modifier.fillMaxWidth().clickable { openLesson(lesson.id) },
                colors = CardDefaults.cardColors(containerColor = track.color.copy(alpha = 0.08f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(track.title, color = track.color, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Chip(lesson.level, track.color)
                    }
                    Text(lesson.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(lesson.summary, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (done) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                            Text(" Done", color = Color(0xFF16A34A), modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.nextLesson(track) }) { Text("One more") }
                        } else {
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { openTrack(track) }) { Text("All lessons") }
                        }
                    }
                }
            }
        }
    }
}

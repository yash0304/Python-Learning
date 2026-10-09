package com.yash.gyaanly.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.gyaanly.ui.AppViewModel
import com.yash.gyaanly.ui.theme.HeroGradient
import com.yash.gyaanly.ui.theme.gradient
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(vm: AppViewModel, openLesson: (String) -> Unit, openTrack: (String) -> Unit) {
    val state by vm.today.collectAsStateWithLifecycle()
    val p = state.progress
    val doneToday = state.plan.count { (_, lesson) -> lesson.id in p.completed }
    val total = state.plan.size
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning ☀️"
        in 12..16 -> "Good afternoon 🌤️"
        else -> "Good evening 🌙"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GradientCard(HeroGradient) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Gyaanly ✨  ·  " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    StatPill("🔥", "${state.streak}", Color.White)
                    Spacer(Modifier.width(8.dp))
                    StatPill("⚡", "${p.xp} XP", Color.White)
                }
                Text(greeting, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text(
                    when {
                        total == 0 -> "No lessons yet."
                        doneToday == total -> "All done today! You're on fire 🎉"
                        doneToday == 0 -> "$total quick wins are waiting for you."
                        else -> "$doneToday of $total done, keep going! 💪"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
                FatProgress(
                    if (total == 0) 0f else doneToday / total.toFloat(),
                    color = Color.White,
                    track = Color.White.copy(alpha = 0.3f),
                )
            }
        }

        SectionTitle("Today's quests", "🎯")

        state.plan.forEach { (track, lesson) ->
            val done = lesson.id in p.completed
            GradientCard(track.gradient, onClick = { openLesson(lesson.id) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(track.emoji, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text(lesson.level, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                        }
                        if (done) Chip("✓ DONE", Color.White)
                    }
                    Text(lesson.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(lesson.summary, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (done) "Nice! Want another?" else "Tap to start ▶",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (done) {
                            TextButton(onClick = { vm.nextLesson(track.key) }) { Text("ONE MORE ➜", color = Color.White) }
                        } else {
                            TextButton(onClick = { openTrack(track.key) }) { Text("ALL LESSONS", color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

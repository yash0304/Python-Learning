package com.yash.dailyedge.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.dailyedge.ui.AppViewModel
import com.yash.dailyedge.ui.theme.HeroGradient
import com.yash.dailyedge.ui.theme.Pop
import com.yash.dailyedge.ui.theme.color

/** Learner levels: every 250 XP unlocks the next title. */
private val ranks = listOf("Curious Starter", "Steady Learner", "Skill Builder", "Rising Pro", "Sharp Thinker", "Edge Master", "Legend")
private const val XP_PER_RANK = 250

@Composable
fun ProfileScreen(vm: AppViewModel) {
    val today by vm.today.collectAsStateWithLifecycle()
    val p = today.progress
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    val rankIndex = (p.xp / XP_PER_RANK).coerceAtMost(ranks.lastIndex)
    val intoRank = p.xp - rankIndex * XP_PER_RANK

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setReminder(true, p.reminderHour, p.reminderMinute)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GradientCard(HeroGradient) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("LEVEL ${rankIndex + 1}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                Text(ranks[rankIndex], color = Color.White, style = MaterialTheme.typography.headlineMedium)
                if (rankIndex < ranks.lastIndex) {
                    FatProgress(intoRank / XP_PER_RANK.toFloat(), Color.White, track = Color.White.copy(alpha = 0.3f))
                    Text("${XP_PER_RANK - intoRank} XP to ${ranks[rankIndex + 1]}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("⚡", "${p.xp} XP", Color.White)
                    StatPill("🔥", "${today.streak} days", Color.White)
                    StatPill("📚", "${p.completed.size}", Color.White)
                }
            }
        }

        SectionTitle("Paths", "🗺️")
        vm.tracks.forEach { track ->
            val lessons = vm.pack(track.key)?.lessons.orEmpty()
            val done = lessons.count { it.id in p.completed }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${track.emoji}  ${track.title}", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text("$done / ${lessons.size}", fontWeight = FontWeight.Bold, color = track.color)
                }
                FatProgress(if (lessons.isEmpty()) 0f else done / lessons.size.toFloat(), track.color)
            }
        }

        SectionTitle("Daily reminder", "⏰")
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Nudge me every day", fontWeight = FontWeight.Bold)
                        Text("at %02d:%02d".format(p.reminderHour, p.reminderMinute), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = p.reminderEnabled,
                        colors = SwitchDefaults.colors(checkedTrackColor = Pop.Grape),
                        onCheckedChange = { on ->
                            if (on && Build.VERSION.SDK_INT >= 33) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                vm.setReminder(on, p.reminderHour, p.reminderMinute)
                            }
                        },
                    )
                }
                PopButton("Change time", Pop.Sky, onClick = {
                    TimePickerDialog(context, { _, h, m -> vm.setReminder(p.reminderEnabled, h, m) }, p.reminderHour, p.reminderMinute, true).show()
                })
            }
        }

        TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Reset progress", color = Pop.Bad)
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset progress? 😱") },
            text = { Text("This clears completed lessons, quiz scores, XP and your streak.") },
            confirmButton = { TextButton(onClick = { vm.resetProgress(); confirmReset = false }) { Text("Reset", color = Pop.Bad) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

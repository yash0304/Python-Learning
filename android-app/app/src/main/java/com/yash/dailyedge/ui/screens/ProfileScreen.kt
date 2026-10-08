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
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.dailyedge.data.Track
import com.yash.dailyedge.ui.AppViewModel
import com.yash.dailyedge.ui.theme.color

@Composable
fun ProfileScreen(vm: AppViewModel, openCalculators: () -> Unit) {
    val today by vm.today.collectAsStateWithLifecycle()
    val p = today.progress
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setReminder(true, p.reminderHour, p.reminderMinute)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Your progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp)) {
                Stat("${today.streak}", "day streak", Modifier.weight(1f))
                Stat("${p.activeDays.size}", "active days", Modifier.weight(1f))
                Stat("${p.completed.size}", "lessons done", Modifier.weight(1f))
            }
        }

        Track.entries.forEach { track ->
            val lessons = vm.pack(track).lessons
            val done = lessons.count { it.id in p.completed }
            Column {
                Row {
                    Text(track.title, color = track.color, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("$done / ${lessons.size}")
                }
                LinearProgressIndicator(
                    progress = { if (lessons.isEmpty()) 0f else done / lessons.size.toFloat() },
                    color = track.color,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }

        Text("Daily reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Remind me every day")
                Text("at %02d:%02d".format(p.reminderHour, p.reminderMinute), style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = p.reminderEnabled,
                onCheckedChange = { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.setReminder(on, p.reminderHour, p.reminderMinute)
                    }
                },
            )
        }
        OutlinedButton(onClick = {
            TimePickerDialog(context, { _, h, m -> vm.setReminder(p.reminderEnabled, h, m) }, p.reminderHour, p.reminderMinute, true).show()
        }) { Text("Change reminder time") }

        Text("Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = openCalculators, modifier = Modifier.fillMaxWidth()) { Text("Finance calculators") }
        TextButton(onClick = { confirmReset = true }) { Text("Reset progress") }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset progress?") },
            text = { Text("This clears completed lessons, quiz scores and your streak.") },
            confirmButton = { TextButton(onClick = { vm.resetProgress(); confirmReset = false }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

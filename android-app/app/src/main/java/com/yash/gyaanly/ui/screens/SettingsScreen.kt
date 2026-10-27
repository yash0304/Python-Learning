package com.yash.gyaanly.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.gyaanly.ui.AppViewModel
import com.yash.gyaanly.ui.practice.AiSettingsCard
import com.yash.gyaanly.ui.practice.PracticeViewModel
import com.yash.gyaanly.ui.theme.accent

/** Turn whole courses, or single topics inside a course, on and off. Progress is always kept. */
@Composable
fun SettingsScreen(vm: AppViewModel, practice: PracticeViewModel, onBack: () -> Unit) {
    val p by vm.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var expanded by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { BackBar("⚙️ Settings", onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Switch off what you don't want right now. Hidden courses and topics disappear from Today and Learn, " +
                        "and your progress in them is kept. Want to focus on one course? Keep only that one on.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { AiSettingsCard(practice) }
            items(vm.tracks, key = { it.key }) { track ->
                val on = p.showsTrack(track.key)
                val color = track.accent
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${track.emoji}  ${track.title}", fontWeight = FontWeight.ExtraBold)
                                val shown = vm.visibleLevels(track, p).size
                                Text(
                                    if (on) "$shown of ${track.levels.size} topics on" else "Hidden",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = on,
                                colors = SwitchDefaults.colors(checkedTrackColor = color),
                                onCheckedChange = { enable ->
                                    if (!enable && vm.visibleTracks(p).size == 1) {
                                        Toast.makeText(context, "Keep at least one course on", Toast.LENGTH_SHORT).show()
                                    } else {
                                        vm.setTrackEnabled(track.key, enable)
                                    }
                                },
                            )
                        }
                        if (on) {
                            TextButton(onClick = { expanded = if (expanded == track.key) null else track.key }) {
                                Text(if (expanded == track.key) "Hide topics ▲" else "Choose topics ▼", color = color, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (on && expanded == track.key) {
                            HorizontalDivider()
                            track.levels.forEach { level ->
                                val levelOn = p.showsLevel(track.key, level)
                                fun toggle() {
                                    if (levelOn && vm.visibleLevels(track, p).size == 1) {
                                        Toast.makeText(context, "Keep at least one topic, or switch the course off", Toast.LENGTH_SHORT).show()
                                    } else {
                                        vm.setLevelEnabled(track.key, level, !levelOn)
                                    }
                                }
                                Row(
                                    Modifier.fillMaxWidth().clickable { toggle() },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = levelOn,
                                        onCheckedChange = { toggle() },
                                        colors = CheckboxDefaults.colors(checkedColor = color),
                                    )
                                    Text(level, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

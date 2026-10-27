package com.yash.gyaanly.ui.practice

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.yash.gyaanly.domain.practice.Scenario
import com.yash.gyaanly.ui.screens.GradientCard
import com.yash.gyaanly.ui.screens.InfoCard
import com.yash.gyaanly.ui.screens.PopButton
import com.yash.gyaanly.ui.screens.SectionTitle
import com.yash.gyaanly.ui.theme.HeroGradient
import com.yash.gyaanly.ui.theme.Pop

val TALK_CATEGORIES = listOf("Daily life", "Office", "Interview", "Sales calls")

fun categoryColor(category: String): Color = when (category) {
    "Daily life" -> Pop.Mint
    "Office" -> Pop.Sky
    "Interview" -> Pop.Grape
    "Sales calls" -> Pop.Coral
    else -> Pop.Pink
}

/** The Talk tab: pick a role-play, paste your own script, or have the AI write one. */
@Composable
fun TalkScreen(vm: PracticeViewModel, open: (String) -> Unit, openSettings: () -> Unit) {
    val ai by vm.ai.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val scripts by vm.scripts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var category by remember { mutableStateOf(TALK_CATEGORIES.first()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GradientCard(HeroGradient) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Talk practice 🎙️", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "A real spoken conversation: the app talks, you answer out loud, hands-free. Afterwards you get a report on what to improve.",
                    color = Color.White.copy(alpha = 0.92f),
                )
                if (history.isNotEmpty()) {
                    val recent = history.take(5).map { it.score }
                    Text("Last ${recent.size} scores: ${recent.joinToString(" · ")}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // AI: write any scenario
        var describe by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        InfoCard(if (ai.ready) "Create any scenario with ${ai.provider.label}" else "Smart AI partner", "🤖", Pop.Grape) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ai.ready) {
                    Text("Describe the conversation and the AI writes it, then plays a partner who reacts to what you actually say.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        describe, { describe = it },
                        label = { Text("e.g. Cold call to sell payroll software to an HR head") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CategoryChips(category) { category = it }
                    PopButton(if (busy) "Writing the scenario…" else "✨  Create & start", Pop.Grape, enabled = !busy && describe.isNotBlank(), onClick = {
                        busy = true
                        vm.generate(describe.trim(), category) { result ->
                            busy = false
                            result.onSuccess(open).onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                        }
                    })
                } else {
                    Text(
                        "Works without AI too. Add your Sarvam (or Claude) API key to get a partner that improvises, scenarios written for you, and detailed coaching.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = openSettings) { Text("⚙️ Add an AI key", fontWeight = FontWeight.Bold, color = Pop.Grape) }
                }
            }
        }

        // Your own scripts
        var title by remember { mutableStateOf("") }
        var script by remember { mutableStateOf("") }
        var showPaste by remember { mutableStateOf(false) }
        InfoCard("Your scripts", "📝", Pop.Pink) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                scripts.forEach { (t, _) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("▶  $t", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clickable {
                            vm.myScripts().firstOrNull { it.title == t }?.let { open(it.id) }
                        })
                        TextButton(onClick = { vm.deleteScript(t) }) { Text("Delete", color = Pop.Bad) }
                    }
                }
                if (showPaste) {
                    Text("Write one line per speaker. The app plays every speaker except \"Me:\". Your lines are hidden while you talk: say them in your own words.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        script, { script = it },
                        label = { Text("Them: Hi, thanks for calling. How can I help?\nMe: Hi, I'm calling about…") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    )
                    PopButton("Save & start", Pop.Pink, enabled = script.isNotBlank(), onClick = {
                        val id = vm.addScript(title.trim(), script)
                        if (id == null) {
                            Toast.makeText(context, "Add at least one line for the other person, e.g. \"Them: …\"", Toast.LENGTH_LONG).show()
                        } else {
                            showPaste = false; title = ""; script = ""
                            open(id)
                        }
                    })
                } else {
                    TextButton(onClick = { showPaste = true }) { Text("➕ Paste a script", fontWeight = FontWeight.Bold, color = Pop.Pink) }
                }
            }
        }

        SectionTitle("Role-plays", "🎭")
        CategoryChips(category) { category = it }
        vm.builtIn.filter { it.category == category }.forEach { ScenarioCard(it) { open(it.id) } }
    }
}

@Composable
private fun CategoryChips(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TALK_CATEGORIES.forEach { c ->
            FilterChip(selected = c == selected, onClick = { onSelect(c) }, label = { Text(c, fontWeight = FontWeight.Bold) })
        }
    }
}

@Composable
private fun ScenarioCard(s: Scenario, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(s.emoji, style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f)) {
                Text(s.title, fontWeight = FontWeight.Bold)
                Text(s.goal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Text("${s.turns.size} turns", style = MaterialTheme.typography.labelSmall, color = categoryColor(s.category))
        }
    }
}

package com.yash.gyaanly.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.gyaanly.data.AiProvider

/** Settings → AI for Talk practice: provider, API keys (kept only on this phone) and the Sarvam model. */
@Composable
fun AiSettingsCard(vm: PracticeViewModel) {
    val ai by vm.ai.collectAsStateWithLifecycle()
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🤖  AI for Talk practice", fontWeight = FontWeight.ExtraBold)
            Text(
                "Optional. With a key, the conversation partner improvises, writes scenarios for you and gives detailed coaching. " +
                    "Each session uses a little of your API credit. Keys stay on this phone and are not included in backups.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AiProvider.entries.forEach { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = ai.provider == p, onClick = { vm.updateAi { it.copy(provider = p) } })
                    Text(p.label)
                }
            }
            when (ai.provider) {
                AiProvider.SARVAM -> {
                    OutlinedTextField(
                        ai.sarvamKey, { key -> vm.updateAi { it.copy(sarvamKey = key) } },
                        label = { Text("Sarvam API subscription key") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        ai.sarvamModel, { m -> vm.updateAi { it.copy(sarvamModel = m) } },
                        label = { Text("Sarvam chat model") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Get the key at dashboard.sarvam.ai. Sarvam renames models now and then; if you get a model error, copy the current chat model name from their docs.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                AiProvider.CLAUDE -> OutlinedTextField(
                    ai.claudeKey, { key -> vm.updateAi { it.copy(claudeKey = key) } },
                    label = { Text("Claude API key (console.anthropic.com)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AiProvider.OFF -> Text("Talk practice works fully offline with the built-in role-plays and your own scripts.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

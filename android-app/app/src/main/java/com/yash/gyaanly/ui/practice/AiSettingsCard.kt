package com.yash.gyaanly.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import kotlin.math.roundToInt

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

            HorizontalDivider()
            Text("🎧  Speech recognition", fontWeight = FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !ai.sarvamSpeech, onClick = { vm.updateAi { it.copy(sarvamSpeech = false) } })
                Column {
                    Text("Phone (free, works offline)")
                    Text("Shows your words live while you speak.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = ai.sarvamSpeech, onClick = { vm.updateAi { it.copy(sarvamSpeech = true) } })
                Column {
                    Text("Sarvam speech-to-text (better with Indian accents)")
                    Text("Needs internet and uses Sarvam credits. Your words appear after each answer.", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (ai.sarvamSpeech) OutlinedTextField(
                ai.sttModel, { m -> vm.updateAi { it.copy(sttModel = m) } },
                label = { Text("Sarvam speech model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            Text("🗣️  Partner's voice", fontWeight = FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !ai.bulbul, onClick = { vm.updateAi { it.copy(bulbul = false) } })
                Text("Phone voice (free, works offline)")
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = ai.bulbul, onClick = { vm.updateAi { it.copy(bulbul = true) } })
                Column {
                    Text("Sarvam Bulbul (natural, human-sounding Indian voice)")
                    Text("Needs internet and uses Sarvam credits; lines are prepared while you answer.", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (ai.bulbul) {
                OutlinedTextField(
                    ai.bulbulSpeaker, { v -> vm.updateAi { it.copy(bulbulSpeaker = v) } },
                    label = { Text("Bulbul voice name (e.g. shubh)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Speed: %.1f×".format(ai.bulbulPace), style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = ai.bulbulPace.toFloat(),
                    onValueChange = { v -> vm.updateAi { it.copy(bulbulPace = (v * 10).roundToInt() / 10.0) } },
                    valueRange = 0.7f..1.5f,
                )
                Text("Voice names are listed in Sarvam's Bulbul docs; type one to change the partner's voice.", style = MaterialTheme.typography.bodySmall)
            }

            if (ai.provider == AiProvider.SARVAM || ai.sarvamSpeech || ai.bulbul) {
                OutlinedTextField(
                    ai.sarvamKey, { key -> vm.updateAi { it.copy(sarvamKey = key) } },
                    label = { Text("Sarvam API subscription key") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if ((ai.sarvamSpeech || ai.bulbul) && ai.sarvamKey.isBlank()) Text("Add your Sarvam key to use Sarvam speech and voices.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

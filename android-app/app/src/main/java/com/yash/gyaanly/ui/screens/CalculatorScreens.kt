package com.yash.gyaanly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yash.gyaanly.domain.Calculators
import com.yash.gyaanly.ui.theme.Pop
import com.yash.gyaanly.ui.theme.soft

private val toolColors = listOf(Pop.Grape, Pop.Pink, Pop.Sky, Pop.Mint, Pop.Coral, Pop.Sun)
private val toolEmoji = mapOf(
    "npv" to "💰", "irr" to "📈", "wacc" to "⚖️", "capm" to "📊", "cagr" to "🚀",
    "emi" to "🏠", "breakeven" to "🎯", "payback" to "⏳", "ratios" to "🩺",
)

/** Tools tab: every calculator. Also reachable from finance lessons (with a back button). */
@Composable
fun CalculatorListScreen(onBack: (() -> Unit)?, open: (String) -> Unit) {
    if (onBack == null) {
        CalculatorList(Modifier, open)
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = { BackBar("Calculators", onBack) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding -> CalculatorList(Modifier.padding(padding), open) }
    }
}

@Composable
private fun CalculatorList(modifier: Modifier, open: (String) -> Unit) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Finance toolbox 🧰", style = MaterialTheme.typography.headlineMedium)
                Text("Plug in numbers and see every step.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        itemsIndexed(Calculators.all, key = { _, c -> c.id }) { i, calc ->
            val color = toolColors[i % toolColors.size]
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().clickable { open(calc.id) },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).background(color.soft(0.18f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(toolEmoji[calc.id] ?: "🧮", style = MaterialTheme.typography.headlineSmall) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(calc.name, fontWeight = FontWeight.ExtraBold)
                        Text(calc.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("›", style = MaterialTheme.typography.headlineMedium, color = color)
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen(id: String, onBack: () -> Unit) {
    val calc = Calculators.byId(id) ?: run { BackBar("Not found", onBack); return }
    val values = remember(id) { mutableStateMapOf<String, String>().apply { calc.inputs.forEach { put(it.key, it.default) } } }
    var output by remember(id) { mutableStateOf<Result<List<String>>?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { BackBar(calc.name, onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("${toolEmoji[calc.id] ?: "🧮"}  ${calc.description}", style = MaterialTheme.typography.bodyLarge)
            InfoCard("Formula", "📐", Pop.Grape) { Text(calc.formula, fontWeight = FontWeight.SemiBold) }
            calc.inputs.forEach { input ->
                OutlinedTextField(
                    value = values[input.key].orEmpty(),
                    onValueChange = { values[input.key] = it },
                    label = { Text(input.label) },
                    supportingText = { if (input.hint.isNotEmpty()) Text(input.hint) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (input.key == "flows") KeyboardType.Text else KeyboardType.Decimal,
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PopButton("Calculate", Pop.Grape, onClick = { output = runCatching { calc.compute(values.toMap()) } })
            output?.let { result ->
                result.fold(
                    onSuccess = { lines ->
                        InfoCard("Result", "✨", Pop.Good) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                lines.forEachIndexed { i, line ->
                                    Text(line, fontWeight = if (i == lines.lastIndex) FontWeight.Black else FontWeight.Normal)
                                }
                            }
                        }
                    },
                    onFailure = {
                        InfoCard("Check your input", "⚠️", Pop.Bad) { Text(it.message ?: "Invalid input", color = Color.Unspecified) }
                    },
                )
            }
        }
    }
}

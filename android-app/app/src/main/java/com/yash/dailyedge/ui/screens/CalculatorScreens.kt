package com.yash.dailyedge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yash.dailyedge.data.Track
import com.yash.dailyedge.domain.Calculators
import com.yash.dailyedge.ui.theme.color

@Composable
fun CalculatorListScreen(onBack: () -> Unit, open: (String) -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = { BackBar("Finance calculators", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Calculators.all, key = { it.id }) { calc ->
                Card(Modifier.fillMaxWidth().clickable { open(calc.id) }) {
                    Column(Modifier.padding(14.dp)) {
                        Text(calc.name, fontWeight = FontWeight.SemiBold)
                        Text(calc.description, style = MaterialTheme.typography.bodySmall)
                    }
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
    val color = Track.FINANCE.color

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = { BackBar(calc.name, onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(calc.description, style = MaterialTheme.typography.bodyMedium)
            InfoCard("Formula", color) { Text(calc.formula, fontWeight = FontWeight.Medium) }
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(onClick = { output = runCatching { calc.compute(values.toMap()) } }, modifier = Modifier.fillMaxWidth()) {
                Text("Calculate")
            }
            output?.let { result ->
                InfoCard("Result", color) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        result.fold(
                            onSuccess = { lines -> lines.forEach { Text(it) } },
                            onFailure = { Text(it.message ?: "Invalid input", color = Color(0xFFDC2626)) },
                        )
                    }
                }
            }
        }
    }
}

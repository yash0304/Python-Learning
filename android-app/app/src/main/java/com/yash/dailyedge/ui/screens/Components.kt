package com.yash.dailyedge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(title: String, onBack: () -> Unit) {
    // The app-level Scaffold already applies system bar insets.
    TopAppBar(
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = { Text(title, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        },
    )
}

@Composable
fun Chip(text: String, color: Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** A titled card with a tinted background, used for lesson blocks. */
@Composable
fun InfoCard(title: String, color: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = color, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Box(Modifier.padding(top = 8.dp)) { content() }
        }
    }
}

package com.yash.dailyedge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.yash.dailyedge.data.Track

private val Light = lightColorScheme(
    primary = Color(0xFF1E3A8A),
    secondary = Color(0xFFD97706),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF93C5FD),
    secondary = Color(0xFFFBBF24),
)

val Track.color: Color
    get() = when (this) {
        Track.AI_EDGE -> Color(0xFF7C3AED)
        Track.ENGLISH -> Color(0xFF0D9488)
        Track.FINANCE -> Color(0xFF2563EB)
    }

@Composable
fun DailyEdgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

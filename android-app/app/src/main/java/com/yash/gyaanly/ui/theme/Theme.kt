package com.yash.gyaanly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yash.gyaanly.data.Track

/** Vibrant, playful palette. */
object Pop {
    val Grape = Color(0xFF6C3BFF)
    val Pink = Color(0xFFFF4D8D)
    val Sun = Color(0xFFFFB800)
    val Mint = Color(0xFF00C896)
    val Sky = Color(0xFF00A8FF)
    val Coral = Color(0xFFFF6B4A)
    val Good = Color(0xFF22C55E)
    val Bad = Color(0xFFFF4B4B)
    val Flame = Color(0xFFFF7A00)
}

private val Light = lightColorScheme(
    primary = Pop.Grape,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE5FF),
    onPrimaryContainer = Color(0xFF24005F),
    secondary = Pop.Pink,
    onSecondary = Color.White,
    tertiary = Pop.Sun,
    background = Color(0xFFFFF9F2),
    onBackground = Color(0xFF1F1535),
    surface = Color.White,
    onSurface = Color(0xFF1F1535),
    surfaceVariant = Color(0xFFF3EEFF),
    onSurfaceVariant = Color(0xFF5E5675),
    outline = Color(0xFFD9D2EA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB69CFF),
    onPrimary = Color(0xFF24005F),
    primaryContainer = Color(0xFF3B2470),
    onPrimaryContainer = Color(0xFFEDE5FF),
    secondary = Color(0xFFFF7EAA),
    tertiary = Pop.Sun,
    background = Color(0xFF140C26),
    onBackground = Color(0xFFF3EEFF),
    surface = Color(0xFF1F1438),
    onSurface = Color(0xFFF3EEFF),
    surfaceVariant = Color(0xFF2A1C4A),
    onSurfaceVariant = Color(0xFFC9BFE3),
    outline = Color(0xFF4A3B70),
)

private val Rounded = FontFamily.SansSerif

private val PlayfulType = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = Rounded, fontWeight = FontWeight.Black),
        headlineLarge = headlineLarge.copy(fontFamily = Rounded, fontWeight = FontWeight.Black),
        headlineMedium = headlineMedium.copy(fontFamily = Rounded, fontWeight = FontWeight.Black),
        headlineSmall = headlineSmall.copy(fontFamily = Rounded, fontWeight = FontWeight.ExtraBold),
        titleLarge = titleLarge.copy(fontFamily = Rounded, fontWeight = FontWeight.ExtraBold),
        titleMedium = titleMedium.copy(fontFamily = Rounded, fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontFamily = Rounded, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
        bodyLarge = TextStyle(fontFamily = Rounded, fontSize = 17.sp, lineHeight = 26.sp),
    )
}

private val PlayfulShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
)

/** The track's colour as a Compose [Color] (the model stores it as an ARGB Long). */
val Track.accent: Color get() = Color(color)

/** Diagonal gradient from the track colour to a warmer, lighter tint. */
val Track.gradient: Brush
    get() = Brush.linearGradient(listOf(accent, lerp(accent, Pop.Pink, 0.45f)))

fun Color.soft(alpha: Float = 0.14f) = copy(alpha = alpha)

val HeroGradient = Brush.linearGradient(listOf(Pop.Grape, Pop.Pink, Pop.Sun))

@Composable
fun GyaanlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = PlayfulType,
        shapes = PlayfulShapes,
        content = content,
    )
}

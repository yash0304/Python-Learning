package com.yash.dailyedge.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yash.dailyedge.ui.theme.Pop
import com.yash.dailyedge.ui.theme.soft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(title: String, onBack: () -> Unit, color: Color = MaterialTheme.colorScheme.background) {
    // The app-level Scaffold already applies system bar insets.
    TopAppBar(
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = color),
        title = { Text(title, maxLines = 1, style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        },
    )
}

/** Pill-shaped tag. */
@Composable
fun Chip(text: String, color: Color, filled: Boolean = false) {
    Box(
        Modifier
            .background(if (filled) color else color.soft(), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            color = if (filled) Color.White else color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** A rounded card with a soft tint, a coloured left accent and an emoji title. */
@Composable
fun InfoCard(title: String, emoji: String, color: Color, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = color.soft(0.10f),
        border = BorderStroke(2.dp, color.soft(0.35f)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Text(title, color = color, style = MaterialTheme.typography.titleMedium)
            }
            Box(Modifier.padding(top = 10.dp)) { content() }
        }
    }
}

/** Card filled with a gradient; content is drawn in white. */
@Composable
fun GradientCard(brush: Brush, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(brush)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp),
    ) { content() }
}

/**
 * Chunky "3D" button that sinks when pressed. The darker ledge underneath gives the playful,
 * game-like feel used throughout the app.
 */
@Composable
fun PopButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Color.White,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val drop by animateDpAsState(if (pressed && enabled) LEDGE else 0.dp, label = "press")
    val face = if (enabled) color else MaterialTheme.colorScheme.outline
    val ledge = lerp(face, Color.Black, 0.25f)
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.fillMaxWidth().height(56.dp + LEDGE)) {
        Box(Modifier.fillMaxWidth().height(56.dp).offset(y = LEDGE).background(ledge, shape))
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(y = drop)
                .clip(shape)
                .background(face)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text.uppercase(), color = contentColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        }
    }
}

enum class OptionState { Idle, Correct, Wrong, Dimmed }

/** Quiz answer tile with a bottom ledge, coloured once answered. */
@Composable
fun OptionTile(text: String, state: OptionState, onClick: () -> Unit) {
    val (border, fill, textColor) = when (state) {
        OptionState.Idle -> Triple(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
        OptionState.Correct -> Triple(Pop.Good, Pop.Good.soft(), Pop.Good)
        OptionState.Wrong -> Triple(Pop.Bad, Pop.Bad.soft(), Pop.Bad)
        OptionState.Dimmed -> Triple(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(border)
            .padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .background(fill)
            .clickable(enabled = state == OptionState.Idle, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}

/** Small round stat bubble, e.g. 🔥 7. */
@Composable
fun StatPill(emoji: String, value: String, color: Color) {
    Row(
        Modifier
            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
            .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(emoji)
        Text(value, color = color, fontWeight = FontWeight.Black)
    }
}

/** Thick rounded progress bar. */
@Composable
fun FatProgress(fraction: Float, color: Color, modifier: Modifier = Modifier, track: Color = color.soft(0.2f)) {
    Box(modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50)).background(track)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@Composable
fun SectionTitle(text: String, emoji: String? = null) {
    Text(
        listOfNotNull(emoji, text).joinToString("  "),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 4.dp),
    )
}

private val LEDGE = 5.dp

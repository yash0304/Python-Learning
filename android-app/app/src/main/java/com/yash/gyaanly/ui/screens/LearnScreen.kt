package com.yash.gyaanly.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.gyaanly.ui.AppViewModel
import com.yash.gyaanly.ui.theme.gradient

/** Grid of every track found in assets/content — new packs appear here automatically. */
@Composable
fun LearnScreen(vm: AppViewModel, openTrack: (String) -> Unit) {
    val progress by vm.progress.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("Learning paths 🗺️", style = MaterialTheme.typography.headlineMedium)
                Text("Pick a path to explore every lesson.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(vm.tracks, key = { it.key }) { track ->
            val lessons = vm.pack(track.key)?.lessons.orEmpty()
            val done = lessons.count { it.id in progress.completed }
            GradientCard(track.gradient, onClick = { openTrack(track.key) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(track.emoji, style = MaterialTheme.typography.displaySmall)
                    Text(track.title, color = Color.White, style = MaterialTheme.typography.titleMedium, minLines = 2)
                    Text("$done / ${lessons.size} lessons", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium)
                    FatProgress(
                        if (lessons.isEmpty()) 0f else done / lessons.size.toFloat(),
                        color = Color.White,
                        track = Color.White.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

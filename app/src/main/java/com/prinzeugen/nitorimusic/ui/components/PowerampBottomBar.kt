package com.prinzeugen.nitorimusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prinzeugen.nitorimusic.playback.PlaybackController
import com.prinzeugen.nitorimusic.playback.PlaybackUiState
import kotlinx.coroutines.delay

@Composable
fun PowerampBottomBar(
    state: PlaybackUiState,
    playback: PlaybackController,
    onOpenNowPlaying: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
) {
    if (state.currentMediaId == null) return

    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.currentMediaId, state.isPlaying) {
        while (true) {
            val dur = state.durationMs
            if (dur > 0) {
                progress = (playback.positionMs().toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            } else {
                progress = 0f
            }
            delay(500)
        }
    }

    Surface(
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 4.dp)
        ) {
            // Row 1: Track Info + Play/Pause Button
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenNowPlaying),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtworkImage(
                        trackId = state.currentMediaId.toLongOrNull(),
                        sizePx = 120,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = state.title.ifEmpty { "Belum ada lagu" },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.artist.ifEmpty { "-" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Play / Pause Button Bulat
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = playback::togglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.isPlaying) "⏸" else "▶",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Row 2: Mini Progress Timeline
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .padding(horizontal = 16.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            // Row 3: Action Icons (Library, Equalizer/NowPlaying, Search, Queue)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Library Button
                IconButton(onClick = onLibraryClick) {
                    Text("⊞", style = MaterialTheme.typography.titleLarge)
                }

                // 2. Now Playing / Equalizer Icon
                IconButton(onClick = onOpenNowPlaying) {
                    Text("ılı", style = MaterialTheme.typography.titleMedium)
                }

                // 3. Search Icon
                IconButton(onClick = onOpenSearch) {
                    Text("🔍", style = MaterialTheme.typography.bodyLarge)
                }

                // 4. Current Queue Button (Up Next)
                IconButton(onClick = onOpenQueue) {
                    Text("☰", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

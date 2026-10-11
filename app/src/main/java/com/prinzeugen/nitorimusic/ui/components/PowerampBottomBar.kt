package com.prinzeugen.nitorimusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prinzeugen.nitorimusic.playback.PlaybackController
import com.prinzeugen.nitorimusic.playback.PlaybackUiState
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 3-Row Bottom Bar ala Poweramp.
 * Selalu tampil bahkan saat aplikasi baru pertama kali dibuka (persistent navigation).
 * Row 1 mendukung gesture swipe kiri/kanan untuk Next/Previous lagu.
 */
@Composable
fun PowerampBottomBar(
    state: PlaybackUiState,
    playback: PlaybackController,
    showPlaybackRows: Boolean = true,
    onOpenNowPlaying: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val hasTrack = state.currentMediaId != null
    var progress by remember { mutableFloatStateOf(0f) }
    val dragOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(state.currentMediaId, state.isPlaying) {
        while (true) {
            val dur = state.durationMs
            if (dur > 0 && hasTrack) {
                progress = (playback.positionMs().toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            } else {
                progress = 0f
            }
            delay(500)
        }
    }

    Surface(
        tonalElevation = if (showPlaybackRows) 6.dp else 2.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(top = if (showPlaybackRows) 8.dp else 2.dp, bottom = 4.dp)
        ) {
            if (showPlaybackRows) {
                // Row 1: Track Info + Play/Pause Button (Dukungan Animasi Swipe Geser Kiri/Kanan)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .graphicsLayer {
                            translationX = dragOffset.value
                            alpha = (1f - (abs(dragOffset.value) / 500f)).coerceIn(0.3f, 1f)
                        }
                        .pointerInput(hasTrack) {
                            if (!hasTrack) return@pointerInput
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (dragOffset.value < -60f) {
                                            dragOffset.animateTo(-300f, tween(150))
                                            playback.next()
                                            dragOffset.snapTo(0f)
                                        } else if (dragOffset.value > 60f) {
                                            dragOffset.animateTo(300f, tween(150))
                                            playback.previous(forcePreviousItem = true)
                                            dragOffset.snapTo(0f)
                                        } else {
                                            dragOffset.animateTo(
                                                0f,
                                                spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffset.animateTo(0f)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        dragOffset.snapTo(dragOffset.value + dragAmount)
                                    }
                                }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .clickable(onClick = {
                                if (hasTrack) onOpenNowPlaying() else onLibraryClick()
                            }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtworkImage(
                            trackId = state.currentMediaId?.toLongOrNull(),
                            sizePx = 120,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (hasTrack) state.title else "Nitori Music Player",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (hasTrack) state.artist else "Pilih lagu untuk memutar",
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasTrack) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(enabled = hasTrack, onClick = playback::togglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = if (hasTrack) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
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
            }

            // Row 3: Action Icons (Library, Equalizer/NowPlaying, Search, Queue, Settings)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Library Button
                IconButton(onClick = onLibraryClick) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = "Library",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2. Now Playing Icon
                IconButton(onClick = {
                    if (hasTrack) onOpenNowPlaying() else onLibraryClick()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "Now Playing",
                        tint = if (hasTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // 3. Search Icon
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 4. Current Queue Button (Up Next)
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 5. Settings Button
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Pengaturan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

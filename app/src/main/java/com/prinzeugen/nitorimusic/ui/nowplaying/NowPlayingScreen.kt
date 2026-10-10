package com.prinzeugen.nitorimusic.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.components.ArtworkImage
import com.prinzeugen.nitorimusic.ui.formatTime
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(viewModel: MainViewModel, onBack: () -> Unit, onOpenQueue: () -> Unit) {
    val playback = viewModel.playback
    val state by playback.state.collectAsState()

    var position by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    var showSleepTimer by remember { mutableStateOf(false) }

    if (showSleepTimer) {
        SleepTimerDialog(
            currentMinutesLeft = state.sleepTimerMinutesLeft,
            onSetTimer = playback::setSleepTimer,
            onCancelTimer = playback::cancelSleepTimer,
            onDismiss = { showSleepTimer = false }
        )
    }

    LaunchedEffect(state.currentMediaId, state.isPlaying) {
        while (true) {
            if (!seeking) position = playback.positionMs()
            delay(500)
        }
    }

    val trackId = state.currentMediaId?.toLongOrNull()
    val duration = state.durationMs.coerceAtLeast(1L)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top Bar: Back & Queue info
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
            }
            IconButton(onClick = onOpenQueue) {
                Icon(Icons.Rounded.QueueMusic, contentDescription = "Queue")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Large Album Artwork
        ArtworkImage(
            trackId = trackId,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp)),
        )

        Spacer(Modifier.height(20.dp))

        // Title & Artist
        Text(
            state.title.ifEmpty { "Tidak ada lagu" },
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            state.artist.ifEmpty { "-" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        // Secondary Controls (Repeat, Sleep Timer, Shuffle All)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Repeat button
            IconButton(onClick = playback::toggleRepeatMode) {
                when (state.repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icon(
                        Icons.Rounded.RepeatOne,
                        contentDescription = "Repeat One",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Player.REPEAT_MODE_ALL -> Icon(
                        Icons.Rounded.Repeat,
                        contentDescription = "Repeat All",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    else -> Icon(
                        Icons.Rounded.Repeat,
                        contentDescription = "Repeat Off",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // Sleep Timer
            IconButton(onClick = { showSleepTimer = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = "Sleep Timer",
                        tint = if (state.sleepTimerMinutesLeft > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.sleepTimerMinutesLeft > 0) {
                        Text(
                            "${state.sleepTimerMinutesLeft}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            // Shuffle Button
            IconButton(onClick = viewModel::shuffleAll) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Progress Slider
        Slider(
            value = (if (seeking) seekValue else position.toFloat()).coerceIn(0f, duration.toFloat()),
            onValueChange = {
                seeking = true
                seekValue = it
            },
            onValueChangeFinished = {
                playback.seekTo(seekValue.toLong())
                position = seekValue.toLong()
                seeking = false
            },
            valueRange = 0f..duration.toFloat(),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(if (seeking) seekValue.toLong() else position), style = MaterialTheme.typography.bodySmall)
            Text(formatTime(state.durationMs), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))

        // Playback Buttons: Prev, Play/Pause, Next dengan Icon Vector Elegan
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = playback::previous,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
            }

            Spacer(Modifier.size(24.dp))

            // Main Play/Pause Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = playback::togglePlayPause,
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(Modifier.size(24.dp))

            IconButton(
                onClick = playback::next,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

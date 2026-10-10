package com.prinzeugen.nitorimusic.ui.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.components.ArtworkImage
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(viewModel: MainViewModel, onBack: () -> Unit, onOpenQueue: () -> Unit) {
    val playback = viewModel.playback
    val state by playback.state.collectAsState()

    var position by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }

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
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Kembali") }
            TextButton(onClick = onOpenQueue) { Text("Queue (${state.queue.size})") }
        }
        Spacer(Modifier.height(8.dp))
        ArtworkImage(
            trackId = trackId,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            state.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            state.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
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
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = playback::previous) { Text("Prev") }
            Button(onClick = playback::togglePlayPause) { Text(if (state.isPlaying) "Pause" else "Play") }
            OutlinedButton(onClick = playback::next) { Text("Next") }
        }

        Spacer(Modifier.weight(1f))

        // Bottom Navigation Icons ala Poweramp
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("⊞", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(onClick = {}) {
                Text("ılı", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onBack) {
                Text("🔍", style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(onClick = onOpenQueue) {
                Text("☰", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

internal fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}


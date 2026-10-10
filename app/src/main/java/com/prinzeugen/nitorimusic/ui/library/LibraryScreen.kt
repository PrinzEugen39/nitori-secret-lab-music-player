package com.prinzeugen.nitorimusic.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prinzeugen.nitorimusic.playback.PlaybackUiState
import com.prinzeugen.nitorimusic.ui.MainViewModel

@Composable
fun LibraryScreen(viewModel: MainViewModel, hasPermission: Boolean) {
    val tracks by viewModel.tracks.collectAsState()
    val playback by viewModel.playback.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        if (!hasPermission) {
            Text(
                "Izin akses audio dibutuhkan untuk membaca library.",
                modifier = Modifier.padding(24.dp),
            )
        } else {
            Text(
                "${tracks.size} lagu",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 48.dp, bottom = 8.dp),
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                    val isCurrent = playback.currentMediaId == track.id.toString()
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.play(index) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            track.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "${track.artist} - ${track.album}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
        MiniPlayer(playback, onToggle = viewModel.playback::togglePlayPause)
    }
}

@Composable
private fun MiniPlayer(state: PlaybackUiState, onToggle: () -> Unit) {
    if (state.currentMediaId == null) return
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    state.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onToggle) { Text(if (state.isPlaying) "Pause" else "Play") }
        }
    }
}

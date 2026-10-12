package com.prinzeugen.nitorimusic.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prinzeugen.nitorimusic.R
import com.prinzeugen.nitorimusic.domain.model.Album
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.components.ArtworkImage
import com.prinzeugen.nitorimusic.ui.formatTime

@Composable
fun AlbumDetailScreen(
    album: Album,
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val tracks = viewModel.getTracksForAlbum(album.id)
    val playback by viewModel.playback.state.collectAsState()
    val hasCustomArt = viewModel.hasCustomAlbumArt(album.id)

    var showAlbumDialog by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveCustomAlbumArt(album.id, uri)
        }
    }

    if (showAlbumDialog) {
        AlbumActionDialog(
            album = album,
            tracks = tracks,
            hasCustomArt = hasCustomArt,
            onPlayNext = { viewModel.playNextAlbum(album.id) },
            onAddToQueue = { viewModel.addAlbumToQueue(album.id) },
            onChangeAlbumArt = { pickImageLauncher.launch("image/*") },
            onRemoveCustomArt = { viewModel.removeCustomAlbumArt(album.id) },
            onDismiss = { showAlbumDialog = false }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_to_albums))
            }
            Text(stringResource(R.string.album_detail_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showAlbumDialog = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.album_menu_tooltip))
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtworkImage(
                trackId = album.coverTrackId,
                albumId = album.id,
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showAlbumDialog = true }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    album.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    album.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                val albumTotalSize = com.prinzeugen.nitorimusic.ui.formatBytes(tracks.sumOf { it.sizeBytes })
                val meta = buildString {
                    if (album.year > 0) append("${album.year} • ")
                    append(stringResource(R.string.album_detail_meta, album.trackCount, albumTotalSize))
                }
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.playTracks(tracks, 0) },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.play))
                    }
                    OutlinedButton(
                        onClick = { viewModel.shuffleTracks(tracks) },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.shuffle))
                    }
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                val isCurrent = playback.currentTrackId == track.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.playTracks(tracks, index) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (track.trackNo > 0) "%02d".format(track.trackNo) else "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(32.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            track.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (track.isCue || track.id < 0L) {
                                Spacer(Modifier.width(6.dp))
                                com.prinzeugen.nitorimusic.ui.components.CueBadge()
                            }
                        }
                    }
                    Text(
                        formatTime(track.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    com.prinzeugen.nitorimusic.ui.components.TrackMoreOptionsButton(
                        track = track,
                        onPlayNext = { viewModel.playback.playNext(track) },
                        onAddToQueue = { viewModel.playback.addToQueue(track) },
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

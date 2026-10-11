package com.prinzeugen.nitorimusic.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prinzeugen.nitorimusic.R
import com.prinzeugen.nitorimusic.domain.model.Album
import com.prinzeugen.nitorimusic.domain.model.Track
import com.prinzeugen.nitorimusic.ui.components.ArtworkImage
import com.prinzeugen.nitorimusic.ui.formatTime

/**
 * Album Detail / Context Dialog ala Poweramp:
 * Menampilkan thumbnail album, judul, artis, ♪ track count | total duration | year,
 * serta aksi Play Next, Queue, Rescan, Album Art (Pilih Gambar), dan List Options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumActionDialog(
    album: Album,
    tracks: List<Track>,
    hasCustomArt: Boolean,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onRescan: () -> Unit,
    onChangeAlbumArt: () -> Unit,
    onRemoveCustomArt: () -> Unit,
    onListOptions: () -> Unit,
    onDismiss: () -> Unit,
) {
    val totalDurationMs = tracks.sumOf { it.durationMs }
    val durationText = formatTime(totalDurationMs)

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(vertical = 16.dp)) {
                // Header (Thumbnail, Title, Artist, Metadata)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtworkImage(
                        trackId = album.coverTrackId,
                        albumId = album.id,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            album.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            album.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        val meta = buildString {
                            append("♪ ${album.trackCount} | $durationText")
                            if (album.year > 0) append(" | ${album.year}")
                        }
                        Text(
                            meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(6.dp))

                // Menu Items
                AlbumActionItem(
                    icon = Icons.Rounded.SkipNext,
                    label = stringResource(R.string.action_play_next),
                    onClick = {
                        onPlayNext()
                        onDismiss()
                    }
                )
                AlbumActionItem(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = stringResource(R.string.queue),
                    onClick = {
                        onAddToQueue()
                        onDismiss()
                    }
                )
                AlbumActionItem(
                    icon = Icons.Rounded.Refresh,
                    label = stringResource(R.string.action_rescan),
                    onClick = {
                        onRescan()
                        onDismiss()
                    }
                )
                AlbumActionItem(
                    icon = Icons.Rounded.Image,
                    label = if (hasCustomArt) stringResource(R.string.action_change_album_art) else stringResource(R.string.action_pick_album_art),
                    onClick = {
                        onChangeAlbumArt()
                        onDismiss()
                    }
                )
                if (hasCustomArt) {
                    AlbumActionItem(
                        icon = Icons.Rounded.Delete,
                        label = stringResource(R.string.action_remove_custom_art),
                        onClick = {
                            onRemoveCustomArt()
                            onDismiss()
                        }
                    )
                }
                AlbumActionItem(
                    icon = Icons.Rounded.Tune,
                    label = stringResource(R.string.list_options_tooltip),
                    onClick = {
                        onListOptions()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun AlbumActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


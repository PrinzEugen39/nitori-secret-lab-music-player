package com.prinzeugen.nitorimusic.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prinzeugen.nitorimusic.domain.model.Track
import com.prinzeugen.nitorimusic.playback.AudioSpecs
import com.prinzeugen.nitorimusic.ui.formatTime

/**
 * Konteks Menu Detail Lagu ala Poweramp (Screenshot 4).
 * Muncul saat user menahan (long-press) cover art atau info lagu di Now Playing,
 * item antrean di Queue, maupun dari opsi track di Library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailActionDialog(
    track: Track,
    audioSpecs: AudioSpecs? = null,
    onPlayNext: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onRemoveFromQueue: (() -> Unit)? = null,
    onGoToAlbum: ((Long) -> Unit)? = null,
    onGoToArtist: ((String) -> Unit)? = null,
    onChangeAlbumArt: ((Long, Uri) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var showInfoTagsDialog by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && onChangeAlbumArt != null) {
            onChangeAlbumArt(track.albumId, uri)
            onDismiss()
        }
    }

    if (showInfoTagsDialog) {
        TrackInfoTagsDialog(
            track = track,
            audioSpecs = audioSpecs,
            onDismiss = { showInfoTagsDialog = false }
        )
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: Album Art, Judul, Artis • Album, Format & Durasi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtworkImage(
                        trackId = track.id,
                        albumId = track.albumId,
                        sizePx = 180,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${track.artist} • ${track.album}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        val specsText = buildString {
                            append("♪ ${formatTime(track.durationMs)}")
                            if (audioSpecs != null && audioSpecs.codec.isNotEmpty()) {
                                append(" | ${audioSpecs.codec.lowercase()}")
                            }
                        }
                        Text(
                            text = specsText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))

                // Action Menu Items Grid / List ala Poweramp
                ActionRowItem(
                    icon = Icons.Rounded.Info,
                    label = "Info / Tags",
                    onClick = { showInfoTagsDialog = true }
                )

                if (onGoToAlbum != null && track.albumId > 0) {
                    ActionRowItem(
                        icon = Icons.Rounded.Album,
                        label = "Buka Album: ${track.album}",
                        onClick = {
                            onDismiss()
                            onGoToAlbum(track.albumId)
                        }
                    )
                }

                if (onGoToArtist != null && track.artist.isNotEmpty()) {
                    ActionRowItem(
                        icon = Icons.Rounded.Person,
                        label = "Buka Artis: ${track.artist}",
                        onClick = {
                            onDismiss()
                            onGoToArtist(track.artist)
                        }
                    )
                }

                if (onChangeAlbumArt != null && track.albumId > 0) {
                    ActionRowItem(
                        icon = Icons.Rounded.Image,
                        label = "Ganti Album Art...",
                        onClick = {
                            pickImageLauncher.launch("image/*")
                        }
                    )
                }

                ActionRowItem(
                    icon = Icons.Rounded.SkipNext,
                    label = "Putar Selanjutnya (Play Next)",
                    onClick = {
                        onDismiss()
                        onPlayNext()
                    }
                )

                if (onRemoveFromQueue != null) {
                    ActionRowItem(
                        icon = Icons.Rounded.Delete,
                        label = "Hapus dari Antrean",
                        onClick = {
                            onDismiss()
                            onRemoveFromQueue()
                        }
                    )
                } else {
                    ActionRowItem(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        label = "Tambah ke Antrean",
                        onClick = {
                            onDismiss()
                            onAddToQueue()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRowItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Dialog Info/Tags Lengkap ala Poweramp (Screenshot 5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackInfoTagsDialog(
    track: Track,
    audioSpecs: AudioSpecs? = null,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header dengan Close X Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Info / Tags",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    // Full File Path / Location
                    val path = if (track.folderPath.isNotEmpty()) {
                        "${track.folderPath}/${track.title}"
                    } else {
                        track.uri.toString()
                    }
                    Text(
                        text = path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    // Audio Specs Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            val codecStr = audioSpecs?.codec ?: "AUDIO"
                            val durationSec = track.durationMs / 1000
                            val summary = buildString {
                                append(codecStr)
                                append(", ${durationSec} sec (${formatTime(track.durationMs)})")
                                if (audioSpecs != null) {
                                    if (audioSpecs.sampleRateHz > 0) append(", ${audioSpecs.sampleRateHz / 1000.0} kHz")
                                    if (audioSpecs.bitDepth > 0) append(", ${audioSpecs.bitDepth} bit")
                                    if (audioSpecs.channelCount > 0) append(", ${if (audioSpecs.channelCount == 2) "Stereo" else "${audioSpecs.channelCount} Ch"}")
                                    if (audioSpecs.bitrateKbps > 0) append(", ${audioSpecs.bitrateKbps} kbps")
                                }
                            }
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Field Grid Table
                    DetailInfoRow(label = "Title", value = track.title)
                    DetailInfoRow(label = "Artist", value = track.artist)
                    DetailInfoRow(label = "Album", value = track.album)
                    if (track.trackNo > 0) DetailInfoRow(label = "Track", value = track.trackNo.toString())
                    if (track.year > 0) DetailInfoRow(label = "Year", value = track.year.toString())
                    if (track.folderPath.isNotEmpty()) DetailInfoRow(label = "Folder", value = track.folderPath)
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Tutup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}


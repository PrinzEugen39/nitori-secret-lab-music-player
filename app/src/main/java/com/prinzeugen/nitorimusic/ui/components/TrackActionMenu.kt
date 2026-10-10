package com.prinzeugen.nitorimusic.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prinzeugen.nitorimusic.domain.model.Track
import com.prinzeugen.nitorimusic.ui.formatTime

@Composable
fun TrackMoreOptionsButton(
    track: Track,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        TrackInfoDialog(track = track, onDismiss = { showInfoDialog = false })
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Opsi lagu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Putar Selanjutnya") },
                leadingIcon = {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                onClick = {
                    expanded = false
                    onPlayNext()
                }
            )

            DropdownMenuItem(
                text = { Text("Tambah ke Antrean") },
                leadingIcon = {
                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                onClick = {
                    expanded = false
                    onAddToQueue()
                }
            )

            DropdownMenuItem(
                text = { Text("Detail File") },
                leadingIcon = {
                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                onClick = {
                    expanded = false
                    showInfoDialog = true
                }
            )
        }
    }
}

@Composable
fun TrackInfoDialog(
    track: Track,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Detail Lagu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                InfoItem(label = "Judul", value = track.title)
                InfoItem(label = "Artis", value = track.artist)
                InfoItem(label = "Album", value = track.album)
                InfoItem(label = "Durasi", value = formatTime(track.durationMs))
                if (track.trackNo > 0) InfoItem(label = "Trek No", value = track.trackNo.toString())
                if (track.year > 0) InfoItem(label = "Tahun", value = track.year.toString())
                if (track.folderPath.isNotEmpty()) InfoItem(label = "Folder", value = track.folderPath)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


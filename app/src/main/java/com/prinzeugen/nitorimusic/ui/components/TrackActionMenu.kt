package com.prinzeugen.nitorimusic.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prinzeugen.nitorimusic.domain.model.Track

@Composable
fun TrackMoreOptionsButton(
    track: Track,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onGoToAlbum: ((Long) -> Unit)? = null,
    onGoToArtist: ((String) -> Unit)? = null,
    onChangeAlbumArt: ((Long, Uri) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        TrackDetailActionDialog(
            track = track,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onGoToAlbum = onGoToAlbum,
            onGoToArtist = onGoToArtist,
            onChangeAlbumArt = onChangeAlbumArt,
            onDismiss = { showDialog = false }
        )
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = { showDialog = true },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Opsi lagu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

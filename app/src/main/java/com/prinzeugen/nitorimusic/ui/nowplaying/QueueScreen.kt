package com.prinzeugen.nitorimusic.ui.nowplaying

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prinzeugen.nitorimusic.R
import com.prinzeugen.nitorimusic.domain.model.Track
import com.prinzeugen.nitorimusic.playback.QueueItem
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.components.ArtworkImage
import com.prinzeugen.nitorimusic.ui.components.TrackDetailActionDialog
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun QueueScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val playback = viewModel.playback
    val state by playback.state.collectAsState()
    val allTracks by viewModel.tracks.collectAsState()
    var targetTrackForDetail by remember { mutableStateOf<Track?>(null) }
    var targetQueueItemForDetail by remember { mutableStateOf<QueueItem?>(null) }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        playback.move(from.index, to.index)
    }

    LaunchedEffect(Unit) {
        if (state.currentIndex >= 0) listState.scrollToItem(state.currentIndex)
    }

    if (targetTrackForDetail != null) {
        val track = targetTrackForDetail!!
        TrackDetailActionDialog(
            track = track,
            audioSpecs = if (track.id == state.currentTrackId) state.audioSpecs else null,
            onPlayNext = { playback.playNext(track) },
            onAddToQueue = { playback.addToQueue(track) },
            onRemoveFromQueue = {
                targetQueueItemForDetail?.let { playback.removeById(it.mediaId) }
                    ?: playback.removeByTrackId(track.id)
            },
            onGoToAlbum = { albumId ->
                val album = viewModel.getAlbums().find { it.id == albumId }
                if (album != null) {
                    viewModel.openAlbum(album)
                    onBack()
                }
            },
            onGoToArtist = { artistName ->
                val artist = viewModel.getArtists().find { it.name.equals(artistName, ignoreCase = true) }
                if (artist != null) {
                    viewModel.openArtist(artist)
                    onBack()
                }
            },
            onChangeAlbumArt = { albumId, uri ->
                viewModel.saveCustomAlbumArt(albumId, uri)
            },
            onDismiss = {
                targetTrackForDetail = null
                targetQueueItemForDetail = null
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.up_next, state.queue.size), style = MaterialTheme.typography.titleMedium)
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            itemsIndexed(state.queue, key = { _, item -> item.mediaId }) { index, item ->
                ReorderableItem(reorderState, key = item.mediaId) {
                    QueueRow(
                        item = item,
                        isCurrent = index == state.currentIndex,
                        onClick = { playback.jumpTo(index) },
                        onLongClick = {
                            targetQueueItemForDetail = item
                            targetTrackForDetail = allTracks.find { it.id == item.trackId }
                        },
                        onRemove = { playback.removeById(item.mediaId) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ReorderableCollectionItemScope.QueueRow(
    item: QueueItem,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemove: () -> Unit,
) {
    // State swipe menyimpan lambda dari komposisi pertama; pakai referensi terbaru agar tidak usang.
    val currentOnRemove by rememberUpdatedState(onRemove)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                currentOnRemove()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                )
            }
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkImage(
                    trackId = item.trackId,
                    sizePx = 128,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        item.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if ((item.trackId ?: 0L) < 0L) {
                            Spacer(Modifier.width(6.dp))
                            com.prinzeugen.nitorimusic.ui.components.CueBadge()
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = stringResource(R.string.drag_to_reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .draggableHandle()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .size(24.dp),
                )
            }
        }
    }
}


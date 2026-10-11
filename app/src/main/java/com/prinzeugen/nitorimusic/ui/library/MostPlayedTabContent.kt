package com.prinzeugen.nitorimusic.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prinzeugen.nitorimusic.R
import com.prinzeugen.nitorimusic.domain.model.Track
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.components.ArtworkImage
import com.prinzeugen.nitorimusic.ui.components.FastAlphabetScroller
import com.prinzeugen.nitorimusic.ui.components.TrackDetailActionDialog
import com.prinzeugen.nitorimusic.ui.components.TrackMoreOptionsButton
import com.prinzeugen.nitorimusic.ui.formatTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MostPlayedTabContent(
    tracks: List<Track>,
    currentMediaId: String?,
    viewModel: MainViewModel,
    onTrackClick: (Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
) {
    var targetTrackForDetail by remember { mutableStateOf<Track?>(null) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    if (targetTrackForDetail != null) {
        val track = targetTrackForDetail!!
        TrackDetailActionDialog(
            track = track,
            audioSpecs = null,
            onPlayNext = { onPlayNext(track) },
            onAddToQueue = { onAddToQueue(track) },
            onGoToAlbum = { albumId ->
                val album = viewModel.getAlbums().find { it.id == albumId }
                if (album != null) viewModel.openAlbum(album)
            },
            onGoToArtist = { artistName ->
                val artist = viewModel.getArtists().find { it.name.equals(artistName, ignoreCase = true) }
                if (artist != null) viewModel.openArtist(artist)
            },
            onChangeAlbumArt = { albumId, uri ->
                viewModel.saveCustomAlbumArt(albumId, uri)
            },
            onDismiss = { targetTrackForDetail = null }
        )
    }

    if (tracks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Whatshot,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.most_played_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.most_played_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(end = 22.dp, bottom = 12.dp)
        ) {
            itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                val isCurrent = currentMediaId == track.id.toString()

                Row(
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onTrackClick(index) },
                            onLongClick = { targetTrackForDetail = track }
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(48.dp)) {
                        ArtworkImage(
                            trackId = track.id,
                            albumId = track.albumId,
                            sizePx = 140,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCurrent) {
                                Icon(
                                    Icons.Rounded.Equalizer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                            Text(
                                track.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Play count badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.most_played_times, track.playCount),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }

                            Text(
                                track.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        formatTime(track.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    TrackMoreOptionsButton(
                        track = track,
                        onPlayNext = { onPlayNext(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onGoToAlbum = { albumId ->
                            val album = viewModel.getAlbums().find { it.id == albumId }
                            if (album != null) viewModel.openAlbum(album)
                        },
                        onGoToArtist = { artistName ->
                            val artist = viewModel.getArtists().find { it.name.equals(artistName, ignoreCase = true) }
                            if (artist != null) viewModel.openArtist(artist)
                        },
                        onChangeAlbumArt = { albumId, uri ->
                            viewModel.saveCustomAlbumArt(albumId, uri)
                        }
                    )
                }
                HorizontalDivider()
            }
        }

        FastAlphabetScroller(
            items = tracks,
            itemTitle = { it.title },
            onScrollToIndex = { targetIdx ->
                scope.launch {
                    listState.scrollToItem(targetIdx)
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

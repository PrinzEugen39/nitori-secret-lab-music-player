package com.prinzeugen.nitorimusic.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prinzeugen.nitorimusic.domain.model.Album
import com.prinzeugen.nitorimusic.domain.model.ViewLayout
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.components.ArtworkImage
import com.prinzeugen.nitorimusic.ui.components.FastAlphabetScroller
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumsTabContent(
    albums: List<Album>,
    layout: ViewLayout,
    gridColumns: Int = 2,
    viewModel: MainViewModel,
    onAlbumClick: (Album) -> Unit,
    onOpenListOptions: () -> Unit,
) {
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedAlbumIds by remember { mutableStateOf(emptySet<Long>()) }

    var targetAlbumForAction by remember { mutableStateOf<Album?>(null) }
    var targetAlbumIdForArt by remember { mutableStateOf<Long?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && targetAlbumIdForArt != null) {
            viewModel.saveCustomAlbumArt(targetAlbumIdForArt!!, uri)
            targetAlbumIdForArt = null
            isSelectionMode = false
            selectedAlbumIds = emptySet()
        }
    }

    if (targetAlbumForAction != null) {
        val album = targetAlbumForAction!!
        val albumTracks = viewModel.getTracksForAlbum(album.id)
        val hasCustomArt = viewModel.hasCustomAlbumArt(album.id)

        AlbumActionDialog(
            album = album,
            tracks = albumTracks,
            hasCustomArt = hasCustomArt,
            onPlayNext = { viewModel.playNextAlbum(album.id) },
            onAddToQueue = { viewModel.addAlbumToQueue(album.id) },
            onRescan = { viewModel.rescanLibrary() },
            onChangeAlbumArt = {
                targetAlbumIdForArt = album.id
                pickImageLauncher.launch("image/*")
            },
            onRemoveCustomArt = { viewModel.removeCustomAlbumArt(album.id) },
            onListOptions = onOpenListOptions,
            onDismiss = { targetAlbumForAction = null }
        )
    }

    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        if (layout == ViewLayout.GRID) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(gridColumns.coerceIn(2, 5)),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 24.dp,
                    top = 12.dp,
                    bottom = if (isSelectionMode) 100.dp else 12.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(albums, key = { it.id }) { album ->
                    val isSelected = album.id in selectedAlbumIds
                    AlbumGridItem(
                        album = album,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                selectedAlbumIds = if (isSelected) {
                                    selectedAlbumIds - album.id
                                } else {
                                    selectedAlbumIds + album.id
                                }
                            } else {
                                onAlbumClick(album)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedAlbumIds = setOf(album.id)
                            } else {
                                selectedAlbumIds = if (isSelected) {
                                    selectedAlbumIds - album.id
                                } else {
                                    selectedAlbumIds + album.id
                                }
                            }
                        },
                        onMoreClick = { targetAlbumForAction = album }
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    end = 22.dp,
                    bottom = if (isSelectionMode) 100.dp else 12.dp
                )
            ) {
                items(albums, key = { it.id }) { album ->
                    val isSelected = album.id in selectedAlbumIds
                    AlbumListItem(
                        album = album,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                selectedAlbumIds = if (isSelected) {
                                    selectedAlbumIds - album.id
                                } else {
                                    selectedAlbumIds + album.id
                                }
                            } else {
                                onAlbumClick(album)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedAlbumIds = setOf(album.id)
                            } else {
                                selectedAlbumIds = if (isSelected) {
                                    selectedAlbumIds - album.id
                                } else {
                                    selectedAlbumIds + album.id
                                }
                            }
                        },
                        onMoreClick = { targetAlbumForAction = album }
                    )
                    HorizontalDivider()
                }
            }
        }

        // Floating Bulk Selection Action Bar ala Poweramp (Screenshot 4)
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(vertical = 10.dp, horizontal = 12.dp)) {
                    // Row 1: All Checkbox Toggle, Counter ("2 / 173"), Close X Button
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val allSelected = selectedAlbumIds.size == albums.size && albums.isNotEmpty()
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedAlbumIds = if (allSelected) emptySet() else albums.map { it.id }.toSet()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (allSelected) MaterialTheme.colorScheme.primary else Color(0x99000000))
                                    .border(
                                        1.5.dp,
                                        if (allSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                        RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (allSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "All",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Counter: "2 / 173"
                        Text(
                            text = "${selectedAlbumIds.size} / ${albums.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Close Button (✕)
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedAlbumIds = emptySet()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close Selection")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(6.dp))

                    // Row 2: Queue, Play Next, Image (Disabled if count != 1)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Queue Action
                        SelectionActionButton(
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            label = "Queue",
                            enabled = selectedAlbumIds.isNotEmpty(),
                            onClick = {
                                viewModel.addAlbumsToQueue(selectedAlbumIds)
                                isSelectionMode = false
                                selectedAlbumIds = emptySet()
                            }
                        )

                        // Play Next Action
                        SelectionActionButton(
                            icon = Icons.Rounded.SkipNext,
                            label = "Play Next",
                            enabled = selectedAlbumIds.isNotEmpty(),
                            onClick = {
                                viewModel.playNextAlbums(selectedAlbumIds)
                                isSelectionMode = false
                                selectedAlbumIds = emptySet()
                            }
                        )

                        // Image Action (Hanya aktif jika tepat 1 album dipilih, seperti Poweramp)
                        val canChangeImage = selectedAlbumIds.size == 1
                        SelectionActionButton(
                            icon = Icons.Rounded.Image,
                            label = "Image",
                            enabled = canChangeImage,
                            onClick = {
                                val albumId = selectedAlbumIds.firstOrNull()
                                if (albumId != null) {
                                    targetAlbumIdForArt = albumId
                                    pickImageLauncher.launch("image/*")
                                }
                            }
                        )
                    }
                }
            }

            if (!isSelectionMode) {
                FastAlphabetScroller(
                    items = albums,
                    itemTitle = { it.title },
                    onScrollToIndex = { targetIdx ->
                        scope.launch {
                            if (layout == ViewLayout.GRID) {
                                gridState.scrollToItem(targetIdx)
                            } else {
                                listState.scrollToItem(targetIdx)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGridItem(
    album: Album,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            ArtworkImage(
                trackId = album.coverTrackId,
                albumId = album.id,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )

            // Checkbox overlay di pojok kiri atas (Screenshot 4)
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0x99000000))
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // Tombol konteks menu "⋮" dibuat absolute di pojok kanan atas album (snug container)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x77000000))
                        .clickable(onClick = onMoreClick)
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Album Menu",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Judul & Artis full width tanpa terpotong icon button
        Text(
            album.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            album.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumListItem(
    album: Album,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(52.dp)
        ) {
            ArtworkImage(
                trackId = album.coverTrackId,
                albumId = album.id,
                sizePx = 140,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
            )

            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0x99000000))
                        .border(
                            width = 1.2.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                album.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                album.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val meta = buildString {
            append("♪ ${album.trackCount}")
            if (album.year > 0) append(" • ${album.year}")
        }
        Text(
            meta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!isSelectionMode) {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.padding(start = 4.dp).size(32.dp)
            ) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "Album Menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.38f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
    }
}

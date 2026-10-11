package com.prinzeugen.nitorimusic.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.prinzeugen.nitorimusic.R
import com.prinzeugen.nitorimusic.domain.model.LibraryTab
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.components.PowerampBottomBar
import com.prinzeugen.nitorimusic.ui.formatBytes

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    hasPermission: Boolean,
) {
    val tracks by viewModel.tracks.collectAsState()
    val playback by viewModel.playback.state.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val albumsOptions by viewModel.albumsOptions.collectAsState()
    val tracksOptions by viewModel.tracksOptions.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanStatus by viewModel.scanStatus.collectAsState()
    val selectedMusicFolders by viewModel.selectedMusicFolders.collectAsState()
    val isInitialLoaded by viewModel.isInitialLoaded.collectAsState()

    var showListOptions by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showFolderSelection by remember { mutableStateOf(false) }

    // Onboarding otomatis: Hanya jika DB sudah selesai dicek dan memang benar-benar kosong,
    // tampilkan dialog Folders Selection (dengan saran folder Music)
    LaunchedEffect(isInitialLoaded, tracks.isEmpty(), selectedMusicFolders.isEmpty(), isScanning) {
        if (isInitialLoaded && tracks.isEmpty() && selectedMusicFolders.isEmpty() && !isScanning) {
            showFolderSelection = true
        }
    }

    if (showSearch) {
        SearchDialog(
            allTracks = tracks,
            onTrackSelect = { track ->
                viewModel.playTracks(listOf(track), 0)
            },
            onDismiss = { showSearch = false },
        )
    }

    if (showFolderSelection) {
        FolderSelectionDialog(
            initialFolders = selectedMusicFolders,
            onSaveAndScan = viewModel::saveMusicFolders,
            onDismiss = { showFolderSelection = false },
        )
    }

    if (showListOptions) {
        val albumsTitle = stringResource(R.string.tab_albums)
        val tracksTitle = stringResource(R.string.tab_tracks)
        val (title, currentOptions, onSave) = when (currentTab) {
            LibraryTab.ALBUMS -> Triple(albumsTitle, albumsOptions, viewModel::setAlbumsOptions)
            else -> Triple(tracksTitle, tracksOptions, viewModel::setTracksOptions)
        }
        ListOptionsDialog(
            title = title,
            currentOptions = currentOptions,
            onOptionsChange = onSave,
            onDismiss = { showListOptions = false },
        )
    }

    val detailStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val tabStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()

    val currentDetailKey = when {
        selectedAlbum != null -> "detail_album_${selectedAlbum!!.id}"
        selectedArtist != null -> "detail_artist_${selectedArtist!!.name}"
        selectedFolder != null -> "detail_folder_${selectedFolder!!.path}"
        else -> "library_main"
    }

    detailStateHolder.SaveableStateProvider(currentDetailKey) {
        when {
            selectedAlbum != null -> {
                AlbumDetailScreen(
                    album = selectedAlbum!!,
                    viewModel = viewModel,
                    onBack = viewModel::closeAlbumDetail,
                )
            }
            selectedArtist != null -> {
                ArtistDetailScreen(
                    artist = selectedArtist!!,
                    viewModel = viewModel,
                    onBack = viewModel::closeArtistDetail,
                )
            }
            selectedFolder != null -> {
                FolderDetailScreen(
                    folder = selectedFolder!!,
                    viewModel = viewModel,
                    onBack = viewModel::closeFolderDetail,
                )
            }
            else -> {
                Column(Modifier.fillMaxSize()) {
                    if (!hasPermission) {
                        Text(
                            stringResource(R.string.permission_audio_required),
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(24.dp),
                        )
                        Column(Modifier.weight(1f)) {}
                    } else {
                        // Tab Row
                        ScrollableTabRow(
                            selectedTabIndex = currentTab.ordinal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding(),
                            edgePadding = 16.dp,
                        ) {
                            LibraryTab.values().forEach { tab ->
                                Tab(
                                    selected = currentTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    text = { Text(stringResource(tab.labelRes)) }
                                )
                            }
                        }

                        // Header Toolbar / Action Bar
                        val sortedTracks = viewModel.sortedTracks()
                        val albums = viewModel.getAlbums()
                        val artists = viewModel.getArtists()
                        val folders = viewModel.getFolders()
                        val mostPlayed = viewModel.getMostPlayedTracks()
                        val totalSizeStr = formatBytes(viewModel.getTotalTracksSize())

                        val countLabel = when (currentTab) {
                            LibraryTab.ALBUMS -> stringResource(R.string.header_albums_count, albums.size, totalSizeStr)
                            LibraryTab.TRACKS -> stringResource(R.string.header_tracks_count, sortedTracks.size, totalSizeStr)
                            LibraryTab.ARTISTS -> stringResource(R.string.header_artists_count, artists.size, totalSizeStr)
                            LibraryTab.MOST_PLAYED -> stringResource(R.string.header_most_played_count, mostPlayed.size)
                            LibraryTab.FOLDERS -> stringResource(R.string.header_folders_count, folders.size, totalSizeStr)
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(countLabel, style = MaterialTheme.typography.titleSmall)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentTab == LibraryTab.ALBUMS || currentTab == LibraryTab.TRACKS) {
                                    IconButton(onClick = { showListOptions = true }) {
                                        Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.list_options_tooltip))
                                    }
                                }

                                if (currentTab == LibraryTab.FOLDERS) {
                                    IconButton(onClick = { showFolderSelection = true }) {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = stringResource(R.string.folder_selection_tooltip))
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (currentTab == LibraryTab.MOST_PLAYED && mostPlayed.isNotEmpty()) {
                                            viewModel.shuffleTracks(mostPlayed)
                                        } else {
                                            viewModel.shuffleAll()
                                        }
                                    },
                                    enabled = tracks.isNotEmpty(),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.shuffle_all))
                                }
                            }
                        }

                        // Banner Progress Scanning di Latar Belakang (Poweramp-style)
                        AnimatedVisibility(
                            visible = isScanning,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = scanStatus ?: stringResource(R.string.scan_syncing_library),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        // Content per tab dengan support dynamic gridColumns dan tabStateHolder
                        Column(Modifier.weight(1f)) {
                            if (isInitialLoaded && tracks.isEmpty() && !isScanning) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.FolderOpen,
                                                contentDescription = null,
                                                modifier = Modifier.size(56.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.height(16.dp))
                                            Text(
                                                stringResource(R.string.onboarding_select_folders_title),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                stringResource(R.string.onboarding_select_folders_desc),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(Modifier.height(20.dp))
                                            Button(
                                                onClick = { showFolderSelection = true },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.FolderOpen,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(stringResource(R.string.onboarding_select_folders_btn))
                                            }
                                        }
                                    }
                                }
                            } else {
                                tabStateHolder.SaveableStateProvider(currentTab) {
                                when (currentTab) {
                                    LibraryTab.ALBUMS -> {
                                        AlbumsTabContent(
                                            albums = albums,
                                            layout = albumsOptions.layout,
                                            gridColumns = albumsOptions.gridColumns,
                                            viewModel = viewModel,
                                            onAlbumClick = viewModel::openAlbum,
                                            onOpenListOptions = { showListOptions = true },
                                        )
                                    }
                                    LibraryTab.TRACKS -> {
                                        TracksTabContent(
                                            tracks = sortedTracks,
                                            currentMediaId = playback.currentMediaId,
                                            layout = tracksOptions.layout,
                                            gridColumns = tracksOptions.gridColumns,
                                            onTrackClick = viewModel::play,
                                            onPlayNext = viewModel.playback::playNext,
                                            onAddToQueue = viewModel.playback::addToQueue,
                                            onPlayNextTracks = viewModel.playback::playNext,
                                            onAddTracksToQueue = viewModel.playback::addToQueue,
                                        )
                                    }
                                    LibraryTab.ARTISTS -> {
                                        ArtistsTabContent(
                                            artists = artists,
                                            onArtistClick = viewModel::openArtist,
                                        )
                                    }
                                    LibraryTab.MOST_PLAYED -> {
                                        MostPlayedTabContent(
                                            tracks = mostPlayed,
                                            currentMediaId = playback.currentMediaId,
                                            viewModel = viewModel,
                                            onTrackClick = { idx ->
                                                viewModel.playTracks(mostPlayed, idx)
                                            },
                                            onPlayNext = viewModel.playback::playNext,
                                            onAddToQueue = viewModel.playback::addToQueue,
                                        )
                                    }
                                    LibraryTab.FOLDERS -> {
                                        FoldersTabContent(
                                            folders = folders,
                                            onFolderClick = viewModel::openFolder,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}


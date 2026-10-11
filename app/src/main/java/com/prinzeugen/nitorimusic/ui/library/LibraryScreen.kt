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
import androidx.compose.material.icons.rounded.Refresh
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

    var showListOptions by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showFolderSelection by remember { mutableStateOf(false) }

    // Onboarding otomatis: Jika pustaka kosong dan belum ada folder terpilih,
    // langsung tampilkan dialog Folders Selection (dengan saran folder Music)
    LaunchedEffect(tracks.isEmpty(), selectedMusicFolders.isEmpty(), isScanning) {
        if (tracks.isEmpty() && selectedMusicFolders.isEmpty() && !isScanning) {
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
        val (title, currentOptions, onSave) = when (currentTab) {
            LibraryTab.ALBUMS -> Triple("Albums", albumsOptions, viewModel::setAlbumsOptions)
            else -> Triple("Tracks", tracksOptions, viewModel::setTracksOptions)
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
                            "Izin akses audio dibutuhkan untuk membaca library.",
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
                                    text = { Text(tab.label) }
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
                            LibraryTab.ALBUMS -> "${albums.size} album • $totalSizeStr"
                            LibraryTab.TRACKS -> "${sortedTracks.size} lagu • $totalSizeStr"
                            LibraryTab.ARTISTS -> "${artists.size} artis • $totalSizeStr"
                            LibraryTab.MOST_PLAYED -> "${mostPlayed.size} lagu sering diputar"
                            LibraryTab.FOLDERS -> "${folders.size} folder • $totalSizeStr"
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
                                        Icon(Icons.Rounded.Tune, contentDescription = "List Options")
                                    }
                                }

                                if (currentTab == LibraryTab.FOLDERS) {
                                    IconButton(onClick = { showFolderSelection = true }) {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = "Folders Selection")
                                    }
                                }

                                IconButton(onClick = { viewModel.refreshLibrary(forceSync = true) }) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Pindai Ulang Library")
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
                                    Text("Shuffle all")
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
                                        text = scanStatus ?: "Menyinkronkan pustaka musik...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        // Content per tab dengan support dynamic gridColumns dan tabStateHolder
                        Column(Modifier.weight(1f)) {
                            if (tracks.isEmpty() && !isScanning) {
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
                                                "Pilih Folder Musik",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "Tentukan folder koleksi musik Anda (seperti Music) untuk pemindaian kilat ala Poweramp dan otomatis mengabaikan audio chat/rekaman.",
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
                                                Text("Pilih Folder Musik")
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


package com.prinzeugen.nitorimusic.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prinzeugen.nitorimusic.domain.model.LibraryTab
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.components.PowerampBottomBar

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    hasPermission: Boolean,
    onOpenNowPlaying: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val tracks by viewModel.tracks.collectAsState()
    val playback by viewModel.playback.state.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val spread by viewModel.shuffleSpread.collectAsState()
    val albumsOptions by viewModel.albumsOptions.collectAsState()
    val tracksOptions by viewModel.tracksOptions.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()

    var showShuffleOptions by remember { mutableStateOf(false) }
    var showListOptions by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    if (showShuffleOptions) {
        ShuffleOptionsDialog(
            spread = spread,
            onSpreadChange = viewModel::setShuffleSpread,
            onDismiss = { showShuffleOptions = false },
        )
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

    // Jika sedang melihat detail album, render AlbumDetailScreen
    if (selectedAlbum != null) {
        Column(Modifier.fillMaxSize()) {
            AlbumDetailScreen(
                album = selectedAlbum!!,
                viewModel = viewModel,
                onBack = viewModel::closeAlbumDetail,
            )
            PowerampBottomBar(
                state = playback,
                playback = viewModel.playback,
                onOpenNowPlaying = onOpenNowPlaying,
                onOpenQueue = onOpenQueue,
                onOpenSearch = { showSearch = true },
                onLibraryClick = viewModel::closeAlbumDetail,
            )
        }
        return
    }

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

            val countLabel = when (currentTab) {
                LibraryTab.ALBUMS -> "${albums.size} albums"
                LibraryTab.TRACKS -> "${sortedTracks.size} lagu"
                LibraryTab.ARTISTS -> "${artists.size} artists"
                LibraryTab.FOLDERS -> "${folders.size} folders"
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
                        TextButton(onClick = { showListOptions = true }) {
                            Text("List Options")
                        }
                    }
                    TextButton(onClick = { showShuffleOptions = true }) {
                        Text("Opsi")
                    }
                    Button(
                        onClick = viewModel::shuffleAll,
                        enabled = tracks.isNotEmpty(),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("Shuffle all")
                    }
                }
            }

            // Content per tab dengan support dynamic gridColumns
            Column(Modifier.weight(1f)) {
                when (currentTab) {
                    LibraryTab.ALBUMS -> {
                        AlbumsTabContent(
                            albums = albums,
                            layout = albumsOptions.layout,
                            gridColumns = albumsOptions.gridColumns,
                            onAlbumClick = viewModel::openAlbum,
                        )
                    }
                    LibraryTab.TRACKS -> {
                        TracksTabContent(
                            tracks = sortedTracks,
                            currentMediaId = playback.currentMediaId,
                            layout = tracksOptions.layout,
                            gridColumns = tracksOptions.gridColumns,
                            onTrackClick = viewModel::play,
                        )
                    }
                    LibraryTab.ARTISTS -> {
                        ArtistsTabContent(
                            artists = artists,
                            onArtistClick = { artist ->
                                val artistTracks = tracks.filter { it.artist == artist.name }
                                viewModel.playTracks(artistTracks, 0)
                            }
                        )
                    }
                    LibraryTab.FOLDERS -> {
                        FoldersTabContent(
                            folders = folders,
                            onFolderClick = { folder ->
                                val folderTracks = tracks.filter { it.folderPath == folder.path }
                                viewModel.playTracks(folderTracks, 0)
                            }
                        )
                    }
                }
            }
        }

        // 3-Row Poweramp Bottom Bar
        PowerampBottomBar(
            state = playback,
            playback = viewModel.playback,
            onOpenNowPlaying = onOpenNowPlaying,
            onOpenQueue = onOpenQueue,
            onOpenSearch = { showSearch = true },
            onLibraryClick = { viewModel.closeAlbumDetail() },
        )
    }
}

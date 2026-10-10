package com.prinzeugen.nitorimusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prinzeugen.nitorimusic.ui.components.PowerampBottomBar
import com.prinzeugen.nitorimusic.ui.library.LibraryScreen
import com.prinzeugen.nitorimusic.ui.library.SearchDialog
import com.prinzeugen.nitorimusic.ui.nowplaying.NowPlayingScreen
import com.prinzeugen.nitorimusic.ui.nowplaying.QueueScreen
import com.prinzeugen.nitorimusic.ui.settings.SettingsScreen

enum class Screen { Library, NowPlaying, Queue, Settings }

@Composable
fun AppRoot(viewModel: MainViewModel, hasPermission: Boolean) {
    var screen by rememberSaveable { mutableStateOf(Screen.Library) }
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val playbackState by viewModel.playback.state.collectAsState()
    val tracks by viewModel.tracks.collectAsState()

    var showSearch by remember { mutableStateOf(false) }

    if (showSearch) {
        SearchDialog(
            allTracks = tracks,
            onTrackSelect = { track ->
                viewModel.playTracks(listOf(track), 0)
            },
            onDismiss = { showSearch = false },
        )
    }

    val hasDetailOpen = selectedAlbum != null || selectedArtist != null || selectedFolder != null

    // Tombol Back fisik/gesture: Cepat & Responsif
    BackHandler(enabled = screen != Screen.Library || hasDetailOpen) {
        if (selectedAlbum != null) {
            viewModel.closeAlbumDetail()
        } else if (selectedArtist != null) {
            viewModel.closeArtistDetail()
        } else if (selectedFolder != null) {
            viewModel.closeFolderDetail()
        } else if (screen == Screen.Queue) {
            screen = Screen.NowPlaying
        } else {
            screen = Screen.Library
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Konten Halaman Aktif (Bisa Library, NowPlaying, Queue, atau Settings)
        Box(Modifier.weight(1f)) {
            Crossfade(targetState = screen, label = "screen_transition") { currentScreen ->
                when (currentScreen) {
                    Screen.Library -> LibraryScreen(
                        viewModel = viewModel,
                        hasPermission = hasPermission,
                    )
                    Screen.NowPlaying -> NowPlayingScreen(
                        viewModel = viewModel,
                        onBack = { screen = Screen.Library },
                        onOpenQueue = { screen = Screen.Queue },
                    )
                    Screen.Queue -> QueueScreen(
                        viewModel = viewModel,
                        onBack = {
                            screen = if (selectedAlbum != null) Screen.Library else Screen.NowPlaying
                        },
                    )
                    Screen.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { screen = Screen.Library },
                    )
                }
            }
        }

        // Persistent Bottom Bar yang tampil di SEMUA halaman (Row 1 & 2 disembunyikan saat di Now Playing)
        PowerampBottomBar(
            state = playbackState,
            playback = viewModel.playback,
            showPlaybackRows = screen != Screen.NowPlaying,
            onOpenNowPlaying = { screen = Screen.NowPlaying },
            onOpenQueue = { screen = Screen.Queue },
            onOpenSearch = { showSearch = true },
            onLibraryClick = {
                if (selectedAlbum != null) viewModel.closeAlbumDetail()
                if (selectedArtist != null) viewModel.closeArtistDetail()
                if (selectedFolder != null) viewModel.closeFolderDetail()
                screen = Screen.Library
            },
            onOpenSettings = { screen = Screen.Settings },
        )
    }
}

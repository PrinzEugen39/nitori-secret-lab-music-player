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

enum class Screen { Library, NowPlaying, Queue }

@Composable
fun AppRoot(viewModel: MainViewModel, hasPermission: Boolean) {
    var screen by rememberSaveable { mutableStateOf(Screen.Library) }
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
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

    // Tombol Back fisik/gesture: Cepat & Responsif
    BackHandler(enabled = screen != Screen.Library || selectedAlbum != null) {
        if (selectedAlbum != null) {
            viewModel.closeAlbumDetail()
        } else if (screen == Screen.Queue) {
            screen = Screen.NowPlaying
        } else {
            screen = Screen.Library
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Konten Halaman Aktif (Bisa Library, NowPlaying, atau Queue)
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
                }
            }
        }

        // Persistent 3-Row Bottom Bar yang tampil di SEMUA halaman!
        PowerampBottomBar(
            state = playbackState,
            playback = viewModel.playback,
            onOpenNowPlaying = { screen = Screen.NowPlaying },
            onOpenQueue = { screen = Screen.Queue },
            onOpenSearch = { showSearch = true },
            onLibraryClick = {
                if (selectedAlbum != null) viewModel.closeAlbumDetail()
                screen = Screen.Library
            },
        )
    }
}

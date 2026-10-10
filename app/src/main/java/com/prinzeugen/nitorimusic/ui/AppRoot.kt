package com.prinzeugen.nitorimusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.prinzeugen.nitorimusic.ui.library.LibraryScreen
import com.prinzeugen.nitorimusic.ui.nowplaying.NowPlayingScreen
import com.prinzeugen.nitorimusic.ui.nowplaying.QueueScreen

private enum class Screen { Library, NowPlaying, Queue }

@Composable
fun AppRoot(viewModel: MainViewModel, hasPermission: Boolean) {
    var screen by rememberSaveable { mutableStateOf(Screen.Library) }

    BackHandler(enabled = screen != Screen.Library) {
        screen = if (screen == Screen.Queue) Screen.NowPlaying else Screen.Library
    }

    when (screen) {
        Screen.Library -> LibraryScreen(
            viewModel = viewModel,
            hasPermission = hasPermission,
            onOpenNowPlaying = { screen = Screen.NowPlaying },
        )
        Screen.NowPlaying -> NowPlayingScreen(
            viewModel = viewModel,
            onBack = { screen = Screen.Library },
            onOpenQueue = { screen = Screen.Queue },
        )
        Screen.Queue -> QueueScreen(
            viewModel = viewModel,
            onBack = { screen = Screen.NowPlaying },
        )
    }
}


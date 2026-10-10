package com.prinzeugen.nitorimusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prinzeugen.nitorimusic.data.LibraryRepository
import com.prinzeugen.nitorimusic.domain.model.Track
import com.prinzeugen.nitorimusic.playback.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = LibraryRepository(app)
    val playback = PlaybackController(app)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    init {
        playback.connect()
    }

    fun refreshLibrary() {
        viewModelScope.launch { _tracks.value = repository.loadTracks() }
    }

    fun play(index: Int) = playback.playAll(_tracks.value, index)

    override fun onCleared() {
        playback.release()
    }
}

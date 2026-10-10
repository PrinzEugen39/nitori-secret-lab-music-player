package com.prinzeugen.nitorimusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prinzeugen.nitorimusic.data.LibraryRepository
import com.prinzeugen.nitorimusic.data.SettingsStore
import com.prinzeugen.nitorimusic.domain.model.Track
import com.prinzeugen.nitorimusic.domain.shuffle.SmartShuffle
import com.prinzeugen.nitorimusic.playback.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = LibraryRepository(app)
    private val settings = SettingsStore(app)
    val playback = PlaybackController(app)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    val shuffleSpread: StateFlow<Float> = settings.shuffleSpread
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsStore.DEFAULT_SPREAD)

    init {
        playback.connect()
    }

    fun refreshLibrary() {
        viewModelScope.launch { _tracks.value = repository.loadTracks() }
    }

    fun play(index: Int) = playback.playAll(_tracks.value, index)

    /** Bangun urutan acak sekali, lalu serahkan ke queue di service. */
    fun shuffleAll() {
        val all = _tracks.value
        if (all.isEmpty()) return
        val engine = SmartShuffle(shuffleSpread.value)
        playback.playAll(engine.shuffle(all), 0)
    }

    fun setShuffleSpread(value: Float) {
        viewModelScope.launch { settings.setShuffleSpread(value) }
    }

    override fun onCleared() {
        playback.release()
    }
}

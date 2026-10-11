package com.prinzeugen.nitorimusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prinzeugen.nitorimusic.data.LibraryRepository
import com.prinzeugen.nitorimusic.data.SettingsStore
import com.prinzeugen.nitorimusic.domain.model.Album
import com.prinzeugen.nitorimusic.domain.model.Artist
import com.prinzeugen.nitorimusic.domain.model.Folder
import com.prinzeugen.nitorimusic.domain.model.LibraryTab
import com.prinzeugen.nitorimusic.domain.model.ListOptions
import com.prinzeugen.nitorimusic.domain.model.SortType
import com.prinzeugen.nitorimusic.domain.model.Track
import com.prinzeugen.nitorimusic.domain.model.ViewLayout
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

    private val _currentTab = MutableStateFlow(LibraryTab.ALBUMS)
    val currentTab: StateFlow<LibraryTab> = _currentTab.asStateFlow()

    val albumsOptions: StateFlow<ListOptions> = settings.getAlbumsOptions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ListOptions(layout = ViewLayout.GRID))

    val tracksOptions: StateFlow<ListOptions> = settings.getTracksOptions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ListOptions(layout = ViewLayout.LIST))

    // State Audio Engine
    val audioFocusEnabled: StateFlow<Boolean> = settings.audioFocusEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val pauseOnHeadsetDisconnect: StateFlow<Boolean> = settings.pauseOnHeadsetDisconnect
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val gaplessEnabled: StateFlow<Boolean> = settings.gaplessEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val replayGainEnabled: StateFlow<Boolean> = settings.replayGainEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // State untuk detail view saat klik album/artist/folder (null jika di list utama)
    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()

    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder: StateFlow<Folder?> = _selectedFolder.asStateFlow()

    init {
        playback.connect()
    }

    fun selectTab(tab: LibraryTab) {
        _selectedAlbum.value = null
        _selectedArtist.value = null
        _selectedFolder.value = null
        _currentTab.value = tab
    }

    fun openAlbum(album: Album) {
        _selectedAlbum.value = album
    }

    fun closeAlbumDetail() {
        _selectedAlbum.value = null
    }

    fun openArtist(artist: Artist) {
        _selectedArtist.value = artist
    }

    fun closeArtistDetail() {
        _selectedArtist.value = null
    }

    fun openFolder(folder: Folder) {
        _selectedFolder.value = folder
    }

    fun closeFolderDetail() {
        _selectedFolder.value = null
    }

    /**
     * Memuat library secara offline-first:
     * Muat dari Room DB cache langsung agar UI instan,
     * lalu lakukan sinkronisasi MediaStore di background.
     */
    fun refreshLibrary(forceSync: Boolean = false) {
        viewModelScope.launch {
            if (!forceSync) {
                val cached = repository.getCachedTracks()
                if (cached.isNotEmpty()) {
                    _tracks.value = cached
                }
            }
            _isScanning.value = true
            try {
                val synced = repository.syncFromMediaStore()
                _tracks.value = synced
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun rescanLibrary() {
        refreshLibrary(forceSync = true)
    }

    fun clearCacheAndRescan() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.clearCache()
                val synced = repository.syncFromMediaStore()
                _tracks.value = synced
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun setAudioFocusEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setAudioFocusEnabled(enabled) }
    }

    fun setPauseOnHeadsetDisconnect(enabled: Boolean) {
        viewModelScope.launch { settings.setPauseOnHeadsetDisconnect(enabled) }
    }

    fun setGaplessEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setGaplessEnabled(enabled) }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setReplayGainEnabled(enabled) }
    }

    fun setAlbumsOptions(options: ListOptions) {
        viewModelScope.launch { settings.setAlbumsOptions(options) }
    }

    fun setTracksOptions(options: ListOptions) {
        viewModelScope.launch { settings.setTracksOptions(options) }
    }

    fun play(index: Int) = playback.playAll(sortedTracks(), index)

    fun playTracks(trackList: List<Track>, startIndex: Int = 0) {
        playback.playAll(trackList, startIndex)
    }

    fun shuffleTracks(trackList: List<Track>) {
        if (trackList.isEmpty()) return
        val engine = SmartShuffle(shuffleSpread.value)
        playback.playAll(engine.shuffle(trackList), 0)
    }

    /** Bangun urutan acak sekali dari seluruh library */
    fun shuffleAll() {
        shuffleTracks(_tracks.value)
    }

    fun setShuffleSpread(value: Float) {
        viewModelScope.launch { settings.setShuffleSpread(value) }
    }

    // --- Helpers Derived & Sorted Lists ---

    fun sortedTracks(source: List<Track> = _tracks.value): List<Track> {
        val opt = tracksOptions.value
        val comparator: Comparator<Track> = when (opt.sortType) {
            SortType.BY_NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortType.BY_ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
            SortType.BY_YEAR -> compareBy { it.year }
            SortType.BY_DATE_ADDED -> compareBy { it.dateAddedSec }
            SortType.BY_TRACK_COUNT, SortType.BY_DURATION -> compareBy { it.durationMs }
        }
        val sorted = source.sortedWith(comparator)
        return if (opt.reverse) sorted.reversed() else sorted
    }

    fun getAlbums(): List<Album> {
        val opt = albumsOptions.value
        val grouped = _tracks.value.groupBy { it.albumId }
        val albums = grouped.mapNotNull { (albumId, list) ->
            val first = list.firstOrNull() ?: return@mapNotNull null
            Album(
                id = albumId,
                title = first.album,
                artist = first.artist,
                year = list.maxOfOrNull { it.year } ?: 0,
                trackCount = list.size,
                coverTrackId = first.id,
                dateAddedSec = list.maxOfOrNull { it.dateAddedSec } ?: 0L,
            )
        }

        val comparator: Comparator<Album> = when (opt.sortType) {
            SortType.BY_NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortType.BY_ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
            SortType.BY_YEAR -> compareBy { it.year }
            SortType.BY_DATE_ADDED -> compareBy { it.dateAddedSec }
            SortType.BY_TRACK_COUNT -> compareBy { it.trackCount }
            SortType.BY_DURATION -> compareBy { it.trackCount }
        }
        val sorted = albums.sortedWith(comparator)
        return if (opt.reverse) sorted.reversed() else sorted
    }

    fun getTracksForAlbum(albumId: Long): List<Track> {
        return _tracks.value
            .filter { it.albumId == albumId }
            .sortedWith(compareBy({ it.trackNo }, { it.title }))
    }

    fun playNextAlbum(albumId: Long) {
        val tracks = getTracksForAlbum(albumId)
        playback.playNext(tracks)
    }

    fun addAlbumToQueue(albumId: Long) {
        val tracks = getTracksForAlbum(albumId)
        playback.addToQueue(tracks)
    }

    fun playNextAlbums(albumIds: Set<Long>) {
        val tracks = albumIds.flatMap { getTracksForAlbum(it) }
        playback.playNext(tracks)
    }

    fun addAlbumsToQueue(albumIds: Set<Long>) {
        val tracks = albumIds.flatMap { getTracksForAlbum(it) }
        playback.addToQueue(tracks)
    }

    fun saveCustomAlbumArt(albumId: Long, imageUri: android.net.Uri): Boolean {
        return com.prinzeugen.nitorimusic.data.CustomArtworkManager.saveCustomArtwork(
            getApplication(),
            albumId,
            imageUri
        )
    }

    fun removeCustomAlbumArt(albumId: Long): Boolean {
        return com.prinzeugen.nitorimusic.data.CustomArtworkManager.deleteCustomArtwork(
            getApplication(),
            albumId
        )
    }

    fun hasCustomAlbumArt(albumId: Long): Boolean {
        return com.prinzeugen.nitorimusic.data.CustomArtworkManager.hasCustomArtwork(
            getApplication(),
            albumId
        )
    }

    fun getTracksForArtist(artistName: String): List<Track> {
        return _tracks.value
            .filter { it.artist.equals(artistName, ignoreCase = true) }
            .sortedWith(compareBy({ it.album }, { it.trackNo }, { it.title }))
    }

    fun getTracksForFolder(folderPath: String): List<Track> {
        return _tracks.value
            .filter { it.folderPath == folderPath }
            .sortedWith(compareBy({ it.trackNo }, { it.title }))
    }

    fun getArtists(): List<Artist> {
        return _tracks.value.groupBy { it.artist }
            .map { (artistName, list) ->
                Artist(
                    name = artistName,
                    trackCount = list.size,
                    albumCount = list.map { it.albumId }.distinct().size,
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun getFolders(): List<Folder> {
        return _tracks.value.groupBy { it.folderPath }
            .filter { it.key.isNotEmpty() }
            .map { (folderPath, list) ->
                Folder(
                    path = folderPath,
                    name = folderPath.substringAfterLast("/"),
                    trackCount = list.size,
                )
            }
            .sortedBy { it.name.lowercase() }
    }
}

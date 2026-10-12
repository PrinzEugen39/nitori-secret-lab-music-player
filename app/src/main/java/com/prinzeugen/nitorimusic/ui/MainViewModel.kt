package com.prinzeugen.nitorimusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prinzeugen.nitorimusic.R
import com.prinzeugen.nitorimusic.data.LibraryRepository
import com.prinzeugen.nitorimusic.data.SettingsStore
import com.prinzeugen.nitorimusic.domain.model.Album
import com.prinzeugen.nitorimusic.domain.model.Artist
import com.prinzeugen.nitorimusic.domain.model.Folder
import com.prinzeugen.nitorimusic.domain.model.LibraryTab
import com.prinzeugen.nitorimusic.domain.model.ListOptions
import com.prinzeugen.nitorimusic.domain.model.MusicFolder
import com.prinzeugen.nitorimusic.domain.model.PlaybackRepeatMode
import com.prinzeugen.nitorimusic.domain.model.ShuffleMode
import com.prinzeugen.nitorimusic.domain.model.SortType
import com.prinzeugen.nitorimusic.domain.model.Track
import com.prinzeugen.nitorimusic.domain.model.ViewLayout
import com.prinzeugen.nitorimusic.domain.shuffle.SmartShuffle
import com.prinzeugen.nitorimusic.playback.PlaybackController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
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

    val albumArtFitMode: StateFlow<Boolean> = settings.albumArtFitMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val keepQueueOnPlay: StateFlow<Boolean> = settings.keepQueueOnPlay
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val countAsPlayedThreshold: StateFlow<Int> = settings.countAsPlayedThreshold
        .stateIn(viewModelScope, SharingStarted.Eagerly, 50)

    val selectedMusicFolders: StateFlow<List<MusicFolder>> = settings.selectedMusicFolders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val appLanguage: StateFlow<String> = settings.appLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    fun setAppLanguage(languageCode: String) {
        viewModelScope.launch { settings.setAppLanguage(languageCode) }
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanStatus = MutableStateFlow<String?>(null)
    val scanStatus: StateFlow<String?> = _scanStatus.asStateFlow()

    // State untuk detail view saat klik album/artist/folder (null jika di list utama)
    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()

    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder: StateFlow<Folder?> = _selectedFolder.asStateFlow()

    private var originalQueueTracks: List<Track> = emptyList()
    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)
    val shuffleMode: StateFlow<ShuffleMode> = _shuffleMode.asStateFlow()
    val isShuffleOn: StateFlow<Boolean> = _shuffleMode.map { it.isActive }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isInitialLoaded = MutableStateFlow(false)
    val isInitialLoaded: StateFlow<Boolean> = _isInitialLoaded.asStateFlow()

    init {
        playback.connect()
        observePlayCountTracking()
        loadCachedTracksImmediately()
        loadPersistedPlaybackModes()
    }

    private fun loadPersistedPlaybackModes() {
        viewModelScope.launch {
            val modeStr = settings.playbackShuffleMode.first()
            val mode = runCatching { ShuffleMode.valueOf(modeStr) }.getOrDefault(ShuffleMode.OFF)
            _shuffleMode.value = mode
        }
        viewModelScope.launch {
            val modeStr = settings.playbackRepeatMode.first()
            val mode = runCatching { PlaybackRepeatMode.valueOf(modeStr) }.getOrDefault(PlaybackRepeatMode.PLAY_CATEGORY)
            playback.setPlaybackRepeatMode(mode)
        }
    }

    fun setPlaybackRepeatMode(mode: PlaybackRepeatMode) {
        playback.setPlaybackRepeatMode(mode)
        viewModelScope.launch {
            settings.setPlaybackRepeatMode(mode.name)
        }
    }

    /**
     * Memuat cache Room DB secepat kilat (0-5ms) saat ViewModel tercipta,
     * sehingga UI langsung menampilkan album, lagu, dan ukuran data tanpa jeda/layar hitam.
     */
    private fun loadCachedTracksImmediately() {
        viewModelScope.launch {
            try {
                val cached = repository.getCachedTracks()
                if (cached.isNotEmpty()) {
                    _tracks.value = cached
                }
            } finally {
                _isInitialLoaded.value = true
            }
        }
    }

    private fun observePlayCountTracking() {
        var lastMediaId: String? = null
        var hasCountedCurrent = false

        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val state = playback.state.value
                val currentMediaId = state.currentMediaId
                if (currentMediaId != lastMediaId) {
                    lastMediaId = currentMediaId
                    hasCountedCurrent = false
                }

                if (!hasCountedCurrent && state.isPlaying && currentMediaId != null) {
                    val tId = currentMediaId.substringBefore('_').toLongOrNull()
                    val dur = state.durationMs
                    val pos = playback.positionMs()
                    val thresholdPercent = countAsPlayedThreshold.value
                    if (dur > 0 && pos >= (dur * thresholdPercent / 100)) {
                        hasCountedCurrent = true
                        if (tId != null) {
                            incrementPlayCount(tId)
                        }
                    }
                }
            }
        }
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
     * Memastikan pustaka musik dimuat saat aplikasi dibuka (Startup).
     * Sesuai standar Poweramp & MusicBee:
     * - Jika cache Room DB sudah ada, gunakan cache langsung (0ms) TANPA memicu scanning ulang di latar belakang.
     * - Hanya jika cache masih benar-benar kosong (pertama kali install aplikasi / data dibersihkan),
     *   maka otomatis lakukan sinkronisasi pertama kali.
     */
    fun ensureLibraryLoaded() {
        viewModelScope.launch {
            val configured = settings.selectedMusicFolders.first()
            val enabledFolders = configured.filter { it.isEnabled }.map { it.path }
            val cached = repository.getCachedTracks(enabledFolders)
            if (cached.isNotEmpty()) {
                _tracks.value = cached
            } else if (configured.isNotEmpty()) {
                refreshLibrary(forceSync = true)
            }
            // Jika folder belum dipilih (pertama kali buka / kosong),
            // JANGAN lakukan blind scan seluruh HP! UI akan memandu memilih folder.
        }
    }

    /**
     * Memuat library secara offline-first ala Poweramp:
     * Selalu pastikan Room DB cache tampil langsung sehingga UI 0ms instan,
     * lalu lakukan sinkronisasi MediaStore & CUE parsing di background.
     */
    fun refreshLibrary(forceSync: Boolean = false) {
        viewModelScope.launch {
            val enabledFolders = settings.selectedMusicFolders.first().filter { it.isEnabled }.map { it.path }
            if (!forceSync || _tracks.value.isEmpty()) {
                val cached = repository.getCachedTracks(enabledFolders)
                if (cached.isNotEmpty()) {
                    _tracks.value = cached
                }
            }
            _isScanning.value = true
            _scanStatus.value = getApplication<Application>().getString(R.string.scan_checking_files)
            try {
                val synced = repository.syncFromMediaStore(enabledFolders = enabledFolders) { status ->
                    _scanStatus.value = status
                }
                _tracks.value = synced
            } finally {
                _isScanning.value = false
                _scanStatus.value = null
            }
        }
    }

    fun rescanLibrary() {
        refreshLibrary(forceSync = true)
    }

    fun saveMusicFolders(folders: List<MusicFolder>) {
        val enabledFolders = folders.filter { it.isEnabled }
        if (enabledFolders.isEmpty()) return
        viewModelScope.launch {
            settings.setSelectedMusicFolders(folders)
            refreshLibrary(forceSync = true)
        }
    }

    fun clearCacheAndRescan() {
        viewModelScope.launch {
            val configured = settings.selectedMusicFolders.first()
            val enabledFolders = configured.filter { it.isEnabled }.map { it.path }
            _isScanning.value = true
            _scanStatus.value = getApplication<Application>().getString(R.string.scan_clearing_cache)
            try {
                repository.clearCache()
                val synced = repository.syncFromMediaStore(enabledFolders = enabledFolders) { status ->
                    _scanStatus.value = status
                }
                _tracks.value = synced
            } finally {
                _isScanning.value = false
                _scanStatus.value = null
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

    fun setAlbumArtFitMode(enabled: Boolean) {
        viewModelScope.launch { settings.setAlbumArtFitMode(enabled) }
    }

    fun setKeepQueueOnPlay(enabled: Boolean) {
        viewModelScope.launch { settings.setKeepQueueOnPlay(enabled) }
    }

    fun setCountAsPlayedThreshold(threshold: Int) {
        viewModelScope.launch { settings.setCountAsPlayedThreshold(threshold) }
    }

    fun setAlbumsOptions(options: ListOptions) {
        viewModelScope.launch { settings.setAlbumsOptions(options) }
    }

    fun setTracksOptions(options: ListOptions) {
        viewModelScope.launch { settings.setTracksOptions(options) }
    }

    fun play(index: Int) {
        val tracks = sortedTracks()
        val targetTrack = tracks.getOrNull(index) ?: return
        if (keepQueueOnPlay.value && playback.state.value.queue.isNotEmpty()) {
            playback.playTrackInQueueOrInsert(targetTrack)
        } else {
            originalQueueTracks = tracks
            _shuffleMode.value = ShuffleMode.OFF
            viewModelScope.launch { settings.setPlaybackShuffleMode(ShuffleMode.OFF.name) }
            playback.playAll(tracks, index)
        }
    }

    fun playTracks(trackList: List<Track>, startIndex: Int = 0) {
        val targetTrack = trackList.getOrNull(startIndex) ?: return
        if (keepQueueOnPlay.value && playback.state.value.queue.isNotEmpty()) {
            playback.playTrackInQueueOrInsert(targetTrack)
        } else {
            originalQueueTracks = trackList
            _shuffleMode.value = ShuffleMode.OFF
            viewModelScope.launch { settings.setPlaybackShuffleMode(ShuffleMode.OFF.name) }
            playback.playAll(trackList, startIndex)
        }
    }

    fun incrementPlayCount(trackId: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.incrementPlayCount(trackId, now)
            _tracks.value = _tracks.value.map {
                if (it.id == trackId) it.copy(playCount = it.playCount + 1, lastPlayedMs = now)
                else it
            }
        }
    }

    fun getMostPlayedTracks(): List<Track> {
        return _tracks.value
            .filter { it.playCount > 0 }
            .sortedWith(compareByDescending<Track> { it.playCount }.thenByDescending { it.lastPlayedMs })
    }

    fun getTotalTracksSize(tracks: List<Track> = _tracks.value): Long {
        return tracks.sumOf { it.sizeBytes }
    }

    fun shuffleTracks(trackList: List<Track>) {
        if (trackList.isEmpty()) return
        originalQueueTracks = trackList
        _shuffleMode.value = ShuffleMode.ALL
        viewModelScope.launch { settings.setPlaybackShuffleMode(ShuffleMode.ALL.name) }
        val engine = SmartShuffle(shuffleSpread.value)
        playback.playAll(engine.shuffle(trackList), 0)
    }

    /** Bangun urutan acak sekali dari seluruh library */
    fun shuffleAll() {
        shuffleTracks(_tracks.value)
    }

    /**
     * Poweramp-style Shuffle Mode Selector:
     * Mengatur mode acak (Off, All, Songs, Categories, Songs/Categories)
     * tanpa memotong audio lagu yang sedang berjalan.
     */
    fun setShuffleMode(mode: ShuffleMode) {
        val currentQueue = playback.state.value.queue
        val currentMediaId = playback.state.value.currentMediaId
        val currentTrackId = currentMediaId?.substringBefore('_')?.toLongOrNull()
        val currentPos = playback.positionMs()

        _shuffleMode.value = mode
        viewModelScope.launch { settings.setPlaybackShuffleMode(mode.name) }

        if (currentQueue.isEmpty() || currentTrackId == null) return

        var baseTracks = originalQueueTracks
        if (baseTracks.isEmpty()) {
            val currentTrack = _tracks.value.find { it.id == currentTrackId }
            baseTracks = if (currentTrack != null) {
                getTracksForAlbum(currentTrack.albumId).ifEmpty {
                    _tracks.value.filter { t -> currentQueue.any { it.trackId == t.id } }
                }
            } else {
                _tracks.value.filter { t -> currentQueue.any { it.trackId == t.id } }
            }
            originalQueueTracks = baseTracks
        }

        val currentTrack = baseTracks.find { it.id == currentTrackId }
            ?: _tracks.value.find { it.id == currentTrackId }
            ?: return

        when (mode) {
            ShuffleMode.OFF -> {
                val originalIndex = baseTracks.indexOfFirst { it.id == currentTrack.id }.coerceAtLeast(0)
                playback.setQueueKeepPosition(baseTracks, startIndex = originalIndex, positionMs = currentPos)
            }
            ShuffleMode.ALL -> {
                val engine = SmartShuffle(shuffleSpread.value)
                val otherTracks = _tracks.value.filter { it.id != currentTrack.id }
                val shuffledOthers = engine.shuffle(otherTracks)
                val newQueue = listOf(currentTrack) + shuffledOthers
                playback.setQueueKeepPosition(newQueue, startIndex = 0, positionMs = currentPos)
            }
            ShuffleMode.SONGS -> {
                val albumTracks = getTracksForAlbum(currentTrack.albumId).ifEmpty { baseTracks }
                val otherTracks = albumTracks.filter { it.id != currentTrack.id }
                val engine = SmartShuffle(shuffleSpread.value)
                val shuffledOthers = engine.shuffle(otherTracks)
                val newQueue = listOf(currentTrack) + shuffledOthers
                playback.setQueueKeepPosition(newQueue, startIndex = 0, positionMs = currentPos)
            }
            ShuffleMode.CATEGORIES -> {
                val currentAlbumTracks = getTracksForAlbum(currentTrack.albumId).ifEmpty { baseTracks }
                val otherAlbumIds = _tracks.value.map { it.albumId }.distinct().filter { it != currentTrack.albumId }.shuffled()
                val otherAlbumsSequentialTracks = otherAlbumIds.flatMap { albumId ->
                    getTracksForAlbum(albumId)
                }
                val otherCurrentAlbumTracks = currentAlbumTracks.filter { it.id != currentTrack.id }
                val newQueue = listOf(currentTrack) + otherCurrentAlbumTracks + otherAlbumsSequentialTracks
                playback.setQueueKeepPosition(newQueue, startIndex = 0, positionMs = currentPos)
            }
            ShuffleMode.SONGS_AND_CATEGORIES -> {
                val engine = SmartShuffle(shuffleSpread.value)
                val currentAlbumTracks = getTracksForAlbum(currentTrack.albumId).ifEmpty { baseTracks }
                val otherCurrentAlbumTracks = engine.shuffle(currentAlbumTracks.filter { it.id != currentTrack.id })
                val otherAlbumIds = _tracks.value.map { it.albumId }.distinct().filter { it != currentTrack.albumId }.shuffled()
                val otherAlbumsShuffledTracks = otherAlbumIds.flatMap { albumId ->
                    engine.shuffle(getTracksForAlbum(albumId))
                }
                val newQueue = listOf(currentTrack) + otherCurrentAlbumTracks + otherAlbumsShuffledTracks
                playback.setQueueKeepPosition(newQueue, startIndex = 0, positionMs = currentPos)
            }
        }
    }

    fun toggleShuffle() {
        if (_shuffleMode.value.isActive) {
            setShuffleMode(ShuffleMode.OFF)
        } else {
            setShuffleMode(ShuffleMode.ALL)
        }
    }

    /** Re-shuffle ulang seluruh sisa antrean dengan lagu saat ini tetap sebagai lagu #1 */
    fun reshuffleCurrentQueue() {
        val currentQueue = playback.state.value.queue
        if (currentQueue.isEmpty()) return

        val currentMediaId = playback.state.value.currentMediaId ?: return
        val currentTrackId = currentMediaId.substringBefore('_').toLongOrNull() ?: return
        val currentPos = playback.positionMs()

        var baseTracks = originalQueueTracks
        if (baseTracks.isEmpty()) {
            baseTracks = _tracks.value.filter { t -> currentQueue.any { it.trackId == t.id } }
            originalQueueTracks = baseTracks
        }

        val currentTrack = baseTracks.find { it.id == currentTrackId }
            ?: _tracks.value.find { it.id == currentTrackId }
            ?: return

        val otherTracks = baseTracks.filter { it.id != currentTrack.id }
        val engine = SmartShuffle(shuffleSpread.value)
        val shuffledOthers = engine.shuffle(otherTracks)
        val newQueue = listOf(currentTrack) + shuffledOthers

        playback.setQueueKeepPosition(newQueue, startIndex = 0, positionMs = currentPos)
        _shuffleMode.value = ShuffleMode.ALL
        viewModelScope.launch { settings.setPlaybackShuffleMode(ShuffleMode.ALL.name) }
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

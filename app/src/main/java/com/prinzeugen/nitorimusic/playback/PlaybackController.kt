package com.prinzeugen.nitorimusic.playback

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.prinzeugen.nitorimusic.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QueueItem(
    val mediaId: String,
    val title: String,
    val artist: String,
) {
    val trackId: Long? get() = mediaId.toLongOrNull()
}

data class AudioSpecs(
    val codec: String,
    val sampleRateHz: Int = 0,
    val bitDepth: Int = 0,
    val bitrateKbps: Int = 0,
    val channelCount: Int = 2,
) {
    val displayString: String get() {
        val parts = mutableListOf<String>()
        if (codec.isNotEmpty()) parts.add(codec)
        if (bitDepth > 0) parts.add("${bitDepth}-bit")
        if (sampleRateHz > 0) {
            val khz = if (sampleRateHz % 1000 == 0) "${sampleRateHz / 1000} kHz"
                      else String.format(java.util.Locale.US, "%.1f kHz", sampleRateHz / 1000.0)
            parts.add(khz)
        }
        if (bitrateKbps > 0 && bitDepth == 0) {
            parts.add("$bitrateKbps kbps")
        }
        return parts.joinToString(" • ")
    }
}

data class PlaybackUiState(
    val currentMediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val queue: List<QueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val sleepTimerMinutesLeft: Int = 0,
    val sleepTimerSecondsLeft: Int = 0,
    val audioSpecs: AudioSpecs? = null,
)

/** Wrapper MediaController untuk UI. Service tetap pemilik player dan queue. */
class PlaybackController(private val context: Context) {

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            val c = f.get()
            controller = c
            c.addListener(listener)
            publish(c)
        }, ContextCompat.getMainExecutor(context))
    }

    fun playAll(tracks: List<Track>, startIndex: Int) {
        val c = controller ?: return
        c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun playNext(track: Track) {
        val c = controller ?: return
        val insertIndex = if (c.mediaItemCount == 0) 0 else c.currentMediaItemIndex + 1
        c.addMediaItem(insertIndex, track.toMediaItem())
    }

    fun playNext(tracks: List<Track>) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        val insertIndex = if (c.mediaItemCount == 0) 0 else c.currentMediaItemIndex + 1
        c.addMediaItems(insertIndex, tracks.map { it.toMediaItem() })
    }

    fun addToQueue(track: Track) {
        val c = controller ?: return
        c.addMediaItem(track.toMediaItem())
    }

    fun addToQueue(tracks: List<Track>) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        c.addMediaItems(tracks.map { it.toMediaItem() })
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNext() }

    fun previous(forcePreviousItem: Boolean = false) {
        val c = controller ?: return
        if (forcePreviousItem && c.hasPreviousMediaItem()) {
            c.seekToPreviousMediaItem()
            c.play()
        } else {
            c.seekToPrevious()
        }
    }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    fun positionMs(): Long = controller?.currentPosition ?: 0L

    fun jumpTo(index: Int) {
        val c = controller ?: return
        c.seekToDefaultPosition(index)
        c.play()
    }

    fun removeById(mediaId: String) {
        val index = _state.value.queue.indexOfFirst { it.mediaId == mediaId }
        if (index >= 0) removeAt(index)
    }

    fun clearQueue() {
        controller?.clearMediaItems()
    }

    fun removeAt(index: Int) {
        val c = controller ?: return
        _state.value = _state.value.let { s ->
            s.copy(queue = s.queue.filterIndexed { i, _ -> i != index })
        }
        c.removeMediaItem(index)
    }

    fun move(from: Int, to: Int) {
        val c = controller ?: return
        // Update optimistis agar drag tidak patah menunggu round-trip ke service.
        _state.value = _state.value.let { s ->
            val list = s.queue.toMutableList()
            list.add(to, list.removeAt(from))
            s.copy(queue = list)
        }
        c.moveMediaItem(from, to)
    }

    fun toggleRepeatMode() {
        val c = controller ?: return
        val nextMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        c.repeatMode = nextMode
        publish(c)
    }

    private var sleepJob: kotlinx.coroutines.Job? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        val totalSeconds = minutes * 60
        sleepJob = scope.launch {
            for (sec in totalSeconds downTo 1) {
                val m = (sec + 59) / 60
                _state.value = _state.value.copy(
                    sleepTimerMinutesLeft = m,
                    sleepTimerSecondsLeft = sec
                )
                // Fade-out halus 15 detik terakhir sebelum timer habis
                if (sec <= 15) {
                    val fadeVol = (sec.toFloat() / 15f).coerceIn(0f, 1f)
                    controller?.volume = fadeVol
                }
                kotlinx.coroutines.delay(1000L)
            }
            _state.value = _state.value.copy(
                sleepTimerMinutesLeft = 0,
                sleepTimerSecondsLeft = 0
            )
            controller?.pause()
            controller?.volume = 1f
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        controller?.volume = 1f
        _state.value = _state.value.copy(
            sleepTimerMinutesLeft = 0,
            sleepTimerSecondsLeft = 0
        )
    }

    fun release() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        val queue = List(player.mediaItemCount) { i ->
            val m = player.getMediaItemAt(i)
            QueueItem(
                mediaId = m.mediaId,
                title = m.mediaMetadata.title?.toString().orEmpty(),
                artist = m.mediaMetadata.artist?.toString().orEmpty(),
            )
        }
        val specs = extractAudioSpecs(player)
        _state.value = PlaybackUiState(
            currentMediaId = item?.mediaId,
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
            isPlaying = player.isPlaying,
            queue = queue,
            currentIndex = if (item == null) -1 else player.currentMediaItemIndex,
            durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L,
            repeatMode = player.repeatMode,
            sleepTimerMinutesLeft = _state.value.sleepTimerMinutesLeft,
            sleepTimerSecondsLeft = _state.value.sleepTimerSecondsLeft,
            audioSpecs = specs,
        )
    }

    private fun extractAudioSpecs(player: Player): AudioSpecs? {
        val tracks = player.currentTracks
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        val format = group.getTrackFormat(i)
                        val mime = format.sampleMimeType.orEmpty()
                        val codec = when {
                            mime.contains("flac", ignoreCase = true) -> "FLAC"
                            mime.contains("mpeg", ignoreCase = true) || mime.contains("mp3", ignoreCase = true) -> "MP3"
                            mime.contains("mp4a", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC"
                            mime.contains("opus", ignoreCase = true) -> "OPUS"
                            mime.contains("vorbis", ignoreCase = true) || mime.contains("ogg", ignoreCase = true) -> "OGG"
                            mime.contains("wav", ignoreCase = true) || mime.contains("raw", ignoreCase = true) -> "WAV"
                            else -> mime.substringAfter("audio/").uppercase().ifEmpty { "AUDIO" }
                        }
                        val bitDepth = when (format.pcmEncoding) {
                            C.ENCODING_PCM_24BIT -> 24
                            C.ENCODING_PCM_32BIT -> 32
                            C.ENCODING_PCM_16BIT -> 16
                            else -> 0
                        }
                        val sampleRate = if (format.sampleRate > 0) format.sampleRate else 0
                        val bitrate = if (format.bitrate > 0) format.bitrate / 1000 else 0
                        return AudioSpecs(
                            codec = codec,
                            sampleRateHz = sampleRate,
                            bitDepth = bitDepth,
                            bitrateKbps = bitrate,
                            channelCount = format.channelCount
                        )
                    }
                }
            }
        }
        return null
    }

    private fun Track.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(uri)
                    .build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri(albumId))
                    .build()
            )
            .build()

    private fun albumArtUri(albumId: Long): Uri =
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

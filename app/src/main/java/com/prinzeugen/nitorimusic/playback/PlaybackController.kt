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

data class QueueItem(
    val mediaId: String,
    val title: String,
    val artist: String,
) {
    val trackId: Long? get() = mediaId.toLongOrNull()
}

data class PlaybackUiState(
    val currentMediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val queue: List<QueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val durationMs: Long = 0L,
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

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNext() }

    fun previous() { controller?.seekToPrevious() }

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
        _state.value = PlaybackUiState(
            currentMediaId = item?.mediaId,
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
            isPlaying = player.isPlaying,
            queue = queue,
            currentIndex = if (item == null) -1 else player.currentMediaItemIndex,
            durationMs = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L,
        )
    }

    private fun Track.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
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

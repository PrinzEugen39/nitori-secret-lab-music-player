package com.prinzeugen.nitorimusic.playback

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
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

data class PlaybackUiState(
    val currentMediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
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

    fun release() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _state.value = PlaybackUiState(
            currentMediaId = item?.mediaId,
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
            isPlaying = player.isPlaying,
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

package com.prinzeugen.nitorimusic.playback

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.prinzeugen.nitorimusic.data.SettingsStore
import com.prinzeugen.nitorimusic.data.db.AppDatabase
import com.prinzeugen.nitorimusic.data.db.entity.SavedQueueItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow

/**
 * Pemilik tunggal player dan queue. UI terhubung lewat MediaController.
 * Bertanggung jawab atas:
 * 1. Audio Focus & Audio Becoming Noisy (jeda saat headset/bluetooth dicabut atau ada panggilan).
 * 2. Persistensi Queue ke Room Database (restore saat service restart).
 * 3. Persistensi Posisi Playback & Track terakhir ke DataStore.
 * 4. Normalisasi Volume Otomatis (ReplayGain / R128).
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var isRestoring = false
    private var isReplayGainEnabled = true
    private var saveQueueJob: Job? = null
    private var positionTickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Audio Focus handling
            .setHandleAudioBecomingNoisy(true) // Pause otomatis saat headset dicabut
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .build()

        player.addListener(object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (!isRestoring) {
                    persistQueue()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (!isRestoring) {
                    savePlaybackPosition(positionOverride = 0L)
                }
                if (!isReplayGainEnabled) {
                    player.volume = 1f
                }
            }

            override fun onMetadata(metadata: Metadata) {
                if (isReplayGainEnabled) {
                    applyReplayGain(player, metadata)
                } else {
                    player.volume = 1f
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    savePlaybackPosition()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Jika file audio hilang / tidak terbaca, lewati ke lagu berikutnya agar tidak macet
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                }
            }
        })

        observeSettings(player)
        startPositionTicker(player)
        restoreQueueAndPlayback(player)
    }

    private fun observeSettings(player: ExoPlayer) {
        val settings = SettingsStore(applicationContext)
        serviceScope.launch {
            settings.replayGainEnabled.collect { enabled ->
                isReplayGainEnabled = enabled
                withContext(Dispatchers.Main) {
                    if (!enabled) {
                        player.volume = 1f
                    }
                }
            }
        }
    }

    private fun applyReplayGain(player: ExoPlayer, metadata: Metadata) {
        val regex = Regex("""([+-]?\d+(?:\.\d+)?)\s*dB""", RegexOption.IGNORE_CASE)
        for (i in 0 until metadata.length()) {
            val entryStr = metadata.get(i).toString()
            if (entryStr.contains("REPLAYGAIN_TRACK_GAIN", ignoreCase = true) ||
                entryStr.contains("R128_TRACK_GAIN", ignoreCase = true)
            ) {
                val match = regex.find(entryStr)
                val gainDb = match?.groupValues?.getOrNull(1)?.toFloatOrNull()
                if (gainDb != null) {
                    val targetVol = (10.0.pow(gainDb / 20.0)).toFloat().coerceIn(0.15f, 1.25f)
                    player.volume = targetVol
                    return
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        savePlaybackPosition()
        positionTickerJob?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun restoreQueueAndPlayback(player: ExoPlayer) {
        serviceScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val savedQueue = db.queueDao().getQueue()
            if (savedQueue.isNotEmpty()) {
                val settings = SettingsStore(applicationContext)
                val lastPlayback = settings.getLastPlayback().first()
                val mediaItems = savedQueue.map { it.toMediaItem() }

                withContext(Dispatchers.Main) {
                    // Hanya restore jika player belum diisi oleh aksi lain
                    if (player.mediaItemCount == 0) {
                        isRestoring = true
                        val startIndex = lastPlayback.index.coerceIn(0, mediaItems.size - 1)
                        val startPos = lastPlayback.positionMs.coerceAtLeast(0L)
                        player.setMediaItems(mediaItems, startIndex, startPos)
                        player.prepare()
                        isRestoring = false
                    }
                }
            }
        }
    }

    private fun persistQueue() {
        saveQueueJob?.cancel()
        saveQueueJob = serviceScope.launch(Dispatchers.IO) {
            delay(350L) // Debounce agar perpindahan urutan beruntun tidak membebani database
            val session = mediaSession ?: return@launch
            val player = session.player

            val count = withContext(Dispatchers.Main) { player.mediaItemCount }
            val items = ArrayList<SavedQueueItem>(count)

            withContext(Dispatchers.Main) {
                for (i in 0 until player.mediaItemCount) {
                    val m = player.getMediaItemAt(i)
                    val uriStr = m.localConfiguration?.uri?.toString()
                        ?: m.requestMetadata.mediaUri?.toString()
                        ?: ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            m.mediaId.substringBefore('_').toLongOrNull() ?: 0L
                        ).toString()
                    val albumId = m.mediaMetadata.artworkUri?.lastPathSegment?.toLongOrNull() ?: 0L
                    val clipConfig = m.clippingConfiguration
                    val startMs = if (clipConfig.startPositionMs != androidx.media3.common.C.TIME_UNSET) clipConfig.startPositionMs else 0L
                    val endMs = if (clipConfig.endPositionMs != androidx.media3.common.C.TIME_END_OF_SOURCE && clipConfig.endPositionMs != androidx.media3.common.C.TIME_UNSET) clipConfig.endPositionMs else 0L
                    val isCue = (endMs > startMs || startMs > 0L)

                    items.add(
                        SavedQueueItem(
                            position = i,
                            trackId = m.mediaId.substringBefore('_').toLongOrNull() ?: 0L,
                            uriString = uriStr,
                            title = m.mediaMetadata.title?.toString().orEmpty(),
                            artist = m.mediaMetadata.artist?.toString().orEmpty(),
                            album = m.mediaMetadata.albumTitle?.toString().orEmpty(),
                            albumId = albumId,
                            startMs = startMs,
                            endMs = endMs,
                            isCue = isCue,
                        )
                    )
                }
            }

            val db = AppDatabase.getInstance(applicationContext)
            db.queueDao().replaceQueue(items)
        }
    }

    private fun savePlaybackPosition(positionOverride: Long? = null) {
        val player = mediaSession?.player ?: return
        val item = player.currentMediaItem
        val index = player.currentMediaItemIndex
        val pos = positionOverride ?: player.currentPosition
        val mediaId = item?.mediaId

        serviceScope.launch(Dispatchers.IO) {
            val settings = SettingsStore(applicationContext)
            settings.saveLastPlayback(mediaId, index, pos)
        }
    }

    private fun startPositionTicker(player: ExoPlayer) {
        positionTickerJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(5000L)
                withContext(Dispatchers.Main) {
                    if (player.isPlaying) {
                        savePlaybackPosition()
                    }
                }
            }
        }
    }

    private fun SavedQueueItem.toMediaItem(): MediaItem {
        val uri = Uri.parse(uriString)
        val artUri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
        val uid = java.util.UUID.randomUUID().toString().take(8)
        val builder = MediaItem.Builder()
            .setMediaId("${trackId}_$uid")
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
                    .setArtworkUri(artUri)
                    .build()
            )
        if (isCue && endMs > startMs) {
            builder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
        }
        return builder.build()
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val resolved = mediaItems.map { resolveMediaItem(it) }
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(resolved, startIndex, startPositionMs)
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { resolveMediaItem(it) }.toMutableList()
            return Futures.immediateFuture(resolved)
        }

        private fun resolveMediaItem(item: MediaItem): MediaItem {
            if (item.localConfiguration?.uri != null) return item
            val uri = item.requestMetadata.mediaUri ?: ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                item.mediaId.substringBefore('_').toLongOrNull() ?: 0L
            )
            return item.buildUpon().setUri(uri).build()
        }
    }
}

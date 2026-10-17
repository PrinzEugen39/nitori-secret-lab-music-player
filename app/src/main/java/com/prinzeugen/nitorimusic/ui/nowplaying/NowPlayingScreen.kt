package com.prinzeugen.nitorimusic.ui.nowplaying

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import com.prinzeugen.nitorimusic.R
import com.prinzeugen.nitorimusic.data.ArtworkLoader
import com.prinzeugen.nitorimusic.domain.model.PlaybackRepeatMode
import com.prinzeugen.nitorimusic.domain.model.ShuffleMode
import com.prinzeugen.nitorimusic.playback.AudioSpecs
import com.prinzeugen.nitorimusic.playback.QueueItem
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.components.ArtworkImage
import com.prinzeugen.nitorimusic.ui.components.CueBadge
import com.prinzeugen.nitorimusic.ui.components.TrackDetailActionDialog
import com.prinzeugen.nitorimusic.ui.formatTime
import com.prinzeugen.nitorimusic.ui.library.ShuffleOptionsDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(viewModel: MainViewModel, onBack: () -> Unit, onOpenQueue: () -> Unit) {
    val playback = viewModel.playback
    val state by playback.state.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    var position by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showTrackDetail by remember { mutableStateOf(false) }
    var showShuffleModeDialog by remember { mutableStateOf(false) }
    var showRepeatModeDialog by remember { mutableStateOf(false) }
    var showShuffleOptionsDialog by remember { mutableStateOf(false) }
    var dominantColor by remember { mutableStateOf(Color.Transparent) }

    val albumArtFitMode by viewModel.albumArtFitMode.collectAsState()
    val isShuffleOn by viewModel.isShuffleOn.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()
    val shuffleSpread by viewModel.shuffleSpread.collectAsState()
    val allTracks by viewModel.tracks.collectAsState()
    val currentTrack = remember(state.currentMediaId, allTracks) {
        val tId = state.currentTrackId
        allTracks.find { it.id == tId }
    }

    // Dynamic Material You Dominant Color Extraction dari Cover Art
    LaunchedEffect(state.currentMediaId) {
        val tId = state.currentTrackId
        if (tId != null) {
            val bmp = withContext(Dispatchers.IO) {
                ArtworkLoader.loadArtwork(context, tId, null, 120)
            }
            if (bmp != null) {
                val palette = withContext(Dispatchers.Default) {
                    try {
                        Palette.from(bmp).maximumColorCount(16).generate()
                    } catch (_: Exception) {
                        null
                    }
                }
                val swatch = palette?.dominantSwatch ?: palette?.vibrantSwatch ?: palette?.mutedSwatch
                dominantColor = if (swatch != null) Color(swatch.rgb) else Color.Transparent
            } else {
                dominantColor = Color.Transparent
            }
        } else {
            dominantColor = Color.Transparent
        }
    }

    val animatedAmbientColor by animateColorAsState(
        targetValue = if (dominantColor != Color.Transparent) dominantColor else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 650),
        label = "NowPlayingAmbientGradient"
    )

    if (showSleepTimer) {
        SleepTimerDialog(
            currentMinutesLeft = state.sleepTimerMinutesLeft,
            currentSecondsLeft = state.sleepTimerSecondsLeft,
            onSetTimer = playback::setSleepTimer,
            onCancelTimer = playback::cancelSleepTimer,
            onDismiss = { showSleepTimer = false }
        )
    }

    if (showTrackDetail && currentTrack != null) {
        TrackDetailActionDialog(
            track = currentTrack,
            audioSpecs = state.audioSpecs,
            onPlayNext = { playback.playNext(currentTrack) },
            onAddToQueue = { playback.addToQueue(currentTrack) },
            onGoToAlbum = { albumId ->
                val album = viewModel.getAlbums().find { it.id == albumId }
                if (album != null) {
                    viewModel.openAlbum(album)
                    onBack()
                }
            },
            onGoToArtist = { artistName ->
                val artist = viewModel.getArtists().find { it.name.equals(artistName, ignoreCase = true) }
                if (artist != null) {
                    viewModel.openArtist(artist)
                    onBack()
                }
            },
            onChangeAlbumArt = { albumId, uri ->
                viewModel.saveCustomAlbumArt(albumId, uri)
            },
            onDismiss = { showTrackDetail = false }
        )
    }

    if (showShuffleModeDialog) {
        ShuffleModeDialog(
            currentMode = shuffleMode,
            onSelectMode = { mode -> viewModel.setShuffleMode(mode) },
            onOpenSettings = { showShuffleOptionsDialog = true },
            onDismiss = { showShuffleModeDialog = false }
        )
    }

    if (showShuffleOptionsDialog) {
        ShuffleOptionsDialog(
            spread = shuffleSpread,
            onSpreadChange = viewModel::setShuffleSpread,
            onDismiss = { showShuffleOptionsDialog = false }
        )
    }

    if (showRepeatModeDialog) {
        RepeatModeDialog(
            currentMode = state.playbackRepeatMode,
            onSelectMode = { mode -> viewModel.setPlaybackRepeatMode(mode) },
            onDismiss = { showRepeatModeDialog = false }
        )
    }

    LaunchedEffect(state.currentMediaId, state.isPlaying) {
        while (true) {
            if (!seeking) position = playback.positionMs()
            delay(500)
        }
    }

    val trackId = state.currentTrackId
    val duration = state.durationMs.coerceAtLeast(1L)
    val queue = state.queue
    val safeIndex = if (state.currentIndex in queue.indices) state.currentIndex else 0
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = safeIndex,
        pageCount = { queue.size.coerceAtLeast(1) }
    )

    LaunchedEffect(state.currentIndex) {
        if (state.currentIndex in queue.indices && pagerState.currentPage != state.currentIndex) {
            pagerState.animateScrollToPage(state.currentIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && state.currentIndex in queue.indices && pagerState.currentPage != state.currentIndex) {
            playback.jumpTo(pagerState.currentPage)
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val backgroundBrush = remember(animatedAmbientColor, isLandscape, backgroundColor) {
        if (isLandscape) {
            Brush.horizontalGradient(
                colors = listOf(
                    animatedAmbientColor.copy(alpha = 0.50f),
                    animatedAmbientColor.copy(alpha = 0.20f),
                    backgroundColor,
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    animatedAmbientColor.copy(alpha = 0.45f),
                    animatedAmbientColor.copy(alpha = 0.15f),
                    backgroundColor,
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        val density = LocalDensity.current
        val swipeDownThreshold = with(density) { 60.dp.toPx() }
        var dragOffsetY by remember { mutableFloatStateOf(0f) }

        if (isLandscape) {
            // LANDSCAPE LAYOUT (Split-view Dual Pane for Phones & Tablets)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = if (isTablet) 32.dp else 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pane: Large Album Artwork (Bounded by available height)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .padding(vertical = 4.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { dragOffsetY = 0f },
                                    onDragEnd = { dragOffsetY = 0f },
                                    onDragCancel = { dragOffsetY = 0f }
                                ) { _, dragAmount ->
                                    if (dragAmount > 0 || dragOffsetY > 0) {
                                        dragOffsetY += dragAmount
                                        if (dragOffsetY > swipeDownThreshold) {
                                            dragOffsetY = 0f
                                            onBack()
                                        }
                                    }
                                }
                            }
                    ) {
                        ArtworkPager(
                            queue = queue,
                            pagerState = pagerState,
                            trackId = trackId,
                            albumArtFitMode = albumArtFitMode,
                            onLongPress = { showTrackDetail = true }
                        )
                    }
                }

                // Right Pane: Header, Song Info, Tools, Seekbar, Controls, Specs
                Column(
                    modifier = Modifier
                        .weight(if (isTablet) 1.2f else 1.35f)
                        .fillMaxHeight()
                        .padding(start = if (isTablet) 24.dp else 12.dp, end = 4.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Header Bar: Back Button, Sleep Timer Countdown, Queue Shortcut
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (state.sleepTimerSecondsLeft > 0) {
                            SleepTimerLiveBadge(
                                secondsLeft = state.sleepTimerSecondsLeft,
                                onClick = { showSleepTimer = true }
                            )
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        IconButton(onClick = onOpenQueue, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = stringResource(R.string.queue),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Song Info (Title, Artist, Album)
                    TrackInfoSection(
                        title = state.title,
                        artist = state.artist,
                        album = state.album,
                        isCue = (state.currentTrackId ?: 0L) < 0L,
                        onLongPress = { showTrackDetail = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                    )

                    // Tools Row (Shuffle, Sleep Timer, Queue Pill, Repeat)
                    PlaybackToolsRow(
                        isShuffleOn = isShuffleOn,
                        onToggleShuffle = viewModel::toggleShuffle,
                        onOpenShuffleDialog = { showShuffleModeDialog = true },
                        sleepTimerSecondsLeft = state.sleepTimerSecondsLeft,
                        onOpenSleepTimer = { showSleepTimer = true },
                        currentIndex = state.currentIndex,
                        queueSize = queue.size,
                        onOpenQueue = onOpenQueue,
                        repeatMode = state.playbackRepeatMode,
                        onToggleRepeat = playback::toggleRepeatMode,
                        onOpenRepeatDialog = { showRepeatModeDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                    )

                    // Seekbar
                    PlaybackSeekBar(
                        position = position,
                        duration = duration,
                        seeking = seeking,
                        seekValue = seekValue,
                        onSeekChange = {
                            seeking = true
                            seekValue = it
                        },
                        onSeekFinish = {
                            playback.seekTo((seekValue * duration).toLong())
                            seeking = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                    )

                    // Playback Buttons (Previous, Play/Pause, Next)
                    PlaybackControlButtons(
                        isPlaying = state.isPlaying,
                        onPrevious = { playback.previous(forcePreviousItem = true) },
                        onTogglePlayPause = playback::togglePlayPause,
                        onNext = playback::next,
                        isCompact = !isTablet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                    )

                    // Audio Specs Badge
                    state.audioSpecs?.let { specs ->
                        AudioSpecsBadge(specs = specs)
                    }
                }
            }
        } else {
            // PORTRAIT LAYOUT (Adaptive for Phones & Portrait Tablets)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = if (isTablet) 32.dp else 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top Area: Sleep Timer Live Countdown Badge (jika aktif)
                if (state.sleepTimerSecondsLeft > 0) {
                    SleepTimerLiveBadge(
                        secondsLeft = state.sleepTimerSecondsLeft,
                        onClick = { showSleepTimer = true }
                    )
                } else {
                    Spacer(Modifier.height(if (isTablet) 12.dp else 16.dp))
                }

                Spacer(Modifier.height(4.dp))

                // Large Album Artwork Carousel
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(
                            if (isTablet) {
                                Modifier.sizeIn(maxWidth = 420.dp, maxHeight = 420.dp)
                            } else {
                                Modifier.fillMaxWidth()
                            }
                        )
                        .aspectRatio(1f)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { dragOffsetY = 0f },
                                onDragEnd = { dragOffsetY = 0f },
                                onDragCancel = { dragOffsetY = 0f }
                            ) { _, dragAmount ->
                                if (dragAmount > 0 || dragOffsetY > 0) {
                                    dragOffsetY += dragAmount
                                    if (dragOffsetY > swipeDownThreshold) {
                                        dragOffsetY = 0f
                                        onBack()
                                    }
                                }
                            }
                        }
                ) {
                    ArtworkPager(
                        queue = queue,
                        pagerState = pagerState,
                        trackId = trackId,
                        albumArtFitMode = albumArtFitMode,
                        onLongPress = { showTrackDetail = true }
                    )
                }

                Spacer(Modifier.height(if (isTablet) 14.dp else 10.dp))

                // Controls Column (Constrained with max width on tablet portrait)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Song Info
                    TrackInfoSection(
                        title = state.title,
                        artist = state.artist,
                        album = state.album,
                        isCue = (state.currentTrackId ?: 0L) < 0L,
                        onLongPress = { showTrackDetail = true }
                    )

                    Spacer(Modifier.height(6.dp))

                    // Tools Row
                    PlaybackToolsRow(
                        isShuffleOn = isShuffleOn,
                        onToggleShuffle = viewModel::toggleShuffle,
                        onOpenShuffleDialog = { showShuffleModeDialog = true },
                        sleepTimerSecondsLeft = state.sleepTimerSecondsLeft,
                        onOpenSleepTimer = { showSleepTimer = true },
                        currentIndex = state.currentIndex,
                        queueSize = queue.size,
                        onOpenQueue = onOpenQueue,
                        repeatMode = state.playbackRepeatMode,
                        onToggleRepeat = playback::toggleRepeatMode,
                        onOpenRepeatDialog = { showRepeatModeDialog = true }
                    )

                    // Seekbar
                    PlaybackSeekBar(
                        position = position,
                        duration = duration,
                        seeking = seeking,
                        seekValue = seekValue,
                        onSeekChange = {
                            seeking = true
                            seekValue = it
                        },
                        onSeekFinish = {
                            playback.seekTo((seekValue * duration).toLong())
                            seeking = false
                        }
                    )

                    Spacer(Modifier.height(if (isTablet) 14.dp else 10.dp))

                    // Playback Buttons
                    PlaybackControlButtons(
                        isPlaying = state.isPlaying,
                        onPrevious = { playback.previous(forcePreviousItem = true) },
                        onTogglePlayPause = playback::togglePlayPause,
                        onNext = playback::next,
                        isCompact = false
                    )

                    Spacer(Modifier.weight(1f))

                    // Audio Specs Badge
                    state.audioSpecs?.let { specs ->
                        AudioSpecsBadge(specs = specs)
                    }

                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtworkPager(
    queue: List<QueueItem>,
    pagerState: PagerState,
    trackId: Long?,
    albumArtFitMode: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (queue.isNotEmpty()) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
            pageSpacing = 16.dp,
        ) { page ->
            val item = queue.getOrNull(page)
            ArtworkImage(
                trackId = item?.trackId,
                contentScale = if (albumArtFitMode) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onLongPress() }
                        )
                    },
            )
        }
    } else {
        ArtworkImage(
            trackId = trackId,
            contentScale = if (albumArtFitMode) ContentScale.Fit else ContentScale.Crop,
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() }
                    )
                },
        )
    }
}

@Composable
private fun SleepTimerLiveBadge(
    secondsLeft: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val min = secondsLeft / 60
    val sec = secondsLeft % 60
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏳ %02d:%02d".format(min, sec),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackInfoSection(
    title: String,
    artist: String,
    album: String,
    isCue: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title.ifEmpty { stringResource(R.string.now_playing_no_track) },
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = artist.ifEmpty { stringResource(R.string.now_playing_unknown_artist) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isCue) {
                Spacer(Modifier.width(6.dp))
                CueBadge()
            }
        }
        if (album.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = album,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaybackToolsRow(
    isShuffleOn: Boolean,
    onToggleShuffle: () -> Unit,
    onOpenShuffleDialog: () -> Unit,
    sleepTimerSecondsLeft: Int,
    onOpenSleepTimer: () -> Unit,
    currentIndex: Int,
    queueSize: Int,
    onOpenQueue: () -> Unit,
    repeatMode: PlaybackRepeatMode,
    onToggleRepeat: () -> Unit,
    onOpenRepeatDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onToggleShuffle,
                    onLongClick = onOpenShuffleDialog,
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.shuffle),
                tint = if (isShuffleOn) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onOpenSleepTimer) {
            Icon(
                Icons.Rounded.Timer,
                contentDescription = stringResource(R.string.sleep_timer_title_set),
                tint = if (sleepTimerSecondsLeft > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenQueue)
        ) {
            Text(
                text = "${(currentIndex + 1).coerceAtLeast(1)} / ${queueSize.coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        val repeatIcon = when (repeatMode) {
            PlaybackRepeatMode.REPEAT_SONG -> Icons.Rounded.RepeatOne
            else -> Icons.Rounded.Repeat
        }
        val repeatTint = if (repeatMode.isRepeating) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onToggleRepeat,
                    onLongClick = onOpenRepeatDialog,
                ),
            contentAlignment = Alignment.Center
        ) {
            if (repeatMode == PlaybackRepeatMode.PLAY_SINGLE_SONG) {
                Text(
                    text = "1",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    ),
                    color = repeatTint
                )
            } else {
                Icon(
                    repeatIcon,
                    contentDescription = stringResource(R.string.repeat),
                    tint = repeatTint
                )
            }
        }
    }
}

@Composable
private fun PlaybackSeekBar(
    position: Long,
    duration: Long,
    seeking: Boolean,
    seekValue: Float,
    onSeekChange: (Float) -> Unit,
    onSeekFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentPos = if (seeking) (seekValue * duration).toLong() else position
    Column(modifier = modifier) {
        Slider(
            value = (currentPos.toFloat() / duration).coerceIn(0f, 1f),
            onValueChange = onSeekChange,
            onValueChangeFinished = onSeekFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPos), style = MaterialTheme.typography.bodySmall)
            Text(formatTime(duration), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlaybackControlButtons(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val playPauseSize = if (isCompact) 56.dp else 64.dp
    val playPauseIconSize = if (isCompact) 32.dp else 36.dp
    val prevNextSize = if (isCompact) 44.dp else 48.dp
    val prevNextIconSize = if (isCompact) 28.dp else 32.dp
    val spacerSize = if (isCompact) 18.dp else 24.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(prevNextSize)
        ) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.previous),
                modifier = Modifier.size(prevNextIconSize)
            )
        }

        Spacer(Modifier.size(spacerSize))

        Box(
            modifier = Modifier
                .size(playPauseSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(playPauseSize)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(playPauseIconSize)
                )
            }
        }

        Spacer(Modifier.size(spacerSize))

        IconButton(
            onClick = onNext,
            modifier = Modifier.size(prevNextSize)
        ) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.next),
                modifier = Modifier.size(prevNextIconSize)
            )
        }
    }
}

@Composable
private fun AudioSpecsBadge(
    specs: AudioSpecs,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = specs.displayString,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

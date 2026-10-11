package com.prinzeugen.nitorimusic.ui.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import com.prinzeugen.nitorimusic.data.ArtworkLoader
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.components.ArtworkImage
import com.prinzeugen.nitorimusic.ui.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun NowPlayingScreen(viewModel: MainViewModel, onBack: () -> Unit, onOpenQueue: () -> Unit) {
    val playback = viewModel.playback
    val state by playback.state.collectAsState()
    val context = LocalContext.current

    var position by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var dominantColor by remember { mutableStateOf(Color.Transparent) }

    // Dynamic Material You Dominant Color Extraction dari Cover Art
    LaunchedEffect(state.currentMediaId) {
        val tId = state.currentMediaId?.toLongOrNull()
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

    LaunchedEffect(state.currentMediaId, state.isPlaying) {
        while (true) {
            if (!seeking) position = playback.positionMs()
            delay(500)
        }
    }

    val trackId = state.currentMediaId?.toLongOrNull()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedAmbientColor.copy(alpha = 0.45f),
                        animatedAmbientColor.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Bar: Back, Sleep Timer Badge (jika aktif), Queue
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }

                // Sleep Timer Live Countdown Badge
                if (state.sleepTimerSecondsLeft > 0) {
                    val min = state.sleepTimerSecondsLeft / 60
                    val sec = state.sleepTimerSecondsLeft % 60
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp,
                        modifier = Modifier.clickable { showSleepTimer = true }
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
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                IconButton(onClick = onOpenQueue) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Queue")
                }
            }

            Spacer(Modifier.height(4.dp))

            // Large Album Artwork Carousel (Dukungan Animasi Swipe Geser Kiri / Kanan)
            if (queue.isNotEmpty()) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    pageSpacing = 16.dp,
                ) { page ->
                    val item = queue.getOrNull(page)
                    ArtworkImage(
                        trackId = item?.trackId,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
            } else {
                ArtworkImage(
                    trackId = trackId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }

            Spacer(Modifier.height(10.dp))

            // Title & Artist
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.title.ifEmpty { "Tidak ada lagu" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = state.artist.ifEmpty { "Artis tidak diketahui" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))

            // Row Tools: Shuffle, Sleep Timer, Repeat
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::shuffleAll) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { showSleepTimer = true }) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = "Sleep Timer",
                        tint = if (state.sleepTimerSecondsLeft > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = playback::toggleRepeatMode) {
                    val icon = when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    }
                    val tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(icon, contentDescription = "Repeat", tint = tint)
                }
            }

            // Seekbar
            val currentPos = if (seeking) (seekValue * duration).toLong() else position
            Slider(
                value = (currentPos.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = {
                    seeking = true
                    seekValue = it
                },
                onValueChangeFinished = {
                    playback.seekTo((seekValue * duration).toLong())
                    seeking = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            // Duration labels
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentPos), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(state.durationMs), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(10.dp))

            // Playback Buttons: Prev, Play/Pause, Next
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { playback.previous(forcePreviousItem = true) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(32.dp))
                }

                Spacer(Modifier.size(24.dp))

                // Main Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = playback::togglePlayPause,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(Modifier.size(24.dp))

                IconButton(
                    onClick = playback::next,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            // Audio Specs Badge ditaruh di tengah paling bawah (khas Audiophile)
            state.audioSpecs?.let { specs ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
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

            Spacer(Modifier.height(8.dp))
        }
    }
}

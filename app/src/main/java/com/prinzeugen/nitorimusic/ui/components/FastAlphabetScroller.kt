package com.prinzeugen.nitorimusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ALPHABET = listOf(
    "#", "A", "B", "C", "D", "E", "F", "G", "H", "I",
    "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
    "T", "U", "V", "W", "X", "Y", "Z"
)

/**
 * Fast Alphabet Scroller (A-Z Indexer) di tepi kanan layar ala Poweramp.
 * Mendukung drag vertikal dan tap untuk langsung melompat ke huruf target
 * dengan visual bubble floating indicator.
 */
@Composable
fun <T> FastAlphabetScroller(
    items: List<T>,
    itemTitle: (T) -> String,
    onScrollToIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.size < 10) return

    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var barHeight by remember { mutableStateOf(1) }
    var hideJob by remember { mutableStateOf<Job?>(null) }

    // Precalculate letter to first index lookup
    val letterIndexMap = remember(items) {
        val map = mutableMapOf<String, Int>()
        items.forEachIndexed { index, item ->
            val title = itemTitle(item).trim()
            val firstChar = title.firstOrNull()?.uppercaseChar() ?: '#'
            val letter = if (firstChar in 'A'..'Z') firstChar.toString() else "#"
            if (!map.containsKey(letter)) {
                map[letter] = index
            }
        }
        map
    }

    fun handleTouch(y: Float) {
        if (barHeight <= 0) return
        val clampedY = y.coerceIn(0f, barHeight.toFloat() - 1f)
        val letterIndex = ((clampedY / barHeight) * ALPHABET.size).toInt().coerceIn(0, ALPHABET.lastIndex)
        val letter = ALPHABET[letterIndex]
        activeLetter = letter
        isDragging = true

        hideJob?.cancel()

        // Cari index terdekat jika huruf exact tidak ada
        val targetIdx = letterIndexMap[letter] ?: run {
            // Cari huruf terdekat sesudahnya, jika tidak ada mundur ke sebelumnya
            val nextLetters = ALPHABET.drop(letterIndex)
            val foundNext = nextLetters.firstNotNullOfOrNull { letterIndexMap[it] }
            foundNext ?: letterIndexMap.values.lastOrNull() ?: 0
        }

        onScrollToIndex(targetIdx)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp, horizontal = 2.dp)
            .zIndex(100f),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating Letter Preview Bubble di samping kiri bar scroller
        AnimatedVisibility(
            visible = isDragging && activeLetter != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.padding(end = 40.dp)
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activeLetter.orEmpty(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Vertical Alphabet Touch Strip
        Column(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f))
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(vertical = 4.dp)
                .onGloballyPositioned { coordinates ->
                    barHeight = coordinates.size.height
                }
                .pointerInput(letterIndexMap) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            handleTouch(offset.y)
                        },
                        onDragEnd = {
                            hideJob = scope.launch {
                                delay(600)
                                isDragging = false
                                activeLetter = null
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            activeLetter = null
                        }
                    ) { change, _ ->
                        handleTouch(change.position.y)
                    }
                }
                .pointerInput(letterIndexMap) {
                    detectTapGestures(
                        onPress = { offset ->
                            handleTouch(offset.y)
                            tryAwaitRelease()
                            hideJob = scope.launch {
                                delay(600)
                                isDragging = false
                                activeLetter = null
                            }
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ALPHABET.forEach { char ->
                val isSelected = activeLetter == char
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        fontSize = 8.5.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}


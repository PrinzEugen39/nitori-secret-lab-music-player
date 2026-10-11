package com.prinzeugen.nitorimusic.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prinzeugen.nitorimusic.R
import com.prinzeugen.nitorimusic.domain.model.PlaybackRepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatModeDialog(
    currentMode: PlaybackRepeatMode,
    onSelectMode: (PlaybackRepeatMode) -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(vertical = 12.dp)) {
                // 1. Play Single Song
                RepeatModeItem(
                    customIcon = { tint ->
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                ),
                                color = tint
                            )
                        }
                    },
                    title = stringResource(R.string.play_single_song),
                    subtitle = stringResource(R.string.play_single_song_desc),
                    isSelected = currentMode == PlaybackRepeatMode.PLAY_SINGLE_SONG,
                    onClick = {
                        onSelectMode(PlaybackRepeatMode.PLAY_SINGLE_SONG)
                        onDismiss()
                    }
                )

                // 2. Play Category
                RepeatModeItem(
                    icon = Icons.Rounded.Repeat,
                    title = stringResource(R.string.play_category),
                    subtitle = stringResource(R.string.play_category_desc),
                    isSelected = currentMode == PlaybackRepeatMode.PLAY_CATEGORY,
                    onClick = {
                        onSelectMode(PlaybackRepeatMode.PLAY_CATEGORY)
                        onDismiss()
                    }
                )

                // 3. Play All Categories
                RepeatModeItem(
                    icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    title = stringResource(R.string.play_all_categories),
                    subtitle = stringResource(R.string.play_all_categories_desc),
                    isSelected = currentMode == PlaybackRepeatMode.PLAY_ALL_CATEGORIES,
                    onClick = {
                        onSelectMode(PlaybackRepeatMode.PLAY_ALL_CATEGORIES)
                        onDismiss()
                    }
                )

                // 4. Repeat Song
                RepeatModeItem(
                    icon = Icons.Rounded.RepeatOne,
                    title = stringResource(R.string.repeat_song),
                    subtitle = stringResource(R.string.repeat_song_desc),
                    isSelected = currentMode == PlaybackRepeatMode.REPEAT_SONG,
                    onClick = {
                        onSelectMode(PlaybackRepeatMode.REPEAT_SONG)
                        onDismiss()
                    }
                )

                // 5. Repeat Category
                RepeatModeItem(
                    icon = Icons.Rounded.Repeat,
                    title = stringResource(R.string.repeat_category),
                    subtitle = stringResource(R.string.repeat_category_desc),
                    isSelected = currentMode == PlaybackRepeatMode.REPEAT_CATEGORY,
                    onClick = {
                        onSelectMode(PlaybackRepeatMode.REPEAT_CATEGORY)
                        onDismiss()
                    }
                )

                // 6. Repeat All Categories
                RepeatModeItem(
                    icon = Icons.Rounded.Repeat,
                    title = stringResource(R.string.repeat_all_categories),
                    subtitle = stringResource(R.string.repeat_all_categories_desc),
                    isSelected = currentMode == PlaybackRepeatMode.REPEAT_ALL_CATEGORIES,
                    onClick = {
                        onSelectMode(PlaybackRepeatMode.REPEAT_ALL_CATEGORIES)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun RepeatModeItem(
    icon: ImageVector? = null,
    customIcon: (@Composable (androidx.compose.ui.graphics.Color) -> Unit)? = null,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (customIcon != null) {
            customIcon(tint)
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}


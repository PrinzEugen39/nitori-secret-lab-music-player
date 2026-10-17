package com.prinzeugen.nitorimusic.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.prinzeugen.nitorimusic.domain.model.ShuffleMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShuffleModeDialog(
    currentMode: ShuffleMode,
    onSelectMode: (ShuffleMode) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp)
            ) {
                // Item 1: Shuffle Off
                ShuffleModeItem(
                    icon = Icons.Rounded.Shuffle,
                    title = stringResource(R.string.shuffle_off),
                    subtitle = null,
                    isSelected = currentMode == ShuffleMode.OFF,
                    onClick = {
                        onSelectMode(ShuffleMode.OFF)
                        onDismiss()
                    }
                )

                // Item 2: Shuffle All
                ShuffleModeItem(
                    icon = Icons.Rounded.Shuffle,
                    title = stringResource(R.string.shuffle_mode_all),
                    subtitle = stringResource(R.string.shuffle_all_desc),
                    isSelected = currentMode == ShuffleMode.ALL,
                    onClick = {
                        onSelectMode(ShuffleMode.ALL)
                        onDismiss()
                    }
                )

                // Item 3: Shuffle Songs
                ShuffleModeItem(
                    icon = Icons.Rounded.Shuffle,
                    title = stringResource(R.string.shuffle_songs),
                    subtitle = stringResource(R.string.shuffle_songs_desc),
                    isSelected = currentMode == ShuffleMode.SONGS,
                    onClick = {
                        onSelectMode(ShuffleMode.SONGS)
                        onDismiss()
                    }
                )

                // Item 4: Shuffle Categories
                ShuffleModeItem(
                    icon = Icons.Rounded.Shuffle,
                    title = stringResource(R.string.shuffle_categories),
                    subtitle = stringResource(R.string.shuffle_categories_desc),
                    isSelected = currentMode == ShuffleMode.CATEGORIES,
                    onClick = {
                        onSelectMode(ShuffleMode.CATEGORIES)
                        onDismiss()
                    }
                )

                // Item 5: Shuffle Songs/Categories
                ShuffleModeItem(
                    icon = Icons.Rounded.Shuffle,
                    title = stringResource(R.string.shuffle_songs_categories),
                    subtitle = null,
                    isSelected = currentMode == ShuffleMode.SONGS_AND_CATEGORIES,
                    onClick = {
                        onSelectMode(ShuffleMode.SONGS_AND_CATEGORIES)
                        onDismiss()
                    }
                )

                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(4.dp))

                // Settings Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onOpenSettings()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(20.dp))
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ShuffleModeItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
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
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

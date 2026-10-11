package com.prinzeugen.nitorimusic.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.prinzeugen.nitorimusic.R
import kotlin.math.roundToInt

@Composable
fun ShuffleOptionsDialog(
    spread: Float,
    onSpreadChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableFloatStateOf(spread) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shuffle_options_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.shuffle_spread_label, (value * 100).roundToInt()),
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    onValueChangeFinished = { onSpreadChange(value) },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.shuffle_pure_random), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.shuffle_even_spread), style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    stringResource(R.string.shuffle_spread_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

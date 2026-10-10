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
import kotlin.math.roundToInt

/** Slider randomness: kiri = acak murni, kanan = lagu satu artis disebar merata. */
@Composable
fun ShuffleOptionsDialog(
    spread: Float,
    onSpreadChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableFloatStateOf(spread) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Opsi shuffle") },
        text = {
            Column {
                Text(
                    "Sebar artis: ${(value * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    onValueChangeFinished = { onSpreadChange(value) },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Acak murni", style = MaterialTheme.typography.bodySmall)
                    Text("Sebar merata", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Semakin kanan, lagu dari artis yang sama makin jarang berdekatan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}


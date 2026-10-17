package com.prinzeugen.nitorimusic.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prinzeugen.nitorimusic.R

@Composable
fun SleepTimerDialog(
    currentMinutesLeft: Int,
    currentSecondsLeft: Int = 0,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(15, 30, 45, 60, 90)

    val titleText = if (currentSecondsLeft > 0) {
        val min = currentSecondsLeft / 60
        val sec = currentSecondsLeft % 60
        stringResource(R.string.sleep_timer_title_remaining, "%02d:%02d".format(min, sec))
    } else if (currentMinutesLeft > 0) {
        stringResource(R.string.sleep_timer_title_min_remaining, currentMinutesLeft)
    } else {
        stringResource(R.string.sleep_timer_title_set)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { minutes ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSetTimer(minutes)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.sleep_timer_minutes, minutes), style = MaterialTheme.typography.bodyLarge)
                    }
                }

                if (currentMinutesLeft > 0 || currentSecondsLeft > 0) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCancelTimer()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.sleep_timer_turn_off),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

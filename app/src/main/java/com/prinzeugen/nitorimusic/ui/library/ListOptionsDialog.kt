package com.prinzeugen.nitorimusic.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prinzeugen.nitorimusic.domain.model.ListOptions
import com.prinzeugen.nitorimusic.domain.model.SortType
import com.prinzeugen.nitorimusic.domain.model.ViewLayout

@Composable
fun ListOptionsDialog(
    title: String,
    currentOptions: ListOptions,
    onOptionsChange: (ListOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var sortType by remember { mutableStateOf(currentOptions.sortType) }
    var reverse by remember { mutableStateOf(currentOptions.reverse) }
    var layout by remember { mutableStateOf(currentOptions.layout) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("List Options: $title", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Sort", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))

                val availableSorts = listOf(
                    SortType.BY_NAME,
                    SortType.BY_ARTIST,
                    SortType.BY_YEAR,
                    SortType.BY_DATE_ADDED,
                    SortType.BY_TRACK_COUNT,
                )

                availableSorts.forEach { type ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { sortType = type }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = sortType == type,
                            onClick = { sortType = type }
                        )
                        Text(
                            type.label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { reverse = !reverse }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = reverse,
                        onCheckedChange = { reverse = it }
                    )
                    Text(
                        "Reverse",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                Text("Layout", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { layout = ViewLayout.GRID }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = layout == ViewLayout.GRID,
                        onClick = { layout = ViewLayout.GRID }
                    )
                    Text(
                        "Grid",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { layout = ViewLayout.LIST }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = layout == ViewLayout.LIST,
                        onClick = { layout = ViewLayout.LIST }
                    )
                    Text(
                        "List",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOptionsChange(ListOptions(sortType = sortType, reverse = reverse, layout = layout))
                    onDismiss()
                }
            ) {
                Text("Close")
            }
        }
    )
}

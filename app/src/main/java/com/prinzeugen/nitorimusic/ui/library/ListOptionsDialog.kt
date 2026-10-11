package com.prinzeugen.nitorimusic.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prinzeugen.nitorimusic.R
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
    var gridColumns by remember { mutableIntStateOf(currentOptions.gridColumns) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.list_options_dialog_title, title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.list_options_sort_section), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                            stringResource(type.labelRes),
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
                        stringResource(R.string.list_options_reverse),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                Text(stringResource(R.string.list_options_layout_section), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                        stringResource(R.string.list_options_layout_grid),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (layout == ViewLayout.GRID) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
                    ) {
                        Text(
                            stringResource(R.string.list_options_columns, gridColumns),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (2..5).forEach { cols ->
                                FilterChip(
                                    selected = gridColumns == cols,
                                    onClick = { gridColumns = cols },
                                    label = { Text("$cols") }
                                )
                            }
                        }
                    }
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
                        stringResource(R.string.list_options_layout_list),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOptionsChange(
                        ListOptions(
                            sortType = sortType,
                            reverse = reverse,
                            layout = layout,
                            gridColumns = gridColumns,
                        )
                    )
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}


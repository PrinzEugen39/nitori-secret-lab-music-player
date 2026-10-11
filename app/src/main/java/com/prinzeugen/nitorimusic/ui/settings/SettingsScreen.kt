package com.prinzeugen.nitorimusic.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prinzeugen.nitorimusic.R
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.library.FolderSelectionDialog

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val tracks by viewModel.tracks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val audioFocus by viewModel.audioFocusEnabled.collectAsState()
    val pauseOnHeadset by viewModel.pauseOnHeadsetDisconnect.collectAsState()
    val gapless by viewModel.gaplessEnabled.collectAsState()
    val replayGain by viewModel.replayGainEnabled.collectAsState()
    val spread by viewModel.shuffleSpread.collectAsState()
    val albumArtFitMode by viewModel.albumArtFitMode.collectAsState()
    val keepQueueOnPlay by viewModel.keepQueueOnPlay.collectAsState()
    val countThreshold by viewModel.countAsPlayedThreshold.collectAsState()
    val selectedMusicFolders by viewModel.selectedMusicFolders.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    var showFolderSelectionDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLanguage = appLanguage,
            onSelectLanguage = { lang ->
                viewModel.setAppLanguage(lang)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // --- Section 1: Language / Bahasa ---
            SectionHeader(icon = Icons.Rounded.Language, title = stringResource(R.string.settings_section_language))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { showLanguageDialog = true }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_language_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val languageLabel = when (appLanguage) {
                            "en" -> stringResource(R.string.settings_language_en)
                            "in" -> stringResource(R.string.settings_language_id)
                            else -> stringResource(R.string.settings_language_system)
                        }
                        Text(
                            languageLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        Icons.Rounded.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Section 2: Audio Engine ---
            SectionHeader(icon = Icons.Rounded.GraphicEq, title = stringResource(R.string.settings_section_audio))

            SettingSwitchRow(
                title = stringResource(R.string.settings_gapless_title),
                subtitle = stringResource(R.string.settings_gapless_desc),
                checked = gapless,
                onCheckedChange = viewModel::setGaplessEnabled
            )

            SettingSwitchRow(
                title = stringResource(R.string.settings_replaygain_title),
                subtitle = stringResource(R.string.settings_replaygain_desc),
                checked = replayGain,
                onCheckedChange = viewModel::setReplayGainEnabled
            )

            SettingSwitchRow(
                title = stringResource(R.string.settings_pause_headset_title),
                subtitle = stringResource(R.string.settings_pause_headset_desc),
                checked = pauseOnHeadset,
                onCheckedChange = viewModel::setPauseOnHeadsetDisconnect
            )

            SettingSwitchRow(
                title = stringResource(R.string.settings_audio_focus_title),
                subtitle = stringResource(R.string.settings_audio_focus_desc),
                checked = audioFocus,
                onCheckedChange = viewModel::setAudioFocusEnabled
            )

            Spacer(Modifier.height(16.dp))

            // --- Section 3: Library & Room Cache ---
            SectionHeader(icon = Icons.Rounded.Storage, title = stringResource(R.string.settings_section_library))

            // Folders Selection Card (Poweramp-style)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { showFolderSelectionDialog = true }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_folders_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val folderSubtitle = if (selectedMusicFolders.isEmpty()) {
                            stringResource(R.string.settings_folders_default)
                        } else {
                            val enabledCount = selectedMusicFolders.count { it.isEnabled }
                            stringResource(
                                R.string.settings_folders_active,
                                enabledCount,
                                selectedMusicFolders.joinToString(", ") { it.displayName }
                            )
                        }
                        Text(
                            folderSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.settings_db_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.settings_db_tracks_cached, tracks.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::rescanLibrary,
                            enabled = !isScanning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_btn_rescan))
                        }

                        OutlinedButton(
                            onClick = viewModel::clearCacheAndRescan,
                            enabled = !isScanning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.settings_btn_reset_cache))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Section 4: Smart Shuffle ---
            SectionHeader(icon = Icons.Rounded.Shuffle, title = stringResource(R.string.settings_section_shuffle))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.settings_shuffle_spread_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${(spread * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        if (spread < 0.1f) stringResource(R.string.settings_shuffle_spread_pure)
                        else stringResource(R.string.settings_shuffle_spread_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Slider(
                        value = spread,
                        onValueChange = viewModel::setShuffleSpread,
                        valueRange = 0f..1f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Section 5: Display & Album Art ---
            SectionHeader(icon = Icons.Rounded.Image, title = stringResource(R.string.settings_section_appearance))

            SettingSwitchRow(
                title = stringResource(R.string.settings_art_fit_title),
                subtitle = stringResource(R.string.settings_art_fit_desc),
                checked = albumArtFitMode,
                onCheckedChange = viewModel::setAlbumArtFitMode
            )

            Spacer(Modifier.height(16.dp))

            // --- Section 6: Queue & Playback ---
            SectionHeader(icon = Icons.AutoMirrored.Rounded.QueueMusic, title = stringResource(R.string.settings_section_queue))

            SettingSwitchRow(
                title = stringResource(R.string.settings_keep_queue_title),
                subtitle = stringResource(R.string.settings_keep_queue_desc),
                checked = keepQueueOnPlay,
                onCheckedChange = viewModel::setKeepQueueOnPlay
            )

            Spacer(Modifier.height(16.dp))

            // --- Section 7: Statistics (Most Played) ---
            SectionHeader(icon = Icons.Rounded.Whatshot, title = stringResource(R.string.settings_section_stats))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.settings_threshold_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "$countThreshold%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        stringResource(R.string.settings_threshold_desc, countThreshold),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Slider(
                        value = countThreshold.toFloat(),
                        onValueChange = { viewModel.setCountAsPlayedThreshold(it.toInt()) },
                        valueRange = 10f..100f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Section 8: About ---
            SectionHeader(icon = Icons.Rounded.Info, title = stringResource(R.string.settings_section_about))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.settings_about_app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.settings_about_version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.settings_about_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showFolderSelectionDialog) {
        FolderSelectionDialog(
            initialFolders = selectedMusicFolders,
            onSaveAndScan = viewModel::saveMusicFolders,
            onDismiss = { showFolderSelectionDialog = false }
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        "system" to stringResource(R.string.settings_language_system),
        "en" to stringResource(R.string.settings_language_en),
        "in" to stringResource(R.string.settings_language_id),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.settings_language_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                options.forEach { (code, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLanguage(code) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == code,
                            onClick = { onSelectLanguage(code) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (currentLanguage == code) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

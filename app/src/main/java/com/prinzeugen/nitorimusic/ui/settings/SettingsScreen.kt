package com.prinzeugen.nitorimusic.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    var showFolderSelectionDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

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
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Pengaturan",
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
            // --- Section 1: Audio Engine ---
            SectionHeader(icon = Icons.Rounded.GraphicEq, title = "Audio Engine")

            SettingSwitchRow(
                title = "Gapless Playback",
                subtitle = "Transisi antar lagu tanpa celah hening (seamless playback)",
                checked = gapless,
                onCheckedChange = viewModel::setGaplessEnabled
            )

            SettingSwitchRow(
                title = "Normalisasi Volume (ReplayGain)",
                subtitle = "Menyelaraskan kenyaringan audio secara otomatis (ReplayGain / R128)",
                checked = replayGain,
                onCheckedChange = viewModel::setReplayGainEnabled
            )

            SettingSwitchRow(
                title = "Jeda saat Headset Lepas",
                subtitle = "Otomatis pause musik saat earphone kabel atau Bluetooth terputus",
                checked = pauseOnHeadset,
                onCheckedChange = viewModel::setPauseOnHeadsetDisconnect
            )

            SettingSwitchRow(
                title = "Audio Focus Handling",
                subtitle = "Otomatis kecilkan volume atau pause saat menerima telepon atau notifikasi",
                checked = audioFocus,
                onCheckedChange = viewModel::setAudioFocusEnabled
            )

            Spacer(Modifier.height(16.dp))

            // --- Section 2: Library & Room Cache ---
            SectionHeader(icon = Icons.Rounded.Storage, title = "Library & Database Cache")

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
                            "Folders Selection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val folderSubtitle = if (selectedMusicFolders.isEmpty()) {
                            "Default (seluruh folder musik, abaikan WhatsApp & rekaman)"
                        } else {
                            val enabledCount = selectedMusicFolders.count { it.isEnabled }
                            "$enabledCount folder aktif (${selectedMusicFolders.joinToString(", ") { it.displayName }})"
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
                                "Database Cache Room",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${tracks.size} lagu tersimpan di cache lokal",
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
                            Text("Pindai Ulang")
                        }

                        OutlinedButton(
                            onClick = viewModel::clearCacheAndRescan,
                            enabled = !isScanning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset Cache")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Section 3: Smart Shuffle ---
            SectionHeader(icon = Icons.Rounded.Shuffle, title = "Smart Shuffle")

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
                            "Sebaran Artis",
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
                        if (spread < 0.1f) "Acak murni (Fisher-Yates)"
                        else "Semakin tinggi, semakin kecil kemungkinan lagu dari artis yang sama muncul berturutan.",
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

            // --- Section 4: Tampilan & Sampul Album (Now Playing UX) ---
            SectionHeader(icon = Icons.Rounded.Image, title = "Tampilan & Sampul Album")

            SettingSwitchRow(
                title = "Pertahankan Rasio Asli Cover Art (Fit)",
                subtitle = "Menampilkan gambar sampul sesuai proporsinya tanpa terpotong (ala Poweramp). Nonaktifkan untuk mengisi penuh kotak (Crop).",
                checked = albumArtFitMode,
                onCheckedChange = viewModel::setAlbumArtFitMode
            )

            Spacer(Modifier.height(16.dp))

            // --- Section 5: Antrean & Pemutaran (Queue Behavior) ---
            SectionHeader(icon = Icons.AutoMirrored.Rounded.QueueMusic, title = "Antrean & Pemutaran")

            SettingSwitchRow(
                title = "Lanjut Mainkan di Antrean yang Sama",
                subtitle = "Memutar lagu dari daftar/album langsung di dalam antrean aktif tanpa mereset atau membuat antrean baru (ala Poweramp).",
                checked = keepQueueOnPlay,
                onCheckedChange = viewModel::setKeepQueueOnPlay
            )

            Spacer(Modifier.height(16.dp))

            // --- Section 6: Statistik Lagu (Most Played) ---
            SectionHeader(icon = Icons.Rounded.Whatshot, title = "Statistik Pemutaran (Most Played)")

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
                            "Batas Hitung Lagu Diputar",
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
                        "Lagu akan dihitung telah diputar (+1) dan masuk ke tab 'Most Played' setelah dimainkan minimal $countThreshold% dari total durasinya.",
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

            // --- Section 7: Tentang ---
            SectionHeader(icon = Icons.Rounded.Info, title = "Tentang")

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
                        "Nitori Secret Lab Music Player",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Versi 1.0 • Offline Hi-Fi Music Player",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Fokus pada reproduksi audio murni (uncolored playback), antrean Up Next yang fleksibel, dan performa cepat tanpa lag.",
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

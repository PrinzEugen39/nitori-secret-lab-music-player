package com.prinzeugen.nitorimusic.ui.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prinzeugen.nitorimusic.domain.model.MusicFolder
import java.io.File

/**
 * Dialog pemilihan direktori musik ("Folders Selection") ala Poweramp & MusicBee.
 * Memungkinkan pengguna menentukan folder spesifik untuk dipindai (whitelist)
 * dan mengecualikan berkas non-musik seperti rekaman atau audio chat WhatsApp/Telegram.
 */
@Composable
fun FolderSelectionDialog(
    initialFolders: List<MusicFolder>,
    onSaveAndScan: (List<MusicFolder>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var folders by remember { mutableStateOf(initialFolders) }

    // Launcher Document Tree Picker untuk memilih folder musik (SAF)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolved = resolveMusicFolder(context, uri)
            // Tambahkan jika belum ada di daftar, atau perbarui
            val existingIndex = folders.indexOfFirst { it.path.equals(resolved.path, ignoreCase = true) }
            if (existingIndex >= 0) {
                folders = folders.toMutableList().also {
                    it[existingIndex] = it[existingIndex].copy(isEnabled = true)
                }
            } else {
                folders = folders + resolved
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Dialog
                Text(
                    text = "Folders Selection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Pilih folder yang memuat file musik Anda. Folder rekaman & chat (WhatsApp/Telegram) otomatis diabaikan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Spacer(Modifier.height(8.dp))

                // List Folder
                if (folders.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "Belum ada folder khusus yang dipilih.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Secara default seluruh penyimpanan dipindai dengan mengecualikan audio chat (WhatsApp, Telegram) dan rekaman. Tekan tombol di bawah untuk membatasi scan hanya ke folder musik Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(folders, key = { it.path }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        folders = folders.map {
                                            if (it.path == item.path) it.copy(isEnabled = !it.isEnabled) else it
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.width(6.dp))

                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.width(10.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.storageLabel.ifEmpty { item.path },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        folders = folders.filterNot { it.path == item.path }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Checkbox(
                                    checked = item.isEnabled,
                                    onCheckedChange = { checked ->
                                        folders = folders.map {
                                            if (it.path == item.path) it.copy(isEnabled = checked) else it
                                        }
                                    }
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tombol Aksi Bawah (Poweramp style)
                FilledTonalButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Select Folder or Storage")
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        onSaveAndScan(folders)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Save and Scan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Resolusi dari URI tree Storage Access Framework (SAF) ke model MusicFolder
 * dengan nama folder dan label perangkat (e.g. "Infinix X6885").
 */
private fun resolveMusicFolder(context: Context, uri: Uri): MusicFolder {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: Exception) {}

    var displayName = "Music"
    var storageLabel = Build.MODEL.ifEmpty { "Internal Storage" }
    var absolutePath = ""

    val docId = try {
        DocumentsContract.getTreeDocumentId(uri)
    } catch (e: Exception) {
        uri.lastPathSegment ?: ""
    }

    if (docId.contains(":")) {
        val split = docId.split(":", limit = 2)
        val type = split[0]
        val relative = split.getOrNull(1) ?: ""
        if (type.equals("primary", ignoreCase = true)) {
            val root = Environment.getExternalStorageDirectory().absolutePath
            absolutePath = if (relative.isNotEmpty()) "$root/$relative" else root
            displayName = if (relative.isNotEmpty()) relative.substringAfterLast("/").ifEmpty { relative } else "Internal Storage"
            storageLabel = Build.MODEL.ifEmpty { "Internal Storage" }
        } else {
            absolutePath = if (relative.isNotEmpty()) "/storage/$type/$relative" else "/storage/$type"
            displayName = if (relative.isNotEmpty()) relative.substringAfterLast("/").ifEmpty { relative } else type
            storageLabel = "SD Card ($type)"
        }
    } else {
        absolutePath = Environment.getExternalStorageDirectory().absolutePath + "/" + docId
        displayName = docId.substringAfterLast("/").ifEmpty { docId }
    }

    return MusicFolder(
        path = absolutePath,
        displayName = displayName,
        storageLabel = storageLabel,
        isEnabled = true
    )
}


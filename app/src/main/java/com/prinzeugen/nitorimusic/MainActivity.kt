package com.prinzeugen.nitorimusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.prinzeugen.nitorimusic.ui.AppRoot
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.theme.NitoriMusicTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var hasRefreshedWithAllFiles = false

    private val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasAllFilesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasAllFilesPermission() && !hasRefreshedWithAllFiles) {
            hasRefreshedWithAllFiles = true
            viewModel.refreshLibrary(forceSync = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NitoriMusicTheme {
                val context = LocalContext.current
                var granted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, audioPermission) ==
                            PackageManager.PERMISSION_GRANTED
                    )
                }
                var showAllFilesDialog by remember {
                    mutableStateOf(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()
                    )
                }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    granted = result[audioPermission] == true
                    if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        showAllFilesDialog = true
                    }
                }

                LaunchedEffect(Unit) {
                    val perms = buildList {
                        add(audioPermission)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    launcher.launch(perms.toTypedArray())
                }

                LaunchedEffect(granted) {
                    if (granted) viewModel.refreshLibrary(forceSync = false)
                }

                if (showAllFilesDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    AlertDialog(
                        onDismissRequest = { showAllFilesDialog = false },
                        title = { Text("Izin Akses File untuk CUE Sheet") },
                        text = {
                            Text(
                                "Agar aplikasi dapat mendeteksi file .cue (CUE Sheet) dan membagi album menjadi lagu terpisah (seperti di Poweramp), izin Akses Semua File diperlukan.\n\nBuka Pengaturan untuk mengaktifkan izin?"
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                showAllFilesDialog = false
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            }) {
                                Text("Buka Pengaturan")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAllFilesDialog = false }) {
                                Text("Nanti Saja")
                            }
                        }
                    )
                }

                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot(viewModel, hasPermission = granted)
                }
            }
        }
    }
}

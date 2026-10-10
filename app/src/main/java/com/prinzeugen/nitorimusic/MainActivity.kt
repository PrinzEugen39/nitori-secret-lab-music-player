package com.prinzeugen.nitorimusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.library.LibraryScreen
import com.prinzeugen.nitorimusic.ui.theme.NitoriMusicTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NitoriMusicTheme {
                var granted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, audioPermission) ==
                            PackageManager.PERMISSION_GRANTED
                    )
                }
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result -> granted = result[audioPermission] == true }

                LaunchedEffect(Unit) {
                    val perms = buildList {
                        add(audioPermission)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    launcher.launch(perms.toTypedArray())
                }
                LaunchedEffect(granted) { if (granted) viewModel.refreshLibrary() }

                Surface(color = MaterialTheme.colorScheme.background) {
                    LibraryScreen(viewModel, hasPermission = granted)
                }
            }
        }
    }
}

package com.prinzeugen.nitorimusic

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.content.res.Configuration
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.LocaleList
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.prinzeugen.nitorimusic.ui.AppRoot
import com.prinzeugen.nitorimusic.ui.MainViewModel
import com.prinzeugen.nitorimusic.ui.theme.NitoriMusicTheme
import java.util.Locale
import androidx.activity.ComponentActivity

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
            viewModel.ensureLibraryLoaded()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appLanguage by viewModel.appLanguage.collectAsState()
            val baseConfig = LocalConfiguration.current

            val targetLocale = remember(appLanguage) {
                when (appLanguage) {
                    "en" -> Locale.ENGLISH
                    "in", "id" -> Locale("in", "ID")
                    else -> null
                }
            }

            LaunchedEffect(targetLocale) {
                val target = targetLocale ?: Locale.getDefault()
                Locale.setDefault(target)
                val config = Configuration(resources.configuration)
                config.setLocale(target)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val localeList = LocaleList(target)
                    LocaleList.setDefault(localeList)
                    config.setLocales(localeList)
                }
                @Suppress("DEPRECATION")
                resources.updateConfiguration(config, resources.displayMetrics)
                @Suppress("DEPRECATION")
                applicationContext.resources.updateConfiguration(config, applicationContext.resources.displayMetrics)
            }

            val localizedConfiguration = remember(targetLocale, baseConfig) {
                Configuration(baseConfig).apply {
                    if (targetLocale != null) {
                        setLocale(targetLocale)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            setLocales(LocaleList(targetLocale))
                        }
                    }
                }
            }

            val localizedContext = remember(targetLocale) {
                if (targetLocale != null) {
                    LocalizedActivityContext(this@MainActivity, localizedConfiguration)
                } else {
                    this@MainActivity
                }
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfiguration,
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                NitoriMusicTheme {
                    val context = LocalContext.current
                    var granted by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(this@MainActivity, audioPermission) ==
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
                        if (granted) viewModel.ensureLibraryLoaded()
                    }

                    if (showAllFilesDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        AlertDialog(
                            onDismissRequest = { showAllFilesDialog = false },
                            title = { Text(stringResource(R.string.cue_permission_dialog_title)) },
                            text = {
                                Text(stringResource(R.string.cue_permission_dialog_msg))
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
                                    Text(stringResource(R.string.cue_permission_open_settings))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAllFilesDialog = false }) {
                                    Text(stringResource(R.string.cue_permission_later))
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
}

private class LocalizedActivityContext(
    private val activity: ComponentActivity,
    private val localizedConfig: Configuration
) : ContextWrapper(activity), ActivityResultRegistryOwner {

    private val localizedRes: Resources by lazy {
        activity.createConfigurationContext(localizedConfig).resources
    }

    override fun getResources(): Resources = localizedRes

    override val activityResultRegistry: ActivityResultRegistry
        get() = activity.activityResultRegistry
}

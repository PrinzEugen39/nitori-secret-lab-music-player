package com.prinzeugen.nitorimusic.ui.components

import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Cover art dari tag/embedded art lewat ContentResolver.loadThumbnail (Android 10+). */
@Composable
fun ArtworkImage(trackId: Long?, modifier: Modifier = Modifier, sizePx: Int = 600) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, trackId) {
        value = if (trackId == null || Build.VERSION.SDK_INT < 29) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId
                    )
                    context.contentResolver
                        .loadThumbnail(uri, Size(sizePx, sizePx), null)
                        .asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } ?: Text("♪", style = MaterialTheme.typography.headlineMedium)
    }
}


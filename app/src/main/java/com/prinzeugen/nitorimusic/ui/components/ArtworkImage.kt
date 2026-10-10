package com.prinzeugen.nitorimusic.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.prinzeugen.nitorimusic.data.ArtworkLoader
import com.prinzeugen.nitorimusic.data.CustomArtworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cover art renderer dengan multi-level fallback:
 * 1. Custom Artwork (yang diunggah/dipilih pengguna untuk album ini)
 * 2. MediaStore loadThumbnail (Android 10+)
 * 3. MediaMetadataRetriever embedded picture (untuk FLAC, MP3, ALAC yang gagal di-thumbnail MediaStore)
 * 4. Legacy MediaStore Audio Albumart Uri
 * 5. Folder cover.jpg / folder.jpg
 */
@Composable
fun ArtworkImage(
    trackId: Long? = null,
    albumId: Long? = null,
    modifier: Modifier = Modifier,
    sizePx: Int = 600,
) {
    val context = LocalContext.current
    val revision by CustomArtworkManager.revision.collectAsState()

    val bitmap by produceState<ImageBitmap?>(null, trackId, albumId, revision) {
        value = if (trackId == null && albumId == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                ArtworkLoader.loadArtwork(context, trackId, albumId, sizePx)?.asImageBitmap()
            }
        }
    }

    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Text("♪", style = MaterialTheme.typography.headlineMedium)
    }
}

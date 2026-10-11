package com.prinzeugen.nitorimusic.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import java.io.File

object ArtworkLoader {

    fun loadArtwork(
        context: Context,
        trackId: Long?,
        albumId: Long?,
        sizePx: Int
    ): Bitmap? {
        if (trackId == null && albumId == null) return null

        val cacheKey = when {
            albumId != null -> "a_${albumId}_$sizePx"
            trackId != null -> "t_${trackId}_$sizePx"
            else -> return null
        }

        ArtworkMemoryCache.get(cacheKey)?.let { return it }

        // 1. Cek Custom Artwork jika albumId diketahui
        if (albumId != null && CustomArtworkManager.hasCustomArtwork(context, albumId)) {
            val file = CustomArtworkManager.getArtworkFile(context, albumId)
            val bitmap = decodeSampledBitmapFromFile(file.absolutePath, sizePx, sizePx)
            if (bitmap != null) {
                ArtworkMemoryCache.put(cacheKey, bitmap)
                return bitmap
            }
        }

        var resolvedAlbumId = albumId
        var folderPath: String? = null

        // 2. Query MediaStore track info jika trackId diketahui untuk mencari albumId & folder jika belum ada
        if (trackId != null) {
            val actualTrackId = if (trackId < 0) (-trackId) / 1000L else trackId
            val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, actualTrackId)
            try {
                context.contentResolver.query(
                    trackUri,
                    arrayOf(MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val albumIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                        val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                        if (albumIdIdx >= 0 && resolvedAlbumId == null) {
                            resolvedAlbumId = cursor.getLong(albumIdIdx)
                        }
                        if (dataIdx >= 0) {
                            val path = cursor.getString(dataIdx)
                            if (!path.isNullOrEmpty()) {
                                folderPath = File(path).parent
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            // Cek custom artwork lagi jika resolvedAlbumId baru ditemukan dari track
            if (resolvedAlbumId != null && CustomArtworkManager.hasCustomArtwork(context, resolvedAlbumId!!)) {
                val file = CustomArtworkManager.getArtworkFile(context, resolvedAlbumId!!)
                val bitmap = decodeSampledBitmapFromFile(file.absolutePath, sizePx, sizePx)
                if (bitmap != null) {
                    ArtworkMemoryCache.put(cacheKey, bitmap)
                    return bitmap
                }
            }

            // 3. Coba ContentResolver.loadThumbnail (Android 10+)
            if (Build.VERSION.SDK_INT >= 29) {
                try {
                    val bmp = context.contentResolver.loadThumbnail(trackUri, Size(sizePx, sizePx), null)
                    ArtworkMemoryCache.put(cacheKey, bmp)
                    return bmp
                } catch (_: Exception) {}
            }

            // 4. Coba MediaMetadataRetriever (embedded picture FLAC / MP3 / M4A)
            try {
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(context, trackUri)
                    val picture = mmr.embeddedPicture
                    if (picture != null) {
                        val bmp = decodeSampledBitmapFromByteArray(picture, sizePx, sizePx)
                        if (bmp != null) {
                            ArtworkMemoryCache.put(cacheKey, bmp)
                            return bmp
                        }
                    }
                } finally {
                    try { mmr.release() } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        // 5. Coba Legacy MediaStore Album Art URI
        if (resolvedAlbumId != null) {
            try {
                val albumUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    resolvedAlbumId!!
                )
                context.contentResolver.openInputStream(albumUri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val bmp = decodeSampledBitmapFromByteArray(bytes, sizePx, sizePx)
                    if (bmp != null) {
                        ArtworkMemoryCache.put(cacheKey, bmp)
                        return bmp
                    }
                }
            } catch (_: Exception) {}
        }

        // 6. Coba Folder image (cover.jpg, folder.jpg, front.jpg, dll)
        if (folderPath != null) {
            val folder = File(folderPath!!)
            val candidateNames = listOf(
                "cover.jpg", "cover.png", "cover.jpeg",
                "folder.jpg", "folder.png", "folder.jpeg",
                "front.jpg", "front.png", "album.jpg"
            )
            for (name in candidateNames) {
                val f = File(folder, name)
                if (f.exists() && f.canRead()) {
                    val bmp = decodeSampledBitmapFromFile(f.absolutePath, sizePx, sizePx)
                    if (bmp != null) {
                        ArtworkMemoryCache.put(cacheKey, bmp)
                        return bmp
                    }
                }
            }
        }

        return null
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565
            BitmapFactory.decodeFile(filePath, options)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampledBitmapFromByteArray(data: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        } catch (_: Exception) {
            null
        }
    }
}


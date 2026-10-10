package com.prinzeugen.nitorimusic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

object ArtworkMemoryCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024 * 16) // Minimal 16MB

    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(key: String): Bitmap? = synchronized(cache) { cache.get(key) }
    fun put(key: String, bitmap: Bitmap) { synchronized(cache) { cache.put(key, bitmap) } }
    fun evictAlbum(albumId: Long) {
        synchronized(cache) {
            val keysToRemove = cache.snapshot().keys.filter { it.contains("a_${albumId}") }
            keysToRemove.forEach { cache.remove(it) }
        }
    }
    fun clear() {
        synchronized(cache) { cache.evictAll() }
    }
}

object CustomArtworkManager {
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()

    fun getArtworkFile(context: Context, albumId: Long): File {
        val dir = File(context.filesDir, "custom_album_art").apply { if (!exists()) mkdirs() }
        return File(dir, "${albumId}.jpg")
    }

    fun hasCustomArtwork(context: Context, albumId: Long): Boolean {
        return getArtworkFile(context, albumId).exists()
    }

    fun saveCustomArtwork(context: Context, albumId: Long, imageUri: Uri): Boolean {
        return try {
            val bitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return false

            val maxDim = 1200
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }

            val file = getArtworkFile(context, albumId)
            FileOutputStream(file).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            ArtworkMemoryCache.evictAlbum(albumId)
            _revision.value += 1
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteCustomArtwork(context: Context, albumId: Long): Boolean {
        val file = getArtworkFile(context, albumId)
        val deleted = if (file.exists()) file.delete() else false
        ArtworkMemoryCache.evictAlbum(albumId)
        _revision.value += 1
        return deleted
    }
}


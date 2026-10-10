package com.prinzeugen.nitorimusic.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.prinzeugen.nitorimusic.data.db.AppDatabase
import com.prinzeugen.nitorimusic.data.db.entity.TrackEntity
import com.prinzeugen.nitorimusic.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository data lagu dengan dukungan Room DB caching (offline-first & startup instan).
 */
class LibraryRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val trackDao = db.trackDao()

    /** Mengambil daftar lagu dari cache database Room lokal (sangat cepat, 0-10ms). */
    suspend fun getCachedTracks(): List<Track> = withContext(Dispatchers.IO) {
        trackDao.getAllTracks().map { it.toDomain() }
    }

    /** Menghitung jumlah lagu tersimpan di database cache. */
    suspend fun getCachedTrackCount(): Int = withContext(Dispatchers.IO) {
        trackDao.getTrackCount()
    }

    /**
     * Memindai seluruh lagu dari sistem MediaStore Android,
     * kemudian memperbarui cache database Room.
     */
    suspend fun syncFromMediaStore(): List<Track> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val tracks = mutableListOf<Track>()
        context.contentResolver.query(collection, projection, selection, null, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = c.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val yearCol = c.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val dateAddedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val rawPath = if (dataCol >= 0) c.getString(dataCol) ?: "" else ""
                val folder = if (rawPath.contains("/")) rawPath.substringBeforeLast("/") else ""
                val rawTrackNo = if (trackCol >= 0) c.getInt(trackCol) else 0
                val trackNo = if (rawTrackNo >= 1000) rawTrackNo % 1000 else rawTrackNo

                tracks += Track(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = c.getString(titleCol) ?: "Unknown",
                    artist = c.getString(artistCol) ?: "Unknown artist",
                    album = c.getString(albumCol) ?: "Unknown album",
                    albumId = c.getLong(albumIdCol),
                    durationMs = c.getLong(durCol),
                    trackNo = trackNo,
                    year = if (yearCol >= 0) c.getInt(yearCol) else 0,
                    dateAddedSec = if (dateAddedCol >= 0) c.getLong(dateAddedCol) else 0L,
                    folderPath = folder,
                )
            }
        }

        // Simpan ke cache Room DB
        val entities = tracks.map { TrackEntity.fromDomain(it) }
        trackDao.replaceAll(entities)

        tracks
    }

    /** Menghapus seluruh cache metadata lagu. */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        trackDao.clearAll()
    }

    /**
     * Memuat lagu dengan strategi cache-first:
     * Jika cache ada dan forceSync false, kembalikan cache langsung.
     */
    suspend fun loadTracks(forceSync: Boolean = false): List<Track> = withContext(Dispatchers.IO) {
        if (!forceSync) {
            val cached = getCachedTracks()
            if (cached.isNotEmpty()) {
                return@withContext cached
            }
        }
        syncFromMediaStore()
    }
}

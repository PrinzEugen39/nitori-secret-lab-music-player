package com.prinzeugen.nitorimusic.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.prinzeugen.nitorimusic.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Membaca daftar lagu dari MediaStore. Belum di-cache ke Room. */
class LibraryRepository(private val context: Context) {

    suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
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

        val result = mutableListOf<Track>()
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
                // Track number in MediaStore can sometimes be 1001 (disc 1, track 1)
                val trackNo = if (rawTrackNo >= 1000) rawTrackNo % 1000 else rawTrackNo

                result += Track(
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
        result
    }
}


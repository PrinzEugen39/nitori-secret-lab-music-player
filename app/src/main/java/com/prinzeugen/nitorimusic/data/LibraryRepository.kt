package com.prinzeugen.nitorimusic.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.prinzeugen.nitorimusic.data.cue.CueSheetParser
import com.prinzeugen.nitorimusic.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Membaca daftar lagu dari MediaStore dengan dukungan CUE sheet splitting. */
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

        val rawTracks = mutableListOf<Track>()
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

                rawTracks += Track(
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

        // Cari dan proses CUE sheet untuk file yang di-rip per album / CD Image
        val finalTracks = mutableListOf<Track>()
        val processedAudioPaths = mutableSetOf<String>()

        for (track in rawTracks) {
            val audioFile = if (track.folderPath.isNotEmpty()) {
                // Cari file fisik berdasarkan folderPath
                val folder = File(track.folderPath)
                findCueFileForTrack(folder, track)
            } else null

            if (audioFile != null) {
                val cueSheet = CueSheetParser.parse(audioFile)
                if (cueSheet != null && cueSheet.tracks.isNotEmpty()) {
                    val totalDuration = track.durationMs
                    val cueTracks = cueSheet.tracks
                    for (i in cueTracks.indices) {
                        val currentCue = cueTracks[i]
                        val startMs = currentCue.startMs
                        val endMs = if (i + 1 < cueTracks.size) cueTracks[i + 1].startMs else totalDuration
                        val duration = (endMs - startMs).coerceAtLeast(1000L)

                        finalTracks += Track(
                            id = track.id * 10_000 + currentCue.trackNo,
                            uri = track.uri,
                            title = currentCue.title,
                            artist = currentCue.performer,
                            album = cueSheet.albumTitle.ifEmpty { track.album },
                            albumId = track.albumId,
                            durationMs = duration,
                            trackNo = currentCue.trackNo,
                            year = track.year,
                            dateAddedSec = track.dateAddedSec,
                            folderPath = track.folderPath,
                            startMs = startMs,
                            endMs = endMs,
                            isCueTrack = true,
                            physicalId = track.id,
                        )
                    }
                    continue
                }
            }
            finalTracks += track
        }

        finalTracks
    }

    private fun findCueFileForTrack(folder: File, track: Track): File? {
        if (!folder.exists() || !folder.isDirectory) return null
        val cueFiles = folder.listFiles { _, name -> name.endsWith(".cue", ignoreCase = true) }
        if (!cueFiles.isNullOrEmpty()) {
            return cueFiles.firstOrNull()
        }

        // Fallback: cari file cue yang namanya sama persis dengan audio file
        val cueSameName = File(folder, "${track.title}.cue")
        if (cueSameName.exists()) return cueSameName

        return null
    }
}

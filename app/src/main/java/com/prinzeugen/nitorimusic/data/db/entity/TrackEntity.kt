package com.prinzeugen.nitorimusic.data.db.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.prinzeugen.nitorimusic.domain.model.Track

@Entity(tableName = "cached_tracks")
data class TrackEntity(
    @PrimaryKey
    val id: Long,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNo: Int = 0,
    val year: Int = 0,
    val dateAddedSec: Long = 0L,
    val folderPath: String = "",
    val sizeBytes: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedMs: Long = 0L,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val isCue: Boolean = false,
    val parentTrackId: Long? = null,
) {
    fun toDomain(): Track = Track(
        id = id,
        uri = Uri.parse(uriString),
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = durationMs,
        trackNo = trackNo,
        year = year,
        dateAddedSec = dateAddedSec,
        folderPath = folderPath,
        sizeBytes = sizeBytes,
        playCount = playCount,
        lastPlayedMs = lastPlayedMs,
        startMs = startMs,
        endMs = endMs,
        isCue = isCue,
        parentTrackId = parentTrackId,
    )

    companion object {
        fun fromDomain(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            uriString = track.uri.toString(),
            title = track.title,
            artist = track.artist,
            album = track.album,
            albumId = track.albumId,
            durationMs = track.durationMs,
            trackNo = track.trackNo,
            year = track.year,
            dateAddedSec = track.dateAddedSec,
            folderPath = track.folderPath,
            sizeBytes = track.sizeBytes,
            playCount = track.playCount,
            lastPlayedMs = track.lastPlayedMs,
            startMs = track.startMs,
            endMs = track.endMs,
            isCue = track.isCue,
            parentTrackId = track.parentTrackId,
        )
    }
}

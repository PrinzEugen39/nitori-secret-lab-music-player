package com.prinzeugen.nitorimusic.domain.model

import android.net.Uri

data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNo: Int = 0,
    val year: Int = 0,
    val dateAddedSec: Long = 0L,
    val folderPath: String = "",
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val year: Int,
    val trackCount: Int,
    val coverTrackId: Long,
    val dateAddedSec: Long,
)

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
)

data class Folder(
    val path: String,
    val name: String,
    val trackCount: Int,
)


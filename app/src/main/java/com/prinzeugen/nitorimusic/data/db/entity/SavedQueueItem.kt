package com.prinzeugen.nitorimusic.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_queue")
data class SavedQueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val position: Int,
    val trackId: Long,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long = 0L,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val isCue: Boolean = false,
)

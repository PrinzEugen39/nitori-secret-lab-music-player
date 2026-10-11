package com.prinzeugen.nitorimusic.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.prinzeugen.nitorimusic.data.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM cached_tracks ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllTracks(): List<TrackEntity>

    @Query("SELECT * FROM cached_tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM cached_tracks")
    suspend fun getTrackCount(): Int

    @Query("UPDATE cached_tracks SET playCount = playCount + 1, lastPlayedMs = :timestamp WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: Long, timestamp: Long)

    @Query("SELECT * FROM cached_tracks WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedMs DESC")
    suspend fun getMostPlayedTracks(): List<TrackEntity>

    @Query("SELECT * FROM cached_tracks WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedMs DESC")
    fun observeMostPlayedTracks(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM cached_tracks")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(tracks: List<TrackEntity>) {
        clearAll()
        if (tracks.isNotEmpty()) {
            insertAll(tracks)
        }
    }
}

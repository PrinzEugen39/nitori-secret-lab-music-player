package com.prinzeugen.nitorimusic.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.prinzeugen.nitorimusic.data.db.entity.SavedQueueItem

@Dao
interface QueueDao {

    @Query("SELECT * FROM saved_queue ORDER BY position ASC")
    suspend fun getQueue(): List<SavedQueueItem>

    @Query("DELETE FROM saved_queue")
    suspend fun clearQueue()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SavedQueueItem>)

    @Transaction
    suspend fun replaceQueue(items: List<SavedQueueItem>) {
        clearQueue()
        if (items.isNotEmpty()) {
            insertAll(items)
        }
    }
}


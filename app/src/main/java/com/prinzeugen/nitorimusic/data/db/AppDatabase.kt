package com.prinzeugen.nitorimusic.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.prinzeugen.nitorimusic.data.db.dao.QueueDao
import com.prinzeugen.nitorimusic.data.db.dao.TrackDao
import com.prinzeugen.nitorimusic.data.db.entity.SavedQueueItem
import com.prinzeugen.nitorimusic.data.db.entity.TrackEntity

@Database(
    entities = [
        SavedQueueItem::class,
        TrackEntity::class,
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun queueDao(): QueueDao
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nitori_music.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

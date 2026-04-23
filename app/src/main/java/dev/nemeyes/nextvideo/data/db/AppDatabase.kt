package dev.nemeyes.nextvideo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        VideoEntity::class,
        DownloadEntity::class,
    ],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun videoDao(): VideoDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance
                    ?: Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "nextvideo.db",
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                        .also { instance = it }
            }
    }
}


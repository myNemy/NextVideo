package dev.nemeyes.nextvideo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        VideoEntity::class,
        DownloadEntity::class,
        PlaybackPositionEntity::class,
    ],
    version = 3,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun videoDao(): VideoDao
    abstract fun downloadDao(): DownloadDao
    abstract fun playbackPositionDao(): PlaybackPositionDao

    companion object {
        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `playback_positions` (
                          `accountId` TEXT NOT NULL,
                          `videoId` TEXT NOT NULL,
                          `positionMs` INTEGER NOT NULL,
                          `updatedAtEpochMs` INTEGER NOT NULL,
                          PRIMARY KEY(`accountId`, `videoId`)
                        )
                        """.trimIndent(),
                    )
                }
            }

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance
                    ?:                     Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "nextvideo.db",
                    )
                        .addMigrations(MIGRATION_2_3)
                        .fallbackToDestructiveMigration()
                        .build()
                        .also { instance = it }
            }
    }
}


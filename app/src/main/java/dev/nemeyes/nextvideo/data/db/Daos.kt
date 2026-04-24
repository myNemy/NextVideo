package dev.nemeyes.nextvideo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY lastUsedAtEpochMs DESC, createdAtEpochMs DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE accountId = :accountId AND isDirectory = 0 ORDER BY displayName COLLATE NOCASE")
    fun observeVideos(accountId: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    suspend fun getById(videoId: String): VideoEntity?

    @Query(
        """
        SELECT * FROM videos
        WHERE accountId = :accountId AND isDirectory = 0
        AND displayName LIKE '%' || :query || '%'
        ORDER BY displayName COLLATE NOCASE
        """,
    )
    fun observeSearch(accountId: String, query: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VideoEntity>)

    @Query("DELETE FROM videos WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}

@Dao
interface PlaybackPositionDao {
    @Query("SELECT * FROM playback_positions WHERE accountId = :accountId AND videoId = :videoId LIMIT 1")
    suspend fun get(accountId: String, videoId: String): PlaybackPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaybackPositionEntity)

    @Query("DELETE FROM playback_positions WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE accountId = :accountId ORDER BY updatedAtEpochMs DESC")
    fun observeByAccount(accountId: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE accountId = :accountId")
    suspend fun getAllByAccount(accountId: String): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE accountId = :accountId AND videoId = :videoId LIMIT 1")
    fun observeByVideo(accountId: String, videoId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadEntity?

    @Query("DELETE FROM downloads WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)
}


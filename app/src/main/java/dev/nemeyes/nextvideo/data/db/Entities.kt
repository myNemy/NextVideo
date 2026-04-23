package dev.nemeyes.nextvideo.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["serverBaseUrl", "loginName"], unique = true),
    ],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val serverBaseUrl: String,
    val loginName: String,
    /** WebDAV folder href used as library root (server-absolute). */
    val libraryFolderHref: String,
    val createdAtEpochMs: Long,
    val lastUsedAtEpochMs: Long?,
)

@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["accountId", "href"], unique = true),
        Index(value = ["accountId", "displayName"]),
    ],
)
data class VideoEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    /** WebDAV href (server-absolute). */
    val href: String,
    val displayName: String,
    val contentType: String?,
    val contentLength: Long?,
    val eTag: String?,
    val lastModified: String?,
    val isDirectory: Boolean,
    val updatedAtEpochMs: Long,
)

enum class DownloadStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED,
}

@Entity(
    tableName = "downloads",
    indices = [
        Index(value = ["accountId", "videoId"], unique = true),
    ],
)
data class DownloadEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val videoId: String,
    val status: DownloadStatus,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val localPath: String?,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)


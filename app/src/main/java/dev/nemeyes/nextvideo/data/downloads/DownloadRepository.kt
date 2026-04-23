package dev.nemeyes.nextvideo.data.downloads

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.nemeyes.nextvideo.data.db.AppDatabase
import dev.nemeyes.nextvideo.data.db.DownloadEntity
import dev.nemeyes.nextvideo.data.db.DownloadStatus
import java.util.UUID

class DownloadRepository(
    private val context: Context,
    private val db: AppDatabase,
) {
    fun observeByVideo(accountId: String, videoId: String) = db.downloadDao().observeByVideo(accountId, videoId)

    suspend fun enqueueDownload(accountId: String, videoId: String): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        db.downloadDao().upsert(
            DownloadEntity(
                id = id,
                accountId = accountId,
                videoId = videoId,
                status = DownloadStatus.QUEUED,
                bytesDownloaded = 0,
                totalBytes = null,
                localPath = null,
                errorMessage = null,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )

        val input =
            Data.Builder()
                .putString(DownloadWorker.KEY_DOWNLOAD_ID, id)
                .build()

        val req =
            OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(input)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "download_$id",
            ExistingWorkPolicy.KEEP,
            req,
        )

        return id
    }
}


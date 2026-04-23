package dev.nemeyes.nextvideo.data.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dev.nemeyes.nextvideo.core.http.OkHttpProvider
import dev.nemeyes.nextvideo.data.accounts.AccountSecretsStore
import dev.nemeyes.nextvideo.data.db.AppDatabase
import dev.nemeyes.nextvideo.data.db.DownloadEntity
import dev.nemeyes.nextvideo.data.db.DownloadStatus
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure()
        val db = AppDatabase.get(applicationContext)
        val secrets = AccountSecretsStore(applicationContext)

        val d = db.downloadDao().getById(downloadId) ?: return Result.failure()
        val acc = db.accountDao().getById(d.accountId) ?: return Result.failure()
        val video = db.videoDao().getById(d.videoId) ?: return Result.failure()

        val auth = secrets.buildBasicAuthHeader(acc.id, acc.loginName) ?: return Result.failure()
        val url = acc.serverBaseUrl.trimEnd('/') + video.href
        val outFile: File = DownloadPaths.videoFile(applicationContext, acc.id, video.id, video.displayName)
        val tmpFile = File(outFile.absolutePath + ".part")

        setForeground(createForegroundInfo(video.displayName))
        update(db, d, DownloadStatus.RUNNING, 0, video.contentLength, null, null)

        try {
            val req =
                Request.Builder()
                    .url(url)
                    .header("Authorization", auth)
                    .build()

            OkHttpProvider.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body ?: error("Empty response")
                val total = body.contentLength().takeIf { it > 0 } ?: video.contentLength

                FileOutputStream(tmpFile).use { fos ->
                    val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    var downloaded = 0L
                    val input = body.byteStream()
                    while (true) {
                        read = input.read(buf)
                        if (read <= 0) break
                        fos.write(buf, 0, read)
                        downloaded += read
                        update(db, d, DownloadStatus.RUNNING, downloaded, total, null, null)
                    }
                }

                if (!tmpFile.renameTo(outFile)) {
                    error("Could not move temp file to destination")
                }
                update(db, d, DownloadStatus.COMPLETED, outFile.length(), total, outFile.absolutePath, null)
            }

            return Result.success()
        } catch (t: Throwable) {
            update(db, d, DownloadStatus.FAILED, d.bytesDownloaded, d.totalBytes, null, t.message ?: t.javaClass.simpleName)
            tmpFile.delete()
            return Result.retry()
        }
    }

    private suspend fun update(
        db: AppDatabase,
        old: DownloadEntity,
        status: DownloadStatus,
        bytesDownloaded: Long,
        totalBytes: Long?,
        localPath: String?,
        errorMessage: String?,
    ) {
        val now = System.currentTimeMillis()
        db.downloadDao().upsert(
            old.copy(
                status = status,
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                localPath = localPath,
                errorMessage = errorMessage,
                updatedAtEpochMs = now,
            ),
        )
    }

    private fun createForegroundInfo(title: String): ForegroundInfo {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "downloads"
        val channel =
            NotificationChannel(
                channelId,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW,
            )
        nm.createNotificationChannel(channel)

        val notif =
            NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle("Downloading")
                .setContentText(title)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .build()

        return ForegroundInfo(1001, notif)
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "downloadId"
    }
}


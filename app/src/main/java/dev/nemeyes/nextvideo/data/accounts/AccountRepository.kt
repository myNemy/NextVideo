package dev.nemeyes.nextvideo.data.accounts

import android.content.Context
import androidx.room.withTransaction
import androidx.work.WorkManager
import dev.nemeyes.nextvideo.data.db.AccountEntity
import dev.nemeyes.nextvideo.data.db.AppDatabase
import dev.nemeyes.nextvideo.data.downloads.DownloadPaths
import java.util.UUID

class AccountRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val secrets: AccountSecretsStore,
) {
    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    private val accountDao get() = db.accountDao()

    fun observeAll() = accountDao.observeAll()

    suspend fun addAccount(
        serverBaseUrl: String,
        loginName: String,
        appPassword: String,
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val libraryHref = "/remote.php/dav/files/${loginName.trim()}/"
        val entity =
            AccountEntity(
                id = id,
                serverBaseUrl = serverBaseUrl.trimEnd('/'),
                loginName = loginName,
                libraryFolderHref = libraryHref,
                createdAtEpochMs = now,
                lastUsedAtEpochMs = now,
            )
        accountDao.insert(entity)
        secrets.putAppPassword(id, appPassword)
        return id
    }

    suspend fun setLibraryFolderHref(accountId: String, href: String) {
        val acc = accountDao.getById(accountId) ?: return
        val normalized =
            when {
                href.isBlank() -> acc.libraryFolderHref
                href.startsWith("/") -> href
                else -> "/" + href
            }.let { if (it.endsWith("/")) it else "$it/" }
        accountDao.update(acc.copy(libraryFolderHref = normalized))
    }

    /**
     * Replace the stored app password (e.g. after rotation in Nextcloud).
     */
    fun setAppPassword(accountId: String, newPassword: String) {
        secrets.putAppPassword(accountId, newPassword)
    }

    /**
     * Cancels in-flight downloads, deletes local files, clears DB rows for the account, and removes the account + secret.
     */
    suspend fun removeAccount(accountId: String) {
        for (d in db.downloadDao().getAllByAccount(accountId)) {
            workManager.cancelUniqueWork("download_${d.id}")
        }
        DownloadPaths.deleteAllFilesForAccount(context, accountId)
        db.withTransaction {
            db.playbackPositionDao().deleteByAccount(accountId)
            db.downloadDao().deleteByAccount(accountId)
            db.videoDao().deleteByAccount(accountId)
            accountDao.deleteById(accountId)
        }
        secrets.removeAppPassword(accountId)
    }
}

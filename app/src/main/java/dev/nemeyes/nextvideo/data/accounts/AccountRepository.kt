package dev.nemeyes.nextvideo.data.accounts

import dev.nemeyes.nextvideo.data.db.AccountDao
import dev.nemeyes.nextvideo.data.db.AccountEntity
import java.util.UUID

class AccountRepository(
    private val accountDao: AccountDao,
    private val secrets: AccountSecretsStore,
) {
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
}


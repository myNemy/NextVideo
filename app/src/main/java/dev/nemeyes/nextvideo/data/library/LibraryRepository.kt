package dev.nemeyes.nextvideo.data.library

import dev.nemeyes.nextvideo.core.http.OkHttpProvider
import dev.nemeyes.nextvideo.data.accounts.AccountSecretsStore
import dev.nemeyes.nextvideo.data.db.AppDatabase
import dev.nemeyes.nextvideo.data.db.VideoEntity
import dev.nemeyes.nextvideo.nextcloud.webdav.NextcloudWebDavClient
import dev.nemeyes.nextvideo.nextcloud.webdav.WebDavItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class LibraryRepository(
    private val db: AppDatabase,
    private val secrets: AccountSecretsStore,
) {
    private val webdav = NextcloudWebDavClient(OkHttpProvider.client)

    data class RefreshResult(
        val folders: List<WebDavItem>,
        val videosUpserted: Int,
    )

    suspend fun refreshFolderDepth1(
        accountId: String,
        serverBaseUrl: String,
        loginName: String,
        folderHref: String,
    ): RefreshResult {
        val auth = secrets.buildBasicAuthHeader(accountId, loginName) ?: error("Missing account secret")
        val items = webdav.listDepth1(serverBaseUrl, folderHref, auth)
        val now = System.currentTimeMillis()

        val folders =
            items
                .filter { it.isDirectory }
                .filter { it.href != folderHref }
                .sortedBy { it.displayName.lowercase() }

        val videos =
            items
                .filter { !it.isDirectory }
                .filter { VideoMime.isLikelyVideo(it.contentType, it.displayName) }
                .map {
                    VideoEntity(
                        id = UUID.nameUUIDFromBytes("$accountId|${it.href}".toByteArray()).toString(),
                        accountId = accountId,
                        href = it.href,
                        displayName = it.displayName,
                        contentType = it.contentType,
                        contentLength = it.contentLength,
                        eTag = it.eTag,
                        lastModified = it.lastModified,
                        isDirectory = false,
                        updatedAtEpochMs = now,
                    )
                }

        withContext(Dispatchers.IO) {
            db.videoDao().upsertAll(videos)
        }

        return RefreshResult(
            folders = folders,
            videosUpserted = videos.size,
        )
    }
}


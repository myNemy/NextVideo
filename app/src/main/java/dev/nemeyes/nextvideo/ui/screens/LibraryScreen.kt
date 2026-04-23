package dev.nemeyes.nextvideo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nemeyes.nextvideo.R
import dev.nemeyes.nextvideo.data.db.AppDatabase
import dev.nemeyes.nextvideo.data.downloads.DownloadRepository
import dev.nemeyes.nextvideo.data.library.LibraryRepository
import dev.nemeyes.nextvideo.nextcloud.webdav.WebDavItem
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    accountId: String,
    db: AppDatabase,
    libraryRepository: LibraryRepository,
    downloadRepository: DownloadRepository,
    onSaveFolderHref: suspend (href: String) -> Unit,
    onOpenVideo: (videoId: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account by db.accountDao().observeAll().collectAsState(initial = emptyList())
    val acc = account.firstOrNull { it.id == accountId }

    var query by remember { mutableStateOf("") }
    var folderHref by remember { mutableStateOf("") }
    var folders by remember { mutableStateOf<List<WebDavItem>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    val videosFlow =
        if (query.isBlank()) {
            db.videoDao().observeVideos(accountId)
        } else {
            db.videoDao().observeSearch(accountId, query)
        }
    val videos by videosFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.title_library))

        if (acc == null) {
            Text(stringResource(R.string.error_account_not_found))
            return@Column
        }

        if (folderHref.isBlank()) folderHref = acc.libraryFolderHref

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        status = context.getString(R.string.status_refreshing)
                        runCatching {
                            val result =
                                libraryRepository.refreshFolderDepth1(
                                    accountId = acc.id,
                                    serverBaseUrl = acc.serverBaseUrl,
                                    loginName = acc.loginName,
                                    folderHref = folderHref,
                                )
                            folders = result.folders
                        }.onSuccess {
                            status = context.getString(R.string.status_done)
                        }.onFailure {
                            status =
                                context.getString(
                                    R.string.status_error_fmt,
                                    it.message ?: it.javaClass.simpleName,
                                )
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.action_refresh))
            }
            Text(acc.serverBaseUrl, modifier = Modifier.weight(1f))
        }

        TextField(
            value = folderHref,
            onValueChange = { folderHref = it },
            label = { Text(stringResource(R.string.field_library_folder_href)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                scope.launch {
                    onSaveFolderHref(folderHref)
                    status = context.getString(R.string.status_saved_folder)
                }
            },
        ) { Text(stringResource(R.string.action_save_folder)) }

        if (status.isNotBlank()) Text(status)

        if (folders.isNotEmpty()) {
            Text(stringResource(R.string.title_folders))
            folders.forEach { f ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                folderHref = f.href
                            }
                            .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(f.displayName)
                    Text(stringResource(R.string.action_open))
                }
            }
        }

        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.field_search)) },
            modifier = Modifier.fillMaxWidth(),
        )

        if (videos.isEmpty()) {
            Text(stringResource(R.string.empty_videos))
        } else {
            videos.forEach { v ->
                val dl by downloadRepository.observeByVideo(accountId, v.id).collectAsState(initial = null)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenVideo(v.id) }
                            .padding(vertical = 10.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(v.displayName)
                            v.contentLength?.let { Text("${it / (1024 * 1024)} MB") }
                            dl?.let {
                                Text(
                                    stringResource(
                                        R.string.download_status_fmt,
                                        it.status.name,
                                        it.bytesDownloaded.toString(),
                                        (it.totalBytes?.toString() ?: stringResource(R.string.unknown_value)),
                                    ),
                                )
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch { downloadRepository.enqueueDownload(accountId, v.id) }
                            },
                        ) {
                            Text(stringResource(R.string.action_download))
                        }
                    }
                }
            }
        }
    }
}


package dev.nemeyes.nextvideo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nemeyes.nextvideo.R
import dev.nemeyes.nextvideo.core.http.NetworkErrorMapper
import dev.nemeyes.nextvideo.data.db.AppDatabase
import dev.nemeyes.nextvideo.data.db.VideoEntity
import dev.nemeyes.nextvideo.data.downloads.DownloadRepository
import dev.nemeyes.nextvideo.data.library.LibraryRepository
import dev.nemeyes.nextvideo.nextcloud.webdav.WebDavItem
import dev.nemeyes.nextvideo.ui.components.FastScrollBar
import dev.nemeyes.nextvideo.ui.theme.ncAppBarTopColors
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
private fun VideoRowSubtitle(
    v: VideoEntity,
) {
    val unknown = stringResource(R.string.unknown_value)
    val sizePart =
        v.contentLength?.let { l ->
            when {
                l >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f GB", l / (1024.0 * 1024.0 * 1024.0))
                l >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", l / (1024.0 * 1024.0))
                l >= 1024L -> String.format(Locale.US, "%.0f KB", l / 1024.0)
                else -> "$l B"
            }
        } ?: unknown
    val mime = v.contentType?.trim()?.takeIf { it.isNotEmpty() } ?: unknown
    Text(
        stringResource(R.string.library_video_subtitle, sizePart, mime),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    accountId: String,
    db: AppDatabase,
    libraryRepository: LibraryRepository,
    downloadRepository: DownloadRepository,
    onSaveFolderHref: suspend (href: String) -> Unit,
    onOpenVideo: (videoId: String) -> Unit,
    showAppBar: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val account by db.accountDao().observeAll().collectAsState(initial = emptyList())
    val acc = account.firstOrNull { it.id == accountId }

    var query by remember { mutableStateOf("") }
    var folderHref by remember { mutableStateOf("") }
    var folders by remember { mutableStateOf<List<WebDavItem>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    val emptyScroll = rememberScrollState()
    val videosFlow =
        if (query.isBlank()) {
            db.videoDao().observeVideos(accountId)
        } else {
            db.videoDao().observeSearch(accountId, query)
        }
    val videos by videosFlow.collectAsState(initial = emptyList())
    val videosListState = rememberLazyListState()

    val content: @Composable (Modifier) -> Unit = { rootMod ->
        Column(
            modifier = rootMod.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (acc == null) {
                Text(stringResource(R.string.error_account_not_found))
                return@Column
            }

            if (folderHref.isBlank()) folderHref = acc.libraryFolderHref

            val doRefresh: () -> Unit = {
                scope.launch {
                    isRefreshing = true
                    try {
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
                                    NetworkErrorMapper.userMessage(context, it),
                                )
                        }
                    } finally {
                        isRefreshing = false
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { doRefresh() },
                ) {
                    Text(stringResource(R.string.action_refresh))
                }
                Text(acc.serverBaseUrl, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
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

            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (folders.isNotEmpty()) {
                Text(stringResource(R.string.title_folders))
                folders.forEach { f ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { folderHref = f.href }
                                .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(f.displayName)
                        Text(stringResource(R.string.action_open), color = MaterialTheme.colorScheme.primary)
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
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { doRefresh() },
                    state = pullState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(emptyScroll),
                    ) {
                        Text(
                            stringResource(R.string.empty_videos),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 480.dp).padding(vertical = 8.dp),
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { doRefresh() },
                        state = pullState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = videosListState,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            itemsIndexed(
                                items = videos,
                                key = { _, v -> v.id },
                            ) { i, v ->
                                val dl by
                                    downloadRepository.observeByVideo(accountId, v.id)
                                        .collectAsState(initial = null)
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (i > 0) HorizontalDivider()
                                    ListItem(
                                        modifier = Modifier.clickable { onOpenVideo(v.id) },
                                        headlineContent = {
                                            Text(
                                                v.displayName,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        supportingContent = {
                                            Column {
                                                VideoRowSubtitle(v = v)
                                                dl?.let { dle ->
                                                    Text(
                                                        stringResource(
                                                            R.string.download_status_fmt,
                                                            dle.status.name,
                                                            dle.bytesDownloaded.toString(),
                                                            dle.totalBytes?.toString()
                                                                ?: stringResource(R.string.unknown_value),
                                                        ),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                        },
                                        trailingContent = {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        downloadRepository.enqueueDownload(accountId, v.id)
                                                    }
                                                },
                                            ) {
                                                Text(stringResource(R.string.action_download))
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    FastScrollBar(
                        state = videosListState,
                        totalItems = videos.size,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
                        thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    )
                }
            }
        }
    }

    if (!showAppBar) {
        content(Modifier)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_library)) },
                colors = ncAppBarTopColors(),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        content(Modifier.padding(padding))
    }
}


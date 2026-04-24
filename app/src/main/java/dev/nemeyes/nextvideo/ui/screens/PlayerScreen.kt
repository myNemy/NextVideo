package dev.nemeyes.nextvideo.ui.screens

import android.net.Uri
import android.view.ContextThemeWrapper
import androidx.activity.compose.BackHandler
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import dev.nemeyes.nextvideo.R
import dev.nemeyes.nextvideo.core.http.OkHttpProvider
import dev.nemeyes.nextvideo.data.accounts.AccountSecretsStore
import dev.nemeyes.nextvideo.data.db.AppDatabase
import dev.nemeyes.nextvideo.data.db.PlaybackPositionEntity
import dev.nemeyes.nextvideo.data.downloads.DownloadPaths
import dev.nemeyes.nextvideo.ui.theme.ncAppBarTopColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking

/** If less than this many ms from the end, treat as "finished" and do not resume next time. */
private const val END_OFFSET_MS = 5_000L

private const val MAX_WAIT_FOR_READY_HOPS = 250

private fun ExoPlayer.seekToSavedIfNeeded(savedMs: Long) {
    if (savedMs <= 0) return
    val duration = this.duration
    if (duration == C.TIME_UNSET || duration <= 0) {
        this.seekTo(savedMs)
        return
    }
    if (savedMs < duration - END_OFFSET_MS) {
        this.seekTo(savedMs)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    accountId: String,
    videoId: String,
    db: AppDatabase,
    secrets: AccountSecretsStore,
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val accountState by db.accountDao().observeAll().collectAsState(initial = emptyList())
    val acc = accountState.firstOrNull { it.id == accountId }
    val videosState by db.videoDao().observeVideos(accountId).collectAsState(initial = emptyList())
    val video = videosState.firstOrNull { it.id == videoId }

    if (acc == null || video == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(stringResource(R.string.error_missing_account_video))
        }
        return
    }

    // -1 = still loading; then 0+ ms
    var savedPositionMs by remember(accountId, videoId) { mutableStateOf(-1L) }
    LaunchedEffect(accountId, videoId) {
        val row = db.playbackPositionDao().get(accountId, videoId)
        savedPositionMs = row?.positionMs ?: 0L
    }

    val videoUrl =
        remember(accountId, videoId, acc.id, video.id, video.href, video.displayName) {
            val local = DownloadPaths.videoFile(context, acc.id, video.id, video.displayName)
            if (local.exists()) {
                Uri.fromFile(local).toString()
            } else {
                acc.serverBaseUrl.trimEnd('/') + video.href
            }
        }

    val authHeader = secrets.buildBasicAuthHeader(acc.id, acc.loginName)
    val player =
        remember(accountId, videoId) {
            val dataSourceFactory =
                OkHttpDataSource.Factory(OkHttpProvider.client).apply {
                    if (authHeader != null) {
                        setDefaultRequestProperties(mapOf("Authorization" to authHeader))
                    }
                }
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build()
        }

    DisposableEffect(player, videoUrl) {
        val uri = Uri.parse(videoUrl)
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
        onDispose { player.release() }
    }

    var resumeApplied by remember(accountId, videoId) { mutableStateOf(false) }
    val latestPlayer by rememberUpdatedState(player)
    val latestResumeMs by rememberUpdatedState(savedPositionMs)
    LaunchedEffect(savedPositionMs, accountId, videoId) {
        if (latestResumeMs < 0 || latestResumeMs == 0L) {
            if (latestResumeMs == 0L) {
                resumeApplied = true
            }
            return@LaunchedEffect
        }
        if (resumeApplied) return@LaunchedEffect
        var n = 0
        while (n++ < MAX_WAIT_FOR_READY_HOPS && isActive) {
            when (latestPlayer.playbackState) {
                Player.STATE_IDLE -> {
                    if (n > 20) {
                        return@LaunchedEffect
                    }
                }
                Player.STATE_READY -> {
                    latestPlayer.seekToSavedIfNeeded(latestResumeMs)
                    resumeApplied = true
                    return@LaunchedEffect
                }
                else -> { }
            }
            delay(32L)
        }
    }

    val posDao = remember { db.playbackPositionDao() }
    val savePlayback: () -> Unit = {
        val p = player.currentPosition
        val d = player.duration
        if (d != C.TIME_UNSET && d > 0) {
            val toStore =
                if (p >= d - END_OFFSET_MS) {
                    0L
                } else {
                    p.coerceAtLeast(0L)
                }
            runBlocking(Dispatchers.IO) {
                posDao.upsert(
                    PlaybackPositionEntity(
                        accountId = accountId,
                        videoId = videoId,
                        positionMs = toStore,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }
    val latestSave by rememberUpdatedState(savePlayback)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, accountId, videoId) {
        val obs =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    latestSave()
                }
            }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            latestSave()
        }
    }

    val exoTheme = R.style.Theme_NextVideo_ExoPlayer
    val title = video.displayName

    BackHandler(onBack = onNavigateUp)

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                        )
                    }
                },
                colors = ncAppBarTopColors(),
            )
        },
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { ctx ->
                val wrapped = ContextThemeWrapper(ctx, exoTheme)
                PlayerView(wrapped).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { v -> v.player = player },
        )
    }
}

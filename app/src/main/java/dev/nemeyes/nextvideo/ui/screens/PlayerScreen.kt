package dev.nemeyes.nextvideo.ui.screens

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import dev.nemeyes.nextvideo.R
import dev.nemeyes.nextvideo.core.http.OkHttpProvider
import dev.nemeyes.nextvideo.data.accounts.AccountSecretsStore
import dev.nemeyes.nextvideo.data.db.AppDatabase
import androidx.compose.runtime.collectAsState
import dev.nemeyes.nextvideo.data.downloads.DownloadPaths
import java.io.File

@Composable
fun PlayerScreen(
    accountId: String,
    videoId: String,
    db: AppDatabase,
    secrets: AccountSecretsStore,
) {
    val context = LocalContext.current
    val accountState by db.accountDao().observeAll().collectAsState(initial = emptyList())
    val acc = accountState.firstOrNull { it.id == accountId }
    val videosState by db.videoDao().observeVideos(accountId).collectAsState(initial = emptyList())
    val video = videosState.firstOrNull { it.id == videoId }

    if (acc == null || video == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(stringResource(R.string.error_missing_account_video))
        }
        return
    }

    val authHeader = secrets.buildBasicAuthHeader(acc.id, acc.loginName)
    val localFile = DownloadPaths.videoFile(context, acc.id, video.id, video.displayName)
    val videoUrl =
        if (localFile.exists()) {
            Uri.fromFile(localFile).toString()
        } else {
            acc.serverBaseUrl.trimEnd('/') + video.href
        }

    val currentAuth by rememberUpdatedState(authHeader)
    val currentUrl by rememberUpdatedState(videoUrl)

    val player =
        remember {
            val dataSourceFactory =
                OkHttpDataSource.Factory(OkHttpProvider.client).apply {
                    if (currentAuth != null) {
                        setDefaultRequestProperties(mapOf("Authorization" to currentAuth!!))
                    }
                }
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build()
        }

    DisposableEffect(player, currentUrl) {
        player.setMediaItem(MediaItem.fromUri(Uri.parse(currentUrl)))
        player.prepare()
        player.playWhenReady = true
        onDispose { player.release() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                this.player = player
            }
        },
        update = { it.player = player },
    )
}


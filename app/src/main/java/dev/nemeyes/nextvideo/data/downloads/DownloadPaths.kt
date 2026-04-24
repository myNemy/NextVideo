package dev.nemeyes.nextvideo.data.downloads

import android.content.Context
import java.io.File

object DownloadPaths {
    fun downloadsDir(context: Context): File = File(context.filesDir, "downloads").apply { mkdirs() }

    fun videoFile(context: Context, accountId: String, videoId: String, suggestedName: String): File {
        val safeName = suggestedName.replace(Regex("""[^\w.\- ]+"""), "_").take(120)
        return File(downloadsDir(context), "${accountId}_${videoId}_$safeName")
    }

    /**
     * Removes on-disk download files and partials for a given [accountId] in app-private storage.
     */
    fun deleteAllFilesForAccount(context: Context, accountId: String) {
        val prefix = "${accountId}_"
        downloadsDir(context)
            .listFiles()
            ?.forEach { f ->
                if (f.isFile && f.name.startsWith(prefix)) f.delete()
            }
    }
}


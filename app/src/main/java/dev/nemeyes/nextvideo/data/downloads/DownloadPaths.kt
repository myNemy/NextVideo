package dev.nemeyes.nextvideo.data.downloads

import android.content.Context
import java.io.File

object DownloadPaths {
    fun downloadsDir(context: Context): File = File(context.filesDir, "downloads").apply { mkdirs() }

    fun videoFile(context: Context, accountId: String, videoId: String, suggestedName: String): File {
        val safeName = suggestedName.replace(Regex("""[^\w.\- ]+"""), "_").take(120)
        return File(downloadsDir(context), "${accountId}_${videoId}_$safeName")
    }
}


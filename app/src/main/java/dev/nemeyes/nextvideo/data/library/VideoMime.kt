package dev.nemeyes.nextvideo.data.library

object VideoMime {
    private val videoExt = setOf("mp4", "mkv", "webm", "mov", "avi", "m4v")

    fun isLikelyVideo(contentType: String?, displayName: String): Boolean {
        if (contentType?.startsWith("video/") == true) return true
        val ext = displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return ext in videoExt
    }
}


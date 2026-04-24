package dev.nemeyes.nextvideo.nextcloud.webdav

import kotlinx.coroutines.delay
import java.io.IOException
import java.io.InterruptedIOException

/**
 * Retries [block] a few times with exponential backoff for transient I/O and HTTP cases.
 * The block should run on an I/O thread (e.g. inside [NextcloudWebDavClient]’s IO dispatcher).
 */
internal object WebDavRequestRetry {
    private const val MAX_ATTEMPTS = 3
    private const val INITIAL_DELAY_MS = 400L
    private const val MAX_DELAY_MS = 4_000L

    /**
     * Nextcloud can return 429/5xx on overload; 408/504 can appear for timeouts. 4xx auth/permission/404
     * are not retried here.
     */
    fun isTransientStatus(httpCode: Int): Boolean {
        return when (httpCode) {
            408, 429, 500, 502, 503, 504 -> true
            else -> false
        }
    }

    suspend fun <T> withBackoff(
        maxAttempts: Int = MAX_ATTEMPTS,
        block: () -> T,
    ): T {
        var delayMs = INITIAL_DELAY_MS
        for (attempt in 0 until maxAttempts) {
            try {
                return block()
            } catch (e: WebDavHttpException) {
                if (!isTransientStatus(e.httpCode) || attempt == maxAttempts - 1) {
                    throw e
                }
            } catch (e: IOException) {
                if (e is InterruptedIOException) throw e
                if (attempt == maxAttempts - 1) throw e
            }
            if (attempt < maxAttempts - 1) {
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(MAX_DELAY_MS)
            }
        }
        @Suppress("UNREACHABLE_CODE")
        error("withBackoff: exhausted")
    }
}

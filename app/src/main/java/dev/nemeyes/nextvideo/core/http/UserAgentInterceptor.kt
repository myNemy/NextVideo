package dev.nemeyes.nextvideo.core.http

import dev.nemeyes.nextvideo.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Sets a single identifiable User-Agent on every request, in the same spirit as the official
 * [Nextcloud Android](https://github.com/nextcloud/android) client (see `nextcloud_user_agent` in
 * their resources), so server and proxy logs can distinguish this app without embedding secrets.
 */
class UserAgentInterceptor : Interceptor {
    private val userAgent: String =
        "Mozilla/5.0 (Android) NextVideo/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain
                .request()
                .newBuilder()
                .header("User-Agent", userAgent)
                .build()
        return chain.proceed(request)
    }
}

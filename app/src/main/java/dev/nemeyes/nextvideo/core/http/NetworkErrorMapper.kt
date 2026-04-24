package dev.nemeyes.nextvideo.core.http

import android.content.Context
import dev.nemeyes.nextvideo.R
import dev.nemeyes.nextvideo.nextcloud.webdav.WebDavHttpException
import org.json.JSONException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Maps throwables to short, user-facing messages without echoing raw [Throwable.message] (may contain
 * URLs or other sensitive details from OkHttp/SSL).
 */
object NetworkErrorMapper {
    fun userMessage(context: Context, t: Throwable): String {
        val root = rootCause(t)
        return when (root) {
            is WebDavHttpException -> messageForWebDavHttp(context, root.httpCode)
            is JSONException -> context.getString(R.string.error_login_response)
            is UnknownHostException -> context.getString(R.string.error_network_unknown_host)
            is SocketTimeoutException -> context.getString(R.string.error_network_timeout)
            is ConnectException -> context.getString(R.string.error_network_connect)
            is SSLException -> context.getString(R.string.error_network_ssl)
            is java.security.cert.CertificateException -> context.getString(R.string.error_network_ssl)
            is IOException -> context.getString(R.string.error_network_io)
            else -> context.getString(R.string.error_generic, root.javaClass.simpleName)
        }
    }

    private fun messageForWebDavHttp(context: Context, code: Int): String {
        return when (code) {
            401 -> context.getString(R.string.error_http_unauthorized)
            403 -> context.getString(R.string.error_http_forbidden)
            404 -> context.getString(R.string.error_http_not_found)
            in 500..599 -> context.getString(R.string.error_http_server)
            else -> context.getString(R.string.error_http_status, code)
        }
    }

    private tailrec fun rootCause(t: Throwable): Throwable {
        val c = t.cause
        return if (c != null && c != t) rootCause(c) else t
    }
}

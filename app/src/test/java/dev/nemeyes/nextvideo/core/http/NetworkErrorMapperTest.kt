package dev.nemeyes.nextvideo.core.http

import dev.nemeyes.nextvideo.R
import dev.nemeyes.nextvideo.nextcloud.webdav.WebDavHttpException
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.DEFAULT_MANIFEST_NAME, minSdk = 28, maxSdk = 35)
class NetworkErrorMapperTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun webDav401_usesUnauthString() {
        val s =
            NetworkErrorMapper.userMessage(
                context,
                WebDavHttpException(401),
            )
        val expected = context.getString(R.string.error_http_unauthorized)
        assertTrue(s == expected)
    }

    @Test
    fun webDav502_usesServerString() {
        val s = NetworkErrorMapper.userMessage(context, WebDavHttpException(502))
        val expected = context.getString(R.string.error_http_server)
        assertTrue(s == expected)
    }

    @Test
    fun unknownHost_resolves() {
        val s = NetworkErrorMapper.userMessage(context, UnknownHostException("example.invalid"))
        assertTrue(
            s == context.getString(R.string.error_network_unknown_host),
        )
    }

    @Test
    fun connectException_resolves() {
        val s = NetworkErrorMapper.userMessage(context, ConnectException("refused"))
        assertTrue(s == context.getString(R.string.error_network_connect))
    }

    @Test
    fun timeout_resolves() {
        val s = NetworkErrorMapper.userMessage(context, SocketTimeoutException("slow"))
        assertTrue(s == context.getString(R.string.error_network_timeout))
    }

    @Test
    fun ssl_resolves() {
        val s = NetworkErrorMapper.userMessage(context, SSLHandshakeException("x"))
        assertTrue(s == context.getString(R.string.error_network_ssl))
    }
}

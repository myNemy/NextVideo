package dev.nemeyes.nextvideo.nextcloud.webdav

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.DEFAULT_MANIFEST_NAME, minSdk = 28, maxSdk = 35)
class WebDavRequestRetryTest {

    @Test
    fun isTransientStatus_503_429_502() {
        assertTrue(WebDavRequestRetry.isTransientStatus(503))
        assertTrue(WebDavRequestRetry.isTransientStatus(429))
        assertTrue(WebDavRequestRetry.isTransientStatus(502))
    }

    @Test
    fun isTransientStatus_401_404_403() {
        assertFalse(WebDavRequestRetry.isTransientStatus(401))
        assertFalse(WebDavRequestRetry.isTransientStatus(404))
        assertFalse(WebDavRequestRetry.isTransientStatus(403))
    }

    @Test
    fun withBackoff_succeedsAfterIOException() = runBlocking {
        var n = 0
        val r =
            WebDavRequestRetry.withBackoff {
                n++
                if (n < 2) throw IOException("simulated")
                42
            }
        assertEquals(2, n)
        assertEquals(42, r)
    }

    @Test
    fun withBackoff_doesNotRetry_404() = runBlocking {
        var n = 0
        val e =
            runCatching {
                WebDavRequestRetry.withBackoff {
                    n++
                    throw WebDavHttpException(404)
                }
            }.exceptionOrNull()
        assertTrue(e is WebDavHttpException)
        assertEquals(404, (e as WebDavHttpException).httpCode)
        assertEquals(1, n)
    }
}

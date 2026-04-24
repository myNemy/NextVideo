package dev.nemeyes.nextvideo.nextcloud.webdav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.DEFAULT_MANIFEST_NAME, minSdk = 28, maxSdk = 35)
class WebDavMultistatusParserTest {
    @Test
    fun parse_returnsDirectoryAndFile() {
        val xml =
            """
            <?xml version="1.0"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/remote.php/dav/files/u/Videos/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype><d:collection/></d:resourcetype>
                    <d:displayname>Videos</d:displayname>
                  </d:prop>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/remote.php/dav/files/u/Videos/a.mp4</d:href>
                <d:propstat>
                  <d:prop>
                    <d:displayname>a.mp4</d:displayname>
                    <d:getcontenttype>video/mp4</d:getcontenttype>
                    <d:getcontentlength>1234</d:getcontentlength>
                  </d:prop>
                </d:propstat>
              </d:response>
            </d:multistatus>
            """.trimIndent()

        val items =
            WebDavMultistatusParser.parse(
                ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)),
            )

        assertEquals(2, items.size)
        val folder = items[0]
        assertTrue(folder.isDirectory)
        assertEquals("Videos", folder.displayName)
        assertTrue(folder.href.endsWith("Videos/"))

        val file = items[1]
        assertFalse(file.isDirectory)
        assertEquals("a.mp4", file.displayName)
        assertEquals("video/mp4", file.contentType)
        assertEquals(1234L, file.contentLength)
    }
}

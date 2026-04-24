package dev.nemeyes.nextvideo.nextcloud.webdav

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Minimal PROPFIND multistatus parser. Shared by [NextcloudWebDavClient] and unit tests.
 * Uses [XmlPullParserFactory] (no [android.util.Xml]) so the same code runs in JVM unit tests.
 */
object WebDavMultistatusParser {
    fun parse(input: InputStream): List<WebDavItem> {
        val parser =
            XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = true
            }.newPullParser()
        parser.setInput(input, null)
        return parseFrom(parser)
    }

    @Throws(XmlPullParserException::class, java.io.IOException::class)
    private fun parseFrom(parser: XmlPullParser): List<WebDavItem> {
        val items = mutableListOf<WebDavItem>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "response") {
                parseResponse(parser)?.let { items += it }
            }
            event = parser.next()
        }
        return items
    }

    @Throws(XmlPullParserException::class, java.io.IOException::class)
    private fun parseResponse(parser: XmlPullParser): WebDavItem? {
        var href: String? = null
        var displayName: String? = null
        var contentType: String? = null
        var contentLength: Long? = null
        var eTag: String? = null
        var lastModified: String? = null
        var isDirectory = false

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "response")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "href" -> href = parser.nextText()
                    "displayname" -> displayName = parser.nextText()
                    "getcontenttype" -> contentType = parser.nextText().takeIf { it.isNotBlank() }
                    "getcontentlength" -> contentLength = parser.nextText().toLongOrNull()
                    "getetag" -> eTag = parser.nextText().trim().trim('"').takeIf { it.isNotBlank() }
                    "getlastmodified" -> lastModified = parser.nextText().takeIf { it.isNotBlank() }
                    "collection" -> isDirectory = true
                }
            }
            event = parser.next()
        }

        val h = href ?: return null
        val name = displayName?.takeIf { it.isNotBlank() } ?: h.trimEnd('/').substringAfterLast('/')
        return WebDavItem(
            href = h,
            displayName = name,
            isDirectory = isDirectory,
            contentType = contentType,
            contentLength = contentLength,
            eTag = eTag,
            lastModified = lastModified,
        )
    }
}

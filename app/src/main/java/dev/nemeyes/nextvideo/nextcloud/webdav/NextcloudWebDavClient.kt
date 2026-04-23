package dev.nemeyes.nextvideo.nextcloud.webdav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import android.util.Xml

data class WebDavItem(
    val href: String,
    val displayName: String,
    val isDirectory: Boolean,
    val contentType: String?,
    val contentLength: Long?,
    val eTag: String?,
    val lastModified: String?,
)

class NextcloudWebDavClient(
    private val http: OkHttpClient,
) {
    suspend fun listDepth1(
        serverBaseUrl: String,
        folderHref: String,
        authorization: String,
    ): List<WebDavItem> =
        withContext(Dispatchers.IO) {
            val url = serverBaseUrl.trimEnd('/') + folderHref
            val body =
                """
                <?xml version="1.0"?>
                <d:propfind xmlns:d="DAV:">
                  <d:prop>
                    <d:displayname />
                    <d:getcontenttype />
                    <d:getcontentlength />
                    <d:getetag />
                    <d:getlastmodified />
                    <d:resourcetype />
                  </d:prop>
                </d:propfind>
                """.trimIndent()
                    .toRequestBody("application/xml; charset=utf-8".toMediaType())

            val req =
                Request.Builder()
                    .url(url)
                    .method("PROPFIND", body)
                    .header("Depth", "1")
                    .header("Authorization", authorization)
                    .build()

            http.newCall(req).execute().use { resp ->
                check(resp.isSuccessful) { "PROPFIND failed: HTTP ${resp.code}" }
                val xml = resp.body?.byteStream() ?: return@withContext emptyList()
                parseMultiStatus(xml)
            }
        }

    private fun parseMultiStatus(input: java.io.InputStream): List<WebDavItem> {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, null)

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


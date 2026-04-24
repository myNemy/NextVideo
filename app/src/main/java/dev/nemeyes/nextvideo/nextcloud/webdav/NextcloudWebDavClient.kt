package dev.nemeyes.nextvideo.nextcloud.webdav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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

            WebDavRequestRetry.withBackoff {
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw WebDavHttpException(resp.code)
                    }
                    val xml = resp.body?.byteStream() ?: return@withBackoff emptyList()
                    WebDavMultistatusParser.parse(xml)
                }
            }
        }
}


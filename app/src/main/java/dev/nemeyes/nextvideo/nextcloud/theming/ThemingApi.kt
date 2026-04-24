package dev.nemeyes.nextvideo.nextcloud.theming

import dev.nemeyes.nextvideo.core.http.OkHttpProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

data class InstanceTheming(
    val primaryHex: String?,
    val onPrimaryHex: String?,
    /** OCS `name` (Nextcloud theming) — the instance display name, when returned. */
    val instanceName: String? = null,
)

object ThemingApi {
    /**
     * Fetch instance theming colors (primary + text-on-primary) using Nextcloud OCS theming endpoint.
     * Safe to call from a coroutine; network runs on an I/O dispatcher.
     *
     * This endpoint is typically public and does not require auth.
     */
    suspend fun fetchOrNull(serverBaseUrl: String): InstanceTheming? =
        withContext(Dispatchers.IO) { runCatching { fetchBlocking(serverBaseUrl) }.getOrNull() }

    private fun fetchBlocking(serverBaseUrl: String): InstanceTheming? {
        val url =
            serverBaseUrl.trimEnd('/') +
                "/ocs/v2.php/apps/theming/api/v1/theme?format=json"

        val req =
            Request.Builder()
                .url(url)
                .header("OCS-APIRequest", "true")
                .header("Accept", "application/json")
                .build()

        OkHttpProvider.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string()?.takeIf { it.isNotBlank() } ?: return null
            val root = JSONObject(body)
            val ocs = root.optJSONObject("ocs") ?: return null
            val data = ocs.optJSONObject("data") ?: return null

            val primary = data.optString("color").ifBlank { null }
            // Nextcloud theming uses "color-text" (sometimes "colorText")
            val onPrimary = data.optString("color-text").ifBlank { null }
                ?: data.optString("colorText").ifBlank { null }
            val instanceName = data.optString("name").trim().ifBlank { null }

            return InstanceTheming(
                primaryHex = primary,
                onPrimaryHex = onPrimary,
                instanceName = instanceName,
            )
        }
    }
}


package dev.nemeyes.nextvideo.nextcloud.theming

import dev.nemeyes.nextvideo.core.http.OkHttpProvider
import okhttp3.Request
import org.json.JSONObject

data class InstanceTheming(
    val primaryHex: String?,
    val onPrimaryHex: String?,
)

object ThemingApi {
    /**
     * Fetch instance theming colors (primary + text-on-primary) using Nextcloud OCS theming endpoint.
     *
     * This endpoint is typically public and does not require auth.
     */
    fun fetch(serverBaseUrl: String): InstanceTheming? {
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

            val primary = data.optString("color", null)
            // Nextcloud theming uses "color-text" (sometimes available as "colorText" in some clients)
            val onPrimary = data.optString("color-text", null).ifBlank { null }
                ?: data.optString("colorText", null).ifBlank { null }

            return InstanceTheming(
                primaryHex = primary?.takeIf { it.isNotBlank() },
                onPrimaryHex = onPrimary,
            )
        }
    }
}


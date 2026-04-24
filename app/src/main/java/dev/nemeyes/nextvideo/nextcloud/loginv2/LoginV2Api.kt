package dev.nemeyes.nextvideo.nextcloud.loginv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject

data class LoginV2Start(
    val loginUrl: String,
    val pollUrl: String,
    val token: String,
)

data class LoginV2PollResult(
    val server: String,
    val loginName: String,
    val appPassword: String,
)

class LoginV2Api(
    private val http: OkHttpClient,
) {
    suspend fun start(serverBaseUrl: String): LoginV2Start =
        withContext(Dispatchers.IO) {
            val url = serverBaseUrl.trimEnd('/') + "/index.php/login/v2"
            val req =
                Request.Builder()
                    .url(url)
                    .post(FormBody.Builder().build())
                    .build()

            http.newCall(req).execute().use { resp ->
                check(resp.isSuccessful) { "Login v2 start failed: HTTP ${resp.code}" }
                val body = resp.body?.string().orEmpty()
                val json =
                    try {
                        JSONObject(body)
                    } catch (_: JSONException) {
                        error("Login v2: invalid response from server")
                    }
                val login = json.optString("login", "")
                val poll = json.optJSONObject("poll")
                if (login.isNotBlank() && poll != null) {
                    val endpoint = poll.optString("endpoint", "")
                    val token = poll.optString("token", "")
                    if (endpoint.isNotEmpty() && token.isNotEmpty()) {
                        LoginV2Start(
                            loginUrl = login,
                            pollUrl = endpoint,
                            token = token,
                        )
                    } else {
                        error("Login v2: missing poll token or endpoint")
                    }
                } else {
                    error("Login v2: unexpected start response from server")
                }
            }
        }

    suspend fun pollUntilApproved(
        pollUrl: String,
        token: String,
        maxAttempts: Int = 60,
        delayMs: Long = 2000,
    ): LoginV2PollResult =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder().add("token", token).build()
            repeat(maxAttempts) { attempt ->
                val req =
                    Request.Builder()
                        .url(pollUrl)
                        .post(body)
                        .build()

                http.newCall(req).execute().use { resp ->
                    if (resp.code == 404 || resp.code == 401) {
                        // Not approved yet (Nextcloud returns 404 until confirmed on some versions)
                    } else if (!resp.isSuccessful) {
                        // Other errors are likely transient; keep polling a bit.
                    } else {
                        val text = resp.body?.string().orEmpty()
                        val json =
                            try {
                                JSONObject(text)
                            } catch (_: JSONException) {
                                return@use
                            }
                        val server = json.optString("server", "").trim()
                        val loginName = json.optString("loginName", "").trim()
                        val appPassword = json.optString("appPassword", "")
                        if (server.isNotEmpty() && loginName.isNotEmpty() && appPassword.isNotEmpty()) {
                            return@withContext LoginV2PollResult(
                                server = server.trimEnd('/'),
                                loginName = loginName,
                                appPassword = appPassword,
                            )
                        }
                    }
                }

                if (attempt != maxAttempts - 1) delay(delayMs)
            }
            error("Login approval timed out")
        }
}


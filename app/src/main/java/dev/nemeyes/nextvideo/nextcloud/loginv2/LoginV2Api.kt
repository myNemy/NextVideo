package dev.nemeyes.nextvideo.nextcloud.loginv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
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
                val json = JSONObject(body)
                val login = json.getString("login")
                val poll = json.getJSONObject("poll")
                LoginV2Start(
                    loginUrl = login,
                    pollUrl = poll.getString("endpoint"),
                    token = poll.getString("token"),
                )
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
                        val json = JSONObject(text)
                        val server = json.getString("server")
                        val loginName = json.getString("loginName")
                        val appPassword = json.getString("appPassword")
                        return@withContext LoginV2PollResult(
                            server = server.trimEnd('/'),
                            loginName = loginName,
                            appPassword = appPassword,
                        )
                    }
                }

                if (attempt != maxAttempts - 1) delay(delayMs)
            }
            error("Login approval timed out")
        }
}


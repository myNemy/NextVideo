package dev.nemeyes.nextvideo.data.accounts

import android.content.Context
import android.util.Base64
import dev.nemeyes.nextvideo.core.security.EncryptedPrefs

class AccountSecretsStore(context: Context) {
    private val prefs = EncryptedPrefs.create(context)

    fun putAppPassword(accountId: String, appPassword: String) {
        prefs.edit().putString(keyAppPassword(accountId), appPassword).apply()
    }

    fun getAppPassword(accountId: String): String? = prefs.getString(keyAppPassword(accountId), null)

    fun buildBasicAuthHeader(accountId: String, loginName: String): String? {
        val appPassword = getAppPassword(accountId) ?: return null
        val userPass = "$loginName:$appPassword"
        val b64 = Base64.encodeToString(userPass.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "Basic $b64"
    }

    private fun keyAppPassword(accountId: String) = "account.$accountId.appPassword"
}


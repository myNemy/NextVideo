package dev.nemeyes.nextvideo.ui

object NavRoutes {
    const val MainTabs = "mainTabs"
    const val AddAccount = "addAccount"
    const val Player = "player"

    fun mainTabs(accountId: String? = null): String =
        if (accountId.isNullOrBlank()) MainTabs else "$MainTabs?accountId=$accountId"

    fun player(accountId: String, videoId: String) = "$Player/$accountId/$videoId"
}


package dev.nemeyes.nextvideo.ui

object NavRoutes {
    const val Accounts = "accounts"
    const val AddAccount = "addAccount"
    const val Library = "library"
    const val Player = "player"

    fun library(accountId: String) = "$Library/$accountId"

    fun player(accountId: String, videoId: String) = "$Player/$accountId/$videoId"
}


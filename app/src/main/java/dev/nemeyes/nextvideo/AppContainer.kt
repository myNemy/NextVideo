package dev.nemeyes.nextvideo

import android.content.Context
import dev.nemeyes.nextvideo.data.accounts.AccountSecretsStore
import dev.nemeyes.nextvideo.data.db.AppDatabase
import dev.nemeyes.nextvideo.data.downloads.DownloadRepository
import dev.nemeyes.nextvideo.data.library.LibraryRepository
import dev.nemeyes.nextvideo.nextcloud.loginv2.LoginV2Api
import dev.nemeyes.nextvideo.core.http.OkHttpProvider

class AppContainer(context: Context) {
    val db: AppDatabase = AppDatabase.get(context)
    val secrets: AccountSecretsStore = AccountSecretsStore(context)

    val loginV2Api: LoginV2Api = LoginV2Api(OkHttpProvider.client)
    val libraryRepository: LibraryRepository = LibraryRepository(db, secrets)
    val downloadRepository: DownloadRepository = DownloadRepository(context, db)
}


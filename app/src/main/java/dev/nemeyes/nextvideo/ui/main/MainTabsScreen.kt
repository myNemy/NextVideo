package dev.nemeyes.nextvideo.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nemeyes.nextvideo.R
import dev.nemeyes.nextvideo.data.accounts.AccountRepository
import dev.nemeyes.nextvideo.data.db.AppDatabase
import dev.nemeyes.nextvideo.data.downloads.DownloadRepository
import dev.nemeyes.nextvideo.data.library.LibraryRepository
import dev.nemeyes.nextvideo.ui.screens.AccountsScreen
import dev.nemeyes.nextvideo.ui.screens.InfoScreen
import dev.nemeyes.nextvideo.ui.screens.LibraryScreen
import dev.nemeyes.nextvideo.ui.screens.SettingsScreen

private enum class MainTab(
    val labelRes: Int,
) {
    Home(R.string.tab_home),
    Settings(R.string.tab_settings),
    Library(R.string.tab_library),
    Info(R.string.tab_info),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(
    accountRepository: AccountRepository,
    db: AppDatabase,
    libraryRepository: LibraryRepository,
    downloadRepository: DownloadRepository,
    initialAccountId: String? = null,
    onAddAccount: () -> Unit,
    onOpenVideo: (accountId: String, videoId: String) -> Unit,
    onSaveFolderHref: suspend (accountId: String, href: String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedAccountId by remember { mutableStateOf<String?>(initialAccountId) }

    Scaffold(
        topBar = {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                MainTab.entries.forEachIndexed { idx, tab ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (MainTab.entries[selectedTab]) {
            MainTab.Home -> {
                AccountsScreen(
                    accountRepository = accountRepository,
                    onAddAccount = onAddAccount,
                    onOpenAccount = { id ->
                        selectedAccountId = id
                        selectedTab = MainTab.Library.ordinal
                    },
                )
            }
            MainTab.Library -> {
                val accId = selectedAccountId
                if (accId == null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    ) {
                        Text(stringResource(R.string.library_select_account_hint))
                    }
                } else {
                    LibraryScreen(
                        accountId = accId,
                        db = db,
                        libraryRepository = libraryRepository,
                        downloadRepository = downloadRepository,
                        onSaveFolderHref = { href -> onSaveFolderHref(accId, href) },
                        onOpenVideo = { videoId -> onOpenVideo(accId, videoId) },
                    )
                }
            }
            MainTab.Settings -> SettingsScreen(modifier = Modifier.padding(padding))
            MainTab.Info -> InfoScreen(modifier = Modifier.padding(padding))
        }
    }
}


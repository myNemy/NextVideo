package dev.nemeyes.nextvideo.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
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
import dev.nemeyes.nextvideo.nextcloud.theming.InstanceTheming
import dev.nemeyes.nextvideo.nextcloud.theming.ThemingApi
import dev.nemeyes.nextvideo.ui.screens.AccountsScreen
import dev.nemeyes.nextvideo.ui.screens.InfoScreen
import dev.nemeyes.nextvideo.ui.screens.LibraryScreen
import dev.nemeyes.nextvideo.ui.screens.SettingsScreen

private enum class MainTab(
    val labelRes: Int,
    val icon: @Composable () -> Unit,
) {
    Home(R.string.tab_home, icon = { Icon(Icons.Outlined.Home, contentDescription = null) }),
    Library(R.string.tab_library, icon = { Icon(Icons.Outlined.VideoLibrary, contentDescription = null) }),
    Settings(R.string.tab_settings, icon = { Icon(Icons.Outlined.Settings, contentDescription = null) }),
    Info(R.string.tab_info, icon = { Icon(Icons.Outlined.Info, contentDescription = null) }),
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
    onThemingChanged: (InstanceTheming?) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedAccountId by remember { mutableStateOf<String?>(initialAccountId) }

    LaunchedEffect(selectedAccountId) {
        val id = selectedAccountId
        if (id.isNullOrBlank()) {
            onThemingChanged(null)
            return@LaunchedEffect
        }

        val acc = db.accountDao().getById(id)
        val theming = acc?.let { ThemingApi.fetch(it.serverBaseUrl) }
        onThemingChanged(theming)
    }

    val currentTab = MainTab.entries[selectedTab]
    val topBarColors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentTab) {
                            MainTab.Home -> stringResource(R.string.title_accounts)
                            MainTab.Library -> stringResource(R.string.title_library)
                            MainTab.Settings -> stringResource(R.string.title_settings)
                            MainTab.Info -> stringResource(R.string.title_info)
                        },
                    )
                },
                actions = {
                    if (currentTab == MainTab.Home) {
                        FilledTonalButton(
                            onClick = onAddAccount,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.action_add))
                        }
                    }
                },
                colors = topBarColors,
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                MainTab.entries.forEachIndexed { idx, tab ->
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        icon = { tab.icon() },
                        label = { Text(stringResource(tab.labelRes)) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        // Apply Scaffold insets to every tab; previously Home/Library ignored this and content drew under bars.
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            when (currentTab) {
                MainTab.Home -> {
                    AccountsScreen(
                        accountRepository = accountRepository,
                        onAddAccount = onAddAccount,
                        showAppBar = false,
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
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                        ) {
                            Text(stringResource(R.string.library_select_account_hint))
                        }
                    } else {
                        LibraryScreen(
                            accountId = accId,
                            db = db,
                            libraryRepository = libraryRepository,
                            downloadRepository = downloadRepository,
                            showAppBar = false,
                            onSaveFolderHref = { href -> onSaveFolderHref(accId, href) },
                            onOpenVideo = { videoId -> onOpenVideo(accId, videoId) },
                        )
                    }
                }
                MainTab.Settings -> SettingsScreen()
                MainTab.Info -> InfoScreen()
            }
        }
    }
}


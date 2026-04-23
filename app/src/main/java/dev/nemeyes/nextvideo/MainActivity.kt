package dev.nemeyes.nextvideo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.nemeyes.nextvideo.data.accounts.AccountRepository
import dev.nemeyes.nextvideo.ui.NavRoutes
import dev.nemeyes.nextvideo.ui.screens.AccountsScreen
import dev.nemeyes.nextvideo.ui.screens.AddAccountScreen
import dev.nemeyes.nextvideo.ui.screens.LibraryScreen
import dev.nemeyes.nextvideo.ui.screens.PlayerScreen
import dev.nemeyes.nextvideo.ui.theme.NextVideoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextVideoTheme {
                Surface(color = Color.Transparent) {
                    NextVideoApp()
                }
            }
        }
    }
}

@Composable
private fun NextVideoApp() {
    val nav = rememberNavController()
    val container = remember { AppContainer(nav.context.applicationContext) }
    val accountRepository = remember { AccountRepository(container.db.accountDao(), container.secrets) }

    NavHost(navController = nav, startDestination = NavRoutes.Accounts) {
        composable(NavRoutes.Accounts) {
            AccountsScreen(
                accountRepository = accountRepository,
                onAddAccount = { nav.navigate(NavRoutes.AddAccount) },
                onOpenAccount = { accountId -> nav.navigate(NavRoutes.library(accountId)) },
            )
        }

        composable(NavRoutes.AddAccount) {
            AddAccountScreen(
                loginV2Api = container.loginV2Api,
                accountRepository = accountRepository,
                onDone = { accountId -> nav.navigate(NavRoutes.library(accountId)) },
            )
        }

        composable(
            route = "${NavRoutes.Library}/{accountId}",
            arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
        ) { backStack ->
            val accountId = backStack.arguments?.getString("accountId").orEmpty()
            LibraryScreen(
                accountId = accountId,
                db = container.db,
                libraryRepository = container.libraryRepository,
                downloadRepository = container.downloadRepository,
                onSaveFolderHref = { href -> accountRepository.setLibraryFolderHref(accountId, href) },
                onOpenVideo = { videoId -> nav.navigate(NavRoutes.player(accountId, videoId)) },
            )
        }

        composable(
            route = "${NavRoutes.Player}/{accountId}/{videoId}",
            arguments =
                listOf(
                    navArgument("accountId") { type = NavType.StringType },
                    navArgument("videoId") { type = NavType.StringType },
                ),
        ) { backStack ->
            val accountId = backStack.arguments?.getString("accountId").orEmpty()
            val videoId = backStack.arguments?.getString("videoId").orEmpty()
            PlayerScreen(
                accountId = accountId,
                videoId = videoId,
                db = container.db,
                secrets = container.secrets,
            )
        }
    }
}


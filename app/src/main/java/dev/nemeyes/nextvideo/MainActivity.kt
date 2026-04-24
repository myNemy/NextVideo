package dev.nemeyes.nextvideo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.nemeyes.nextvideo.data.accounts.AccountRepository
import dev.nemeyes.nextvideo.ui.NavRoutes
import dev.nemeyes.nextvideo.ui.screens.AddAccountScreen
import dev.nemeyes.nextvideo.ui.screens.PlayerScreen
import dev.nemeyes.nextvideo.ui.main.MainTabsScreen
import dev.nemeyes.nextvideo.ui.theme.NextVideoTheme
import dev.nemeyes.nextvideo.nextcloud.theming.InstanceTheming

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextVideoApp()
        }
    }
}

@Composable
private fun NextVideoApp() {
    val nav = rememberNavController()
    val container = remember { AppContainer(nav.context.applicationContext) }
    val accountRepository =
        remember {
            AccountRepository(
                context = nav.context.applicationContext,
                db = container.db,
                secrets = container.secrets,
            )
        }
    var theming by remember { mutableStateOf<InstanceTheming?>(null) }

    NextVideoTheme(
        instancePrimaryHex = theming?.primaryHex,
        instanceOnPrimaryHex = theming?.onPrimaryHex,
    ) {
        Surface(color = Color.Transparent) {
            NavHost(navController = nav, startDestination = NavRoutes.MainTabs) {
                composable(
                    route = "${NavRoutes.MainTabs}?accountId={accountId}",
                    arguments =
                        listOf(
                            navArgument("accountId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                ) { backStack ->
                    val accountId = backStack.arguments?.getString("accountId")
                    MainTabsScreen(
                        accountRepository = accountRepository,
                        db = container.db,
                        libraryRepository = container.libraryRepository,
                        downloadRepository = container.downloadRepository,
                        initialAccountId = accountId,
                        instanceTheming = theming,
                        onAddAccount = { nav.navigate(NavRoutes.AddAccount) },
                        onSaveFolderHref = { id, href -> accountRepository.setLibraryFolderHref(id, href) },
                        onOpenVideo = { id, videoId -> nav.navigate(NavRoutes.player(id, videoId)) },
                        onThemingChanged = { theming = it },
                    )
                }

                composable(NavRoutes.AddAccount) {
                    AddAccountScreen(
                        loginV2Api = container.loginV2Api,
                        accountRepository = accountRepository,
                        onDone = { accountId ->
                            nav.navigate(NavRoutes.mainTabs(accountId)) {
                                popUpTo(NavRoutes.MainTabs) { inclusive = true }
                            }
                        },
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
                        onNavigateUp = { nav.popBackStack() },
                    )
                }
            }
        }
    }
}


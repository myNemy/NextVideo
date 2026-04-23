package dev.nemeyes.nextvideo.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nemeyes.nextvideo.R
import dev.nemeyes.nextvideo.data.accounts.AccountRepository
import dev.nemeyes.nextvideo.nextcloud.loginv2.LoginV2Api
import kotlinx.coroutines.launch

@Composable
fun AddAccountScreen(
    loginV2Api: LoginV2Api,
    accountRepository: AccountRepository,
    onDone: (accountId: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf("https://") }
    var status by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.title_add_account))
        TextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text(stringResource(R.string.field_server_base_url)) },
        )

        Button(
            enabled = !isBusy,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            onClick = {
                scope.launch {
                    try {
                        isBusy = true
                        status = context.getString(R.string.status_starting_login)
                        val start = loginV2Api.start(serverUrl)
                        status = context.getString(R.string.status_open_browser_approve)
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(start.loginUrl))
                        val result = loginV2Api.pollUntilApproved(start.pollUrl, start.token)
                        status = context.getString(R.string.status_saving_account)
                        val accountId =
                            accountRepository.addAccount(
                                serverBaseUrl = result.server,
                                loginName = result.loginName,
                                appPassword = result.appPassword,
                            )
                        status = context.getString(R.string.status_done)
                        onDone(accountId)
                    } catch (t: Throwable) {
                        status =
                            context.getString(
                                R.string.status_error_fmt,
                                t.message ?: t.javaClass.simpleName,
                            )
                    } finally {
                        isBusy = false
                    }
                }
            },
        ) {
            Text(stringResource(R.string.action_login))
        }

        Text(status)
    }
}


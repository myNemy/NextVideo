package dev.nemeyes.nextvideo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import dev.nemeyes.nextvideo.R
import dev.nemeyes.nextvideo.data.accounts.AccountRepository

@Composable
fun AccountsScreen(
    accountRepository: AccountRepository,
    onAddAccount: () -> Unit,
    onOpenAccount: (accountId: String) -> Unit,
) {
    val accounts by accountRepository.observeAll().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.title_accounts))
            Button(onClick = onAddAccount) { Text(stringResource(R.string.action_add)) }
        }

        if (accounts.isEmpty()) {
            Text(stringResource(R.string.empty_accounts))
        } else {
            accounts.forEach { acc ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAccount(acc.id) }
                            .padding(vertical = 10.dp),
                ) {
                    Text(acc.loginName)
                    Text(acc.serverBaseUrl)
                }
            }
        }
    }
}


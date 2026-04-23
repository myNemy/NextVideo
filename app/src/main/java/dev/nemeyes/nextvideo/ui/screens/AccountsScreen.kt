package dev.nemeyes.nextvideo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import dev.nemeyes.nextvideo.R
import dev.nemeyes.nextvideo.data.accounts.AccountRepository
import dev.nemeyes.nextvideo.ui.components.FastScrollBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accountRepository: AccountRepository,
    onAddAccount: () -> Unit,
    onOpenAccount: (accountId: String) -> Unit,
) {
    val accounts by accountRepository.observeAll().collectAsState(initial = emptyList())
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_accounts)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                actions = {
                    FilledTonalButton(
                        onClick = onAddAccount,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(stringResource(R.string.action_add))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (accounts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.empty_accounts), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(accounts.size) { idx ->
                        val acc = accounts[idx]
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenAccount(acc.id) }
                                    .padding(vertical = 10.dp),
                        ) {
                            Text(acc.loginName)
                            Text(acc.serverBaseUrl, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                FastScrollBar(
                    state = listState,
                    totalItems = accounts.size,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                )
            }
        }
    }
}


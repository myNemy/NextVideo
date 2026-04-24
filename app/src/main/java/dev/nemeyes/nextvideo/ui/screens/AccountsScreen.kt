package dev.nemeyes.nextvideo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import dev.nemeyes.nextvideo.R
import dev.nemeyes.nextvideo.data.accounts.AccountRepository
import dev.nemeyes.nextvideo.data.db.AccountEntity
import dev.nemeyes.nextvideo.ui.components.FastScrollBar
import dev.nemeyes.nextvideo.ui.theme.ncAppBarTopColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accountRepository: AccountRepository,
    onAddAccount: () -> Unit,
    onOpenAccount: (accountId: String) -> Unit,
    onAccountRemoved: (String) -> Unit = {},
    showAppBar: Boolean = true,
) {
    val accounts by accountRepository.observeAll().collectAsState(initial = emptyList())
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var accountMenuId by remember { mutableStateOf<String?>(null) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<AccountEntity?>(null) }

    val content: @Composable (Modifier) -> Unit = { rootMod ->
        if (accounts.isEmpty()) {
            Column(
                modifier = rootMod.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.empty_accounts),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Box(modifier = rootMod.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        val goAccount = { onOpenAccount(acc.id) }
                        ListItem(
                            modifier = Modifier.fillMaxWidth(),
                            headlineContent = {
                                Text(
                                    acc.loginName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable(onClick = goAccount),
                                )
                            },
                            supportingContent = {
                                Text(
                                    acc.serverBaseUrl,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable(onClick = goAccount),
                                )
                            },
                            trailingContent = {
                                Box {
                                    IconButton(
                                        onClick = { accountMenuId = if (accountMenuId == acc.id) null else acc.id },
                                    ) {
                                        Icon(
                                            Icons.Outlined.MoreVert,
                                            contentDescription = stringResource(R.string.content_desc_account_options),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = accountMenuId == acc.id,
                                        onDismissRequest = { accountMenuId = null },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.account_menu_edit)) },
                                            onClick = {
                                                accountMenuId = null
                                                editing = acc
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(R.string.account_menu_remove),
                                                    color = MaterialTheme.colorScheme.error,
                                                )
                                            },
                                            onClick = {
                                                accountMenuId = null
                                                deleteId = acc.id
                                            },
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
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

    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text(stringResource(R.string.account_delete_title)) },
            text = { Text(stringResource(R.string.account_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toRemove = id
                        deleteId = null
                        scope.launch {
                            runCatching { accountRepository.removeAccount(toRemove) }
                                .onSuccess { onAccountRemoved(toRemove) }
                        }
                    },
                ) { Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    editing?.let { acc ->
        var href by remember(acc.id) { mutableStateOf(acc.libraryFolderHref) }
        var newPassword by remember(acc.id) { mutableStateOf("") }
        val scroll = rememberScrollState()
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.account_edit_title)) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.account_edit_server),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        acc.serverBaseUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.account_edit_login),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        acc.loginName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextField(
                        value = href,
                        onValueChange = { href = it },
                        label = { Text(stringResource(R.string.field_library_folder_href)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.field_new_app_password_optional)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val accId = acc.id
                        val pwd = newPassword.trim()
                        editing = null
                        scope.launch {
                            runCatching {
                                accountRepository.setLibraryFolderHref(accId, href)
                            }
                            if (pwd.isNotEmpty()) {
                                runCatching { accountRepository.setAppPassword(accId, pwd) }
                            }
                        }
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (!showAppBar) {
        Box(modifier = Modifier.fillMaxSize()) { content(Modifier) }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_accounts)) },
                colors = ncAppBarTopColors(),
                actions = {
                    TextButton(onClick = onAddAccount) {
                        Text(
                            stringResource(R.string.action_add),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) { content(Modifier) }
    }
}

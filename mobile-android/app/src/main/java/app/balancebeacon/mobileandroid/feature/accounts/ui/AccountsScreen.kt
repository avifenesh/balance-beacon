package app.balancebeacon.mobileandroid.feature.accounts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.ui.components.SkeletonListScreen
import app.balancebeacon.mobileandroid.ui.util.sanitizeError
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current
    var showDeleteConfirmId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    if (state.isLoading && state.items.isEmpty()) {
        SkeletonListScreen(modifier = modifier)
        return
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            viewModel.load()
        },
        modifier = modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.createName,
                    onValueChange = viewModel::onCreateNameChanged,
                    label = { Text("New account name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isMutating
                )

                Text("Type", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SELF", "PARTNER", "OTHER").forEach { accountType ->
                        FilterChip(
                            selected = state.createType.equals(accountType, ignoreCase = true),
                            onClick = { viewModel.onCreateTypeChanged(accountType) },
                            label = { Text(accountType) },
                            enabled = !state.isMutating
                        )
                    }
                }

                OutlinedTextField(
                    value = state.createPreferredCurrency,
                    onValueChange = viewModel::onCreatePreferredCurrencyChanged,
                    label = { Text("Currency (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isMutating
                )

                OutlinedTextField(
                    value = state.createColor,
                    onValueChange = viewModel::onCreateColorChanged,
                    label = { Text("Color hex (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isMutating
                )

                Button(
                    onClick = viewModel::createAccount,
                    enabled = !state.isMutating
                ) {
                    Text(if (state.isMutating) "Working..." else "Create account")
                }
            }
        }

        if (state.editingAccountId != null) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Edit account", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = state.editName,
                        onValueChange = viewModel::onEditNameChanged,
                        label = { Text("Edit name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isMutating
                    )

                    Text("Type", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SELF", "PARTNER", "OTHER").forEach { accountType ->
                            FilterChip(
                                selected = state.editType.equals(accountType, ignoreCase = true),
                                onClick = { viewModel.onEditTypeChanged(accountType) },
                                label = { Text(accountType) },
                                enabled = !state.isMutating
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.editPreferredCurrency,
                        onValueChange = viewModel::onEditPreferredCurrencyChanged,
                        label = { Text("Currency") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isMutating
                    )

                    OutlinedTextField(
                        value = state.editColor,
                        onValueChange = viewModel::onEditColorChanged,
                        label = { Text("Color hex") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isMutating
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::updateAccount,
                            enabled = !state.isMutating
                        ) {
                            Text("Save")
                        }
                        TextButton(
                            onClick = viewModel::cancelEditing,
                            enabled = !state.isMutating
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        state.statusMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        state.error?.let {
            Text(sanitizeError(it), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text("Existing accounts", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = state.items, key = { it.id }) { account ->
                AccountItem(
                    account = account,
                    isMutating = state.isMutating,
                    onActivate = viewModel::activateAccount,
                    onEdit = viewModel::startEditing,
                    onDelete = { showDeleteConfirmId = it }
                )
            }
        }
    }
    } // PullToRefreshBox

    showDeleteConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmId = null },
            title = { Text("Delete account") },
            text = { Text("Are you sure you want to delete this account? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.deleteAccount(id)
                    showDeleteConfirmId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AccountItem(
    account: AccountDto,
    isMutating: Boolean,
    onActivate: (String) -> Unit,
    onEdit: (AccountDto) -> Unit,
    onDelete: (String) -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Type: ${account.type} | Balance: ${account.balance}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Currency: ${account.preferredCurrency ?: "-"} | Color: ${account.color ?: "-"}",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onActivate(account.id) },
                    enabled = !isMutating
                ) {
                    Text("Activate")
                }
                TextButton(
                    onClick = { onEdit(account) },
                    enabled = !isMutating
                ) {
                    Text("Edit")
                }
                TextButton(
                    onClick = { onDelete(account.id) },
                    enabled = !isMutating
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

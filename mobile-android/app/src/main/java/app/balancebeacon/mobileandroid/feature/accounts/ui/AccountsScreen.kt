package app.balancebeacon.mobileandroid.feature.accounts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    if (state.isLoading && state.items.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Accounts", style = MaterialTheme.typography.headlineSmall)

                OutlinedTextField(
                    value = state.createName,
                    onValueChange = viewModel::onCreateNameChanged,
                    label = { Text("New account name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isMutating
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.createType,
                        onValueChange = viewModel::onCreateTypeChanged,
                        label = { Text("Type (SELF/PARTNER/OTHER)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !state.isMutating
                    )
                    OutlinedTextField(
                        value = state.createPreferredCurrency,
                        onValueChange = viewModel::onCreatePreferredCurrencyChanged,
                        label = { Text("Currency (optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !state.isMutating
                    )
                }

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.editType,
                            onValueChange = viewModel::onEditTypeChanged,
                            label = { Text("Type") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !state.isMutating
                        )
                        OutlinedTextField(
                            value = state.editPreferredCurrency,
                            onValueChange = viewModel::onEditPreferredCurrencyChanged,
                            label = { Text("Currency") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !state.isMutating
                        )
                    }

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
            Text(it, color = MaterialTheme.colorScheme.error)
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
                    onDelete = viewModel::deleteAccount
                )
            }
        }
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

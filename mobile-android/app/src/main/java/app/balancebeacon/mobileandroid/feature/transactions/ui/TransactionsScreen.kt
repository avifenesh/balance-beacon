package app.balancebeacon.mobileandroid.feature.transactions.ui

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardTransactionRequestDto
import app.balancebeacon.mobileandroid.feature.transactions.model.CreateTransactionRequest
import app.balancebeacon.mobileandroid.feature.transactions.model.UpdateTransactionRequest
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onShareTransaction: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var accountId by rememberSaveable { mutableStateOf("") }
    var monthKey by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var selectedTransactionId by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var listTypeFilter by rememberSaveable { mutableStateOf("ALL") }
    var listAccountFilter by rememberSaveable { mutableStateOf("") }

    val canSubmit = accountId.isNotBlank() && amount.isNotBlank() && type.isNotBlank() && date.isNotBlank()
    val filteredItems = remember(state.items, searchQuery, listTypeFilter, listAccountFilter) {
        val normalizedQuery = searchQuery.trim().lowercase()
        val normalizedTypeFilter = listTypeFilter.trim().uppercase()
        val normalizedAccountFilter = listAccountFilter.trim().lowercase()

        state.items.filter { tx ->
            val matchesType = normalizedTypeFilter.isBlank() ||
                normalizedTypeFilter == "ALL" ||
                tx.type.equals(normalizedTypeFilter, ignoreCase = true)
            val matchesAccount = normalizedAccountFilter.isBlank() ||
                tx.accountId.lowercase().contains(normalizedAccountFilter)
            val searchableText = buildString {
                append(tx.type)
                append(' ')
                append(tx.amount)
                append(' ')
                append(tx.accountId)
                append(' ')
                append(tx.date)
                tx.description?.let {
                    append(' ')
                    append(it)
                }
                tx.categoryId?.let {
                    append(' ')
                    append(it)
                }
            }.lowercase()
            val matchesQuery = normalizedQuery.isBlank() || searchableText.contains(normalizedQuery)
            matchesType && matchesAccount && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Transactions", style = MaterialTheme.typography.headlineSmall)
                if (state.isLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text("Syncing...")
                    }
                }
                state.statusMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = accountId,
                    onValueChange = { accountId = it },
                    label = { Text("Account ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthKey,
                    onValueChange = { monthKey = it },
                    label = { Text("Month Key (YYYY-MM, optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoryId,
                    onValueChange = { categoryId = it },
                    label = { Text("Category ID (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search list (amount/desc/date/category)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = listTypeFilter,
                    onValueChange = { listTypeFilter = it.uppercase() },
                    label = { Text("List type filter (ALL/INCOME/EXPENSE)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = listAccountFilter,
                    onValueChange = { listAccountFilter = it },
                    label = { Text("List account filter (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.load(
                                accountId = accountId.ifBlank { null },
                                month = monthKey.ifBlank { null }
                            )
                        }
                    ) {
                        Text("Refresh")
                    }
                    Button(
                        enabled = canSubmit,
                        onClick = {
                            viewModel.createTransaction(
                                request = CreateTransactionRequest(
                                    accountId = accountId,
                                    amount = amount,
                                    type = type,
                                    date = date,
                                    description = description.ifBlank { null },
                                    categoryId = categoryId.ifBlank { null }
                                )
                            )
                        }
                    ) {
                        Text("Create")
                    }
                    Button(
                        enabled = canSubmit && selectedTransactionId != null,
                        onClick = {
                            selectedTransactionId?.let { id ->
                                viewModel.updateTransaction(
                                    id = id,
                                    request = UpdateTransactionRequest(
                                        accountId = accountId,
                                        amount = amount,
                                        type = type,
                                        date = date,
                                        description = description.ifBlank { null },
                                        categoryId = categoryId.ifBlank { null }
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Update")
                    }
                }

                Text(
                    text = "Selected: ${selectedTransactionId ?: "none"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Showing ${filteredItems.size} of ${state.items.size} transactions",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        RequestsSection(
            requests = state.requestItems,
            requestActionInProgressId = state.requestActionInProgressId,
            requestActionMessage = state.requestActionMessage,
            requestActionError = state.requestActionError,
            isLoading = state.isLoading,
            onApprove = viewModel::approveTransactionRequest,
            onReject = viewModel::rejectTransactionRequest,
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredItems, key = { it.id }) { tx ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("${tx.type}: ${tx.amount}")
                        Text("Date: ${tx.date}", style = MaterialTheme.typography.bodySmall)
                        Text("Account: ${tx.accountId}", style = MaterialTheme.typography.bodySmall)
                        tx.description?.let { Text("Desc: $it", style = MaterialTheme.typography.bodySmall) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    selectedTransactionId = tx.id
                                    accountId = tx.accountId
                                    amount = tx.amount
                                    type = tx.type
                                    date = tx.date
                                    description = tx.description.orEmpty()
                                    categoryId = tx.categoryId.orEmpty()
                                }
                            ) {
                                Text("Edit")
                            }
                            Button(
                                onClick = {
                                    viewModel.deleteTransaction(tx.id)
                                    if (selectedTransactionId == tx.id) {
                                        selectedTransactionId = null
                                    }
                                }
                            ) {
                                Text("Delete")
                            }
                            if (tx.type.equals("EXPENSE", ignoreCase = true) && onShareTransaction != null) {
                                OutlinedButton(
                                    onClick = { onShareTransaction(tx.id) }
                                ) {
                                    Text("Share")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestsSection(
    requests: List<DashboardTransactionRequestDto>,
    requestActionInProgressId: String?,
    requestActionMessage: String?,
    requestActionError: String?,
    isLoading: Boolean,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Pending Requests", style = MaterialTheme.typography.titleMedium)
            requestActionMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            requestActionError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            val actionsEnabled = !isLoading && requestActionInProgressId == null
            if (requests.isEmpty()) {
                Text("No pending requests")
            } else {
                requests.take(5).forEach { request ->
                    val fromName = request.from?.name
                        ?: request.from?.email
                        ?: "Unknown sender"
                    val category = request.category?.name ?: "Uncategorized"
                    val isRowLoading = requestActionInProgressId == request.id
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "$fromName • ${request.amount} ${request.currency} • $category • ${request.date}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        request.description?.takeIf { it.isNotBlank() }?.let { description ->
                            Text(
                                description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onApprove(request.id) },
                                enabled = actionsEnabled
                            ) {
                                Text(if (isRowLoading) "Working..." else "Approve")
                            }
                            OutlinedButton(
                                onClick = { onReject(request.id) },
                                enabled = actionsEnabled
                            ) {
                                Text(if (isRowLoading) "Working..." else "Reject")
                            }
                        }
                    }
                }
            }
        }
    }
}

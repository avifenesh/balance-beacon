package app.balancebeacon.mobileandroid.feature.transactions.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.ui.components.SkeletonLine
import app.balancebeacon.mobileandroid.ui.theme.SkyBlue
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardTransactionRequestDto
import app.balancebeacon.mobileandroid.feature.transactions.model.CreateTransactionRequest
import app.balancebeacon.mobileandroid.feature.transactions.model.UpdateTransactionRequest
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onShareTransaction: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current
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
    var showDeleteConfirmId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    var hasInteracted by rememberSaveable { mutableStateOf(false) }
    val canSubmit = accountId.isNotBlank() && amount.isNotBlank() && type.isNotBlank() && date.isNotBlank()
    val filteredItems = remember(state.items, searchQuery, listTypeFilter, listAccountFilter) {
        filterTransactions(
            items = state.items,
            searchQuery = searchQuery,
            listTypeFilter = listTypeFilter,
            listAccountFilter = listAccountFilter
        )
    }

    val accountNames = remember(state.accounts) { state.accounts.associate { it.id to it.name } }
    val categoryNames = remember(state.categories) { state.categories.associate { it.id to it.name } }
    val categoryColors = remember(state.categories) { state.categories.associate { it.id to it.color } }
    val selectedAccountName = accountNames[accountId] ?: "Select account"
    val selectedCategoryName = categoryNames[categoryId] ?: "Select category"
    val categoryOptions = remember(state.categories, type) {
        val normalizedType = type.trim().uppercase()
        if (normalizedType.isBlank() || normalizedType == "ALL") {
            state.categories.filter { !it.isArchived && !it.isHolding }
        } else {
            state.categories.filter {
                !it.isArchived && !it.isHolding && it.type.equals(normalizedType, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // Auto-select first account if available
    LaunchedEffect(state.accounts) {
        if (accountId.isBlank() && state.accounts.isNotEmpty()) {
            accountId = state.accounts.first().id
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            viewModel.initialize()
        },
        modifier = modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section 1: Create Transaction
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Create Transaction", style = MaterialTheme.typography.titleMedium)

                    if (state.isLoading) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkeletonLine(width = 180.dp, height = 14.dp)
                            SkeletonLine(width = 120.dp, height = 14.dp)
                        }
                    }
                    state.statusMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }
                    if (hasInteracted) {
                        state.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Text("Account", style = MaterialTheme.typography.labelLarge)
                    AccountSelectorRow(
                        accountNames = state.accounts.map { it.id to it.name },
                        selectedAccountId = accountId,
                        onSelect = { accountId = it }
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
                    Text("Type", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("EXPENSE", "INCOME").forEach { txType ->
                            FilterChip(
                                selected = type.equals(txType, ignoreCase = true),
                                onClick = { type = txType },
                                label = { Text(txType) }
                            )
                        }
                    }
                    Text("Date", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(date.ifBlank { "Select date" })
                    }

                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    CategorySelector(
                        selectedCategoryName = if (categoryId.isBlank()) "Select category (optional)" else selectedCategoryName,
                        categories = categoryOptions,
                        selectedCategoryId = categoryId,
                        onSelectCategory = { categoryId = it }
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = canSubmit,
                            onClick = {
                                hasInteracted = true
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
                                hasInteracted = true
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
                }
            }
        }

        // Section 2: Search
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Search", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search (amount/desc/date/category)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Type filter", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL", "EXPENSE", "INCOME").forEach { filterType ->
                            FilterChip(
                                selected = listTypeFilter.equals(filterType, ignoreCase = true),
                                onClick = { listTypeFilter = filterType },
                                label = { Text(filterType) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = listAccountFilter,
                        onValueChange = { listAccountFilter = it },
                        label = { Text("Account filter (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            hasInteracted = true
                            viewModel.load(
                                accountId = accountId.ifBlank { null },
                                month = monthKey.ifBlank { null }
                            )
                        }
                    ) {
                        Text("Load Transactions")
                    }

                    Text(
                        text = "Showing ${filteredItems.size} of ${state.items.size} transactions",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Pending Requests
        item {
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
        }

        // Section 3: Transaction List
        items(filteredItems, key = { it.id }) { tx ->
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("${tx.type}: ${tx.amount}")
                    Text("Date: ${tx.date}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Account: ${accountNames[tx.accountId] ?: tx.accountId}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    tx.categoryId?.let { catId ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = parseHexColor(categoryColors[catId]),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                "Category: ${categoryNames[catId] ?: catId}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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
                            onClick = { showDeleteConfirmId = tx.id }
                        ) {
                            Text("Delete")
                        }
                        if (isShareEligible(tx.type, onShareTransaction != null)) {
                            OutlinedButton(
                                onClick = { onShareTransaction?.invoke(tx.id) }
                            ) {
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }
    }
    } // PullToRefreshBox

    showDeleteConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmId = null },
            title = { Text("Delete transaction") },
            text = { Text("Are you sure you want to delete this transaction? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.deleteTransaction(id)
                    if (selectedTransactionId == id) {
                        selectedTransactionId = null
                    }
                    showDeleteConfirmId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmId = null }) { Text("Cancel") }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        formatter.timeZone = TimeZone.getTimeZone("UTC")
                        date = formatter.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AccountSelectorRow(
    accountNames: List<Pair<String, String>>,
    selectedAccountId: String,
    onSelect: (String) -> Unit
) {
    if (accountNames.isEmpty()) {
        Text(
            text = "No accounts available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accountNames.forEach { (id, name) ->
            FilterChip(
                selected = selectedAccountId == id,
                onClick = { onSelect(id) },
                label = { Text(name) }
            )
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategoryName: String,
    categories: List<CategoryDto>,
    selectedCategoryId: String,
    onSelectCategory: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = categories.isNotEmpty()
        ) {
            Text(selectedCategoryName)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        val suffix = if (selectedCategoryId == category.id) " selected" else ""
                        Text("${category.name}$suffix")
                    },
                    onClick = {
                        expanded = false
                        onSelectCategory(category.id)
                    }
                )
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

private fun parseHexColor(hex: String?, default: Color = SkyBlue): Color {
    if (hex.isNullOrBlank()) return default
    return try {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    } catch (_: Exception) {
        default
    }
}

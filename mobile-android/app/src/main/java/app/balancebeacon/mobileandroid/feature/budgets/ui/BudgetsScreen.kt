package app.balancebeacon.mobileandroid.feature.budgets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.budgets.model.CreateBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.QuickBudgetRequest
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import kotlin.math.max
import kotlin.math.min

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var accountId by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var monthKey by rememberSaveable { mutableStateOf("") }
    var planned by rememberSaveable { mutableStateOf("") }
    var currencyCode by rememberSaveable { mutableStateOf("USD") }
    var notes by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("ALL") }
    var incomeGoalAmount by rememberSaveable { mutableStateOf("") }
    var incomeGoalCurrency by rememberSaveable { mutableStateOf("USD") }
    var setAsDefaultGoal by rememberSaveable { mutableStateOf(false) }

    val canSubmit = accountId.isNotBlank() &&
        categoryId.isNotBlank() &&
        monthKey.isNotBlank() &&
        planned.isNotBlank()

    val canSaveIncomeGoal = accountId.isNotBlank() &&
        MONTH_KEY_REGEX.matches(monthKey.trim()) &&
        (incomeGoalAmount.toDoubleOrNull()?.let { it > 0.0 } == true)
    val canDeleteIncomeGoal = (state.incomeGoal?.isDefault == false) &&
        MONTH_KEY_REGEX.matches(monthKey.trim())

    val filteredItems = remember(state.items, searchQuery, typeFilter) {
        val normalizedQuery = searchQuery.trim().lowercase()
        val normalizedTypeFilter = typeFilter.trim().uppercase()
        state.items.filter { budget ->
            val categoryType = budget.category?.type?.uppercase()
            val matchesType = normalizedTypeFilter.isBlank() ||
                normalizedTypeFilter == "ALL" ||
                normalizedTypeFilter == categoryType
            val searchableText = buildString {
                append(budget.accountId)
                append(' ')
                append(budget.categoryId)
                append(' ')
                append(budget.category?.name.orEmpty())
                append(' ')
                append(budget.monthKey)
                append(' ')
                append(budget.amount)
                append(' ')
                append(budget.notes.orEmpty())
            }.lowercase()
            val matchesQuery = normalizedQuery.isBlank() || searchableText.contains(normalizedQuery)
            matchesType && matchesQuery
        }
    }

    val plannedExpenseTotal = remember(filteredItems) {
        filteredItems
            .filter { it.category?.type.equals("EXPENSE", ignoreCase = true) }
            .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }
    val plannedIncomeTotal = remember(filteredItems) {
        filteredItems
            .filter { it.category?.type.equals("INCOME", ignoreCase = true) }
            .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }
    val incomeGoalValue = state.incomeGoal?.amount?.toDoubleOrNull() ?: 0.0
    val actualIncomeValue = state.actualIncome.toDoubleOrNull() ?: 0.0
    val incomeProgress = if (incomeGoalValue > 0.0) {
        min((actualIncomeValue / incomeGoalValue) * 100.0, 100.0)
    } else {
        0.0
    }
    val incomeRemaining = max(incomeGoalValue - actualIncomeValue, 0.0)

    LaunchedEffect(Unit) {
        viewModel.load(
            accountId = accountId.ifBlank { null },
            month = monthKey.ifBlank { null }
        )
    }

    LaunchedEffect(state.selectedAccountId) {
        if (accountId.isBlank() && state.selectedAccountId.isNotBlank()) {
            accountId = state.selectedAccountId
        }
    }

    LaunchedEffect(state.selectedMonthKey) {
        if (monthKey.isBlank() && state.selectedMonthKey.isNotBlank()) {
            monthKey = state.selectedMonthKey
        }
    }

    LaunchedEffect(state.incomeGoal) {
        val goal = state.incomeGoal
        if (goal != null) {
            incomeGoalAmount = goal.amount
            incomeGoalCurrency = goal.currency
            setAsDefaultGoal = goal.isDefault
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
                Text("Budgets", style = MaterialTheme.typography.headlineSmall)
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
                    onValueChange = {
                        accountId = it
                        viewModel.onAccountIdChanged(it)
                    },
                    label = { Text("Account ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoryId,
                    onValueChange = { categoryId = it },
                    label = { Text("Category ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthKey,
                    onValueChange = { monthKey = it },
                    label = { Text("Month Key (YYYY-MM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = planned,
                    onValueChange = { planned = it },
                    label = { Text("Planned Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = currencyCode,
                    onValueChange = { currencyCode = it.uppercase() },
                    label = { Text("Currency (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search budgets") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = typeFilter,
                    onValueChange = { typeFilter = it.uppercase() },
                    label = { Text("Type filter (ALL/EXPENSE/INCOME)") },
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
                            viewModel.createBudget(
                                request = CreateBudgetRequest(
                                    accountId = accountId,
                                    categoryId = categoryId,
                                    monthKey = monthKey,
                                    planned = planned,
                                    currencyCode = currencyCode.ifBlank { null },
                                    notes = notes.ifBlank { null }
                                )
                            )
                        }
                    ) {
                        Text("Create")
                    }
                    Button(
                        enabled = canSubmit,
                        onClick = {
                            viewModel.createQuickBudget(
                                request = QuickBudgetRequest(
                                    accountId = accountId,
                                    categoryId = categoryId,
                                    monthKey = monthKey,
                                    planned = planned,
                                    currencyCode = currencyCode.ifBlank { null }
                                )
                            )
                        }
                    ) {
                        Text("Quick")
                    }
                }
                Text(
                    text = "Expense planned: ${formatAmount(plannedExpenseTotal)} • Income planned: ${formatAmount(plannedIncomeTotal)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Monthly Income Goal", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Actual: ${state.actualIncome} • Goal: ${state.incomeGoal?.amount ?: "not set"}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (incomeGoalValue > 0.0) {
                    LinearProgressIndicator(
                        progress = { (incomeProgress / 100.0).toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${incomeProgress.toInt()}% complete • Remaining ${formatAmount(incomeRemaining)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = incomeGoalAmount,
                    onValueChange = { incomeGoalAmount = it },
                    label = { Text("Income goal amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = incomeGoalCurrency,
                    onValueChange = { incomeGoalCurrency = it.uppercase() },
                    label = { Text("Income goal currency") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = setAsDefaultGoal,
                        onCheckedChange = { setAsDefaultGoal = it }
                    )
                    Text("Set as default for future months")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = canSaveIncomeGoal,
                        onClick = {
                            viewModel.upsertIncomeGoal(
                                accountId = accountId,
                                monthKey = monthKey,
                                amount = incomeGoalAmount,
                                currency = incomeGoalCurrency,
                                setAsDefault = setAsDefaultGoal
                            )
                        }
                    ) {
                        Text("Save Goal")
                    }
                    OutlinedButton(
                        enabled = canDeleteIncomeGoal,
                        onClick = {
                            viewModel.deleteIncomeGoal(
                                accountId = accountId,
                                monthKey = monthKey
                            )
                        }
                    ) {
                        Text("Delete Goal")
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredItems.isEmpty()) {
                item {
                    Text("No matching budgets loaded.")
                }
            }
            items(
                items = filteredItems,
                key = { item -> "${item.accountId}:${item.categoryId}:${item.monthKey}" }
            ) { item ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val categoryLabel = item.category?.name ?: item.categoryId
                        val categoryType = item.category?.type ?: "UNKNOWN"
                        Text(
                            text = "${item.monthKey} • ${item.amount} ${item.currencyCode ?: "USD"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text("Account: ${item.accountId}", style = MaterialTheme.typography.bodySmall)
                        Text("Category: $categoryLabel ($categoryType)", style = MaterialTheme.typography.bodySmall)
                        item.notes?.let {
                            Text("Notes: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                viewModel.deleteBudget(
                                    accountId = item.accountId,
                                    categoryId = item.categoryId,
                                    monthKey = item.monthKey
                                )
                            }
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

private fun formatAmount(value: Double): String {
    return "%.2f".format(value)
}

private val MONTH_KEY_REGEX = Regex("^\\d{4}-\\d{2}$")

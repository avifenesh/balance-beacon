package app.balancebeacon.mobileandroid.feature.budgets.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.budgets.model.CreateBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.QuickBudgetRequest
import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.ui.components.MonthNavigator
import app.balancebeacon.mobileandroid.ui.components.SkeletonLine
import androidx.compose.foundation.shape.RoundedCornerShape
import app.balancebeacon.mobileandroid.ui.theme.Emerald400
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import app.balancebeacon.mobileandroid.ui.theme.GlassSurface
import app.balancebeacon.mobileandroid.ui.theme.Rose400
import app.balancebeacon.mobileandroid.ui.theme.SkyBlue
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current
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
    var hasInteracted by rememberSaveable { mutableStateOf(false) }
    var showDeleteBudgetKey by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showDeleteIncomeGoal by rememberSaveable { mutableStateOf(false) }
    var showFormSheet by rememberSaveable { mutableStateOf(false) }

    val canSubmit = accountId.isNotBlank() &&
        categoryId.isNotBlank() &&
        monthKey.isNotBlank() &&
        planned.isNotBlank()

    val canSaveIncomeGoal = accountId.isNotBlank() &&
        MONTH_KEY_REGEX.matches(monthKey.trim()) &&
        (incomeGoalAmount.toDoubleOrNull()?.let { it > 0.0 } == true)
    val canDeleteIncomeGoal = (state.incomeGoal?.isDefault == false) &&
        MONTH_KEY_REGEX.matches(monthKey.trim())

    val accountNames = remember(state.accounts) { state.accounts.associate { it.id to it.name } }
    val categoryNames = remember(state.categories) { state.categories.associate { it.id to it.name } }
    val selectedAccountName = accountNames[accountId] ?: "Select account"
    val selectedCategoryName = categoryNames[categoryId] ?: "Select category"

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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    LaunchedEffect(state.selectedAccountId) {
        if (accountId.isBlank() && state.selectedAccountId.isNotBlank()) {
            accountId = state.selectedAccountId
        }
    }

    LaunchedEffect(state.selectedMonthKey) {
        if (state.selectedMonthKey.isNotBlank()) {
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

    // Auto-load when account is set via initialize
    LaunchedEffect(state.accounts) {
        if (accountId.isBlank() && state.accounts.isNotEmpty()) {
            accountId = state.accounts.first().id
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                viewModel.load()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Search", style = MaterialTheme.typography.titleMedium)

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search budgets") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ALL", "EXPENSE", "INCOME").forEach { option ->
                                FilterChip(
                                    selected = typeFilter == option,
                                    onClick = { typeFilter = option },
                                    label = { Text(option) }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                hasInteracted = true
                                viewModel.load(
                                    accountId = accountId.ifBlank { null },
                                    month = monthKey.ifBlank { null }
                                )
                            }
                        ) {
                            Text("Refresh")
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
                                    hasInteracted = true
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
                                onClick = { showDeleteIncomeGoal = true }
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
                                Text(
                                    "Account: ${accountNames[item.accountId] ?: item.accountId}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = parseHexColor(item.category?.color),
                                                shape = CircleShape
                                            )
                                    )
                                    Text("Category: $categoryLabel ($categoryType)", style = MaterialTheme.typography.bodySmall)
                                }
                                item.notes?.let {
                                    Text("Notes: $it", style = MaterialTheme.typography.bodySmall)
                                }
                                // Budget progress bar with animation
                                run {
                                    val planned = item.amount.toDoubleOrNull() ?: 0.0
                                    val spent = item.spent?.toDoubleOrNull() ?: 0.0
                                    val rawProgress = if (planned > 0.0) {
                                        (spent / planned).toFloat().coerceIn(0f, 1.5f)
                                    } else {
                                        0f
                                    }
                                    val animatedProgress by animateFloatAsState(
                                        targetValue = rawProgress,
                                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                        label = "budget_progress"
                                    )
                                    val progressColor = when {
                                        animatedProgress > 1.0f -> Rose400
                                        animatedProgress > 0.75f -> Color(0xFFF59E0B)
                                        else -> Emerald400
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { animatedProgress.coerceAtMost(1f) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = progressColor,
                                            trackColor = GlassSurface
                                        )
                                        Text(
                                            text = "${(animatedProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = progressColor
                                        )
                                        AnimatedVisibility(
                                            visible = rawProgress < 0.5f,
                                            enter = fadeIn() + scaleIn()
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Emerald400.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp), tint = Emerald400)
                                                    Text("On track", style = MaterialTheme.typography.labelSmall, color = Emerald400)
                                                }
                                            }
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        showDeleteBudgetKey = Triple(item.accountId, item.categoryId, item.monthKey)
                                    }
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }

                showDeleteBudgetKey?.let { (acctId, catId, month) ->
                    AlertDialog(
                        onDismissRequest = { showDeleteBudgetKey = null },
                        title = { Text("Delete budget") },
                        text = { Text("Are you sure you want to delete this budget? This cannot be undone.") },
                        confirmButton = {
                            TextButton(onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                hasInteracted = true
                                viewModel.deleteBudget(
                                    accountId = acctId,
                                    categoryId = catId,
                                    monthKey = month
                                )
                                showDeleteBudgetKey = null
                            }) { Text("Delete") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteBudgetKey = null }) { Text("Cancel") }
                        }
                    )
                }

                if (showDeleteIncomeGoal) {
                    AlertDialog(
                        onDismissRequest = { showDeleteIncomeGoal = false },
                        title = { Text("Delete income goal") },
                        text = { Text("Are you sure you want to delete this income goal? This cannot be undone.") },
                        confirmButton = {
                            TextButton(onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                hasInteracted = true
                                viewModel.deleteIncomeGoal(
                                    accountId = accountId,
                                    monthKey = monthKey
                                )
                                showDeleteIncomeGoal = false
                            }) { Text("Delete") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteIncomeGoal = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        } // PullToRefreshBox

        FloatingActionButton(
            onClick = {
                categoryId = ""
                planned = ""
                currencyCode = "USD"
                notes = ""
                showFormSheet = true
            },
            containerColor = SkyBlue,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
        }
    } // Box

    if (showFormSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFormSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E293B),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "New Budget",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Text("Account", style = MaterialTheme.typography.labelLarge, color = Color.White)
                AccountSelectorRow(
                    accountNames = state.accounts.map { it.id to it.name },
                    selectedAccountId = accountId,
                    onSelect = {
                        accountId = it
                        viewModel.onAccountIdChanged(it)
                    }
                )

                Text("Category", style = MaterialTheme.typography.labelLarge, color = Color.White)
                CategorySelector(
                    selectedCategoryName = selectedCategoryName,
                    categories = state.categories,
                    selectedCategoryId = categoryId,
                    onSelectCategory = { categoryId = it }
                )

                MonthNavigator(
                    monthKey = monthKey.ifBlank { state.selectedMonthKey },
                    onPreviousMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth,
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = canSubmit,
                        onClick = {
                            hasInteracted = true
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
                            showFormSheet = false
                        }
                    ) {
                        Text("Create")
                    }
                    OutlinedButton(
                        enabled = canSubmit,
                        onClick = {
                            hasInteracted = true
                            viewModel.createQuickBudget(
                                request = QuickBudgetRequest(
                                    accountId = accountId,
                                    categoryId = categoryId,
                                    monthKey = monthKey,
                                    planned = planned,
                                    currencyCode = currencyCode.ifBlank { null }
                                )
                            )
                            showFormSheet = false
                        }
                    ) {
                        Text("Quick")
                    }
                }
            }
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

private fun formatAmount(value: Double): String {
    return "%.2f".format(value)
}

private val MONTH_KEY_REGEX = Regex("^\\d{4}-\\d{2}$")

private fun parseHexColor(hex: String?, default: Color = SkyBlue): Color {
    if (hex.isNullOrBlank()) return default
    return try {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    } catch (_: Exception) {
        default
    }
}

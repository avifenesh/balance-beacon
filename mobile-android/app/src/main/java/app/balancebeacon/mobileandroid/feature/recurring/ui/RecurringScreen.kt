package app.balancebeacon.mobileandroid.feature.recurring.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringTemplateDto
import app.balancebeacon.mobileandroid.ui.components.SkeletonLine
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import app.balancebeacon.mobileandroid.ui.theme.SkyBlue
import app.balancebeacon.mobileandroid.ui.util.sanitizeError
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current
    var showDeleteConfirmId by rememberSaveable { mutableStateOf<String?>(null) }
    val filteredTemplates = remember(state.templates, state.typeFilter, state.showInactiveTemplates) {
        filterRecurringTemplates(
            templates = state.templates,
            typeFilter = state.typeFilter,
            includeInactive = state.showInactiveTemplates
        )
    }
    val activeTemplates = remember(filteredTemplates) { filteredTemplates.filter { it.isActive } }
    val pausedTemplates = remember(filteredTemplates) { filteredTemplates.filterNot { it.isActive } }
    val summary = remember(filteredTemplates) { buildRecurringSummary(filteredTemplates) }
    val categoryOptions = remember(state.categories, state.formType) {
        filterRecurringCategories(categories = state.categories, type = state.formType)
    }
    val accountNames = remember(state.accounts) { state.accounts.associate { it.id to it.name } }
    val categoryNames = remember(state.categories) { state.categories.associate { it.id to it.name } }
    val selectedAccountName = accountNames[state.accountId] ?: "Select account"
    val selectedCurrencyCode = state.accounts
        .firstOrNull { it.id == state.accountId }
        ?.preferredCurrency
        ?.takeIf { it.isNotBlank() }
        ?: "USD"
    val selectedCategoryName = categoryOptions.firstOrNull { it.id == state.formCategoryId }?.name
        ?: categoryNames[state.formCategoryId]
        ?: "Select category"

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            viewModel.load()
        },
        modifier = modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.isLoading) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkeletonLine(width = 200.dp, height = 14.dp)
                            SkeletonLine(width = 140.dp, height = 14.dp)
                        }
                    }
                    state.statusMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }
                    state.error?.let {
                        Text(
                            text = sanitizeError(it),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text("Account", style = MaterialTheme.typography.labelLarge)
                    AccountSelectorRow(
                        accountNames = state.accounts.map { it.id to it.name },
                        selectedAccountId = state.accountId,
                        onSelect = viewModel::selectAccount
                    )

                    OutlinedTextField(
                        value = state.monthKey,
                        onValueChange = viewModel::onMonthKeyChanged,
                        label = { Text("Month (YYYY-MM)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.load() }, enabled = !state.isLoading && !state.isMutating) {
                            Text("Refresh")
                        }
                        Button(
                            onClick = viewModel::applyTemplates,
                            enabled = state.accountId.isNotBlank() && !state.isMutating
                        ) {
                            Text("Apply this month")
                        }
                    }
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Template focus", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "See active recurring volume before you apply anything.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SummaryRow(
                        summary = summary,
                        currencyCode = selectedCurrencyCode
                    )
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Filters", style = MaterialTheme.typography.titleMedium)
                    FilterRow(
                        selectedFilter = state.typeFilter,
                        showInactiveTemplates = state.showInactiveTemplates,
                        onFilterSelected = viewModel::onTypeFilterChanged,
                        onToggleInactive = viewModel::toggleShowInactiveTemplates
                    )
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Create template", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Saving into: $selectedAccountName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("EXPENSE", "INCOME").forEach { type ->
                            FilterChip(
                                selected = state.formType.equals(type, ignoreCase = true),
                                onClick = { viewModel.onFormTypeChanged(type) },
                                label = { Text(type.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    CategorySelector(
                        selectedCategoryName = selectedCategoryName,
                        categories = categoryOptions,
                        selectedCategoryId = state.formCategoryId,
                        onSelectCategory = viewModel::onFormCategoryIdChanged
                    )

                    OutlinedTextField(
                        value = state.formAmount,
                        onValueChange = viewModel::onFormAmountChanged,
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.formCurrency,
                        onValueChange = viewModel::onFormCurrencyChanged,
                        label = { Text("Currency") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.formDayOfMonth,
                        onValueChange = viewModel::onFormDayOfMonthChanged,
                        label = { Text("Day of month (1-28)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.formStartMonthKey,
                        onValueChange = viewModel::onFormStartMonthKeyChanged,
                        label = { Text("Start month (YYYY-MM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.formEndMonthKey,
                        onValueChange = viewModel::onFormEndMonthKeyChanged,
                        label = { Text("End month (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.formDescription,
                        onValueChange = viewModel::onFormDescriptionChanged,
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::saveTemplate, enabled = !state.isMutating) {
                            Text(if (state.isMutating) "Saving..." else "Save template")
                        }
                        OutlinedButton(onClick = viewModel::resetForm, enabled = !state.isMutating) {
                            Text("Reset")
                        }
                    }
                }
            }
        }

        if (filteredTemplates.isEmpty() && !state.isLoading) {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (state.templates.isEmpty()) {
                            "No recurring templates yet. Save one above to start automating monthly cashflow."
                        } else {
                            "No recurring templates match the current filters."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (activeTemplates.isNotEmpty()) {
            item {
                Text("Active templates", style = MaterialTheme.typography.titleMedium)
            }
            items(activeTemplates, key = { it.id }) { template ->
                RecurringTemplateItem(
                    template = template,
                    accountName = accountNames[template.accountId] ?: template.accountId,
                    categoryName = template.category?.name ?: categoryNames[template.categoryId] ?: template.categoryId,
                    categoryColor = template.category?.color,
                    isMutating = state.isMutating,
                    onToggle = { viewModel.toggleTemplate(template.id, false) },
                    onDelete = { showDeleteConfirmId = template.id }
                )
            }
        }

        if (state.showInactiveTemplates && pausedTemplates.isNotEmpty()) {
            item {
                Text("Paused templates", style = MaterialTheme.typography.titleMedium)
            }
            items(pausedTemplates, key = { it.id }) { template ->
                RecurringTemplateItem(
                    template = template,
                    accountName = accountNames[template.accountId] ?: template.accountId,
                    categoryName = template.category?.name ?: categoryNames[template.categoryId] ?: template.categoryId,
                    categoryColor = template.category?.color,
                    isMutating = state.isMutating,
                    onToggle = { viewModel.toggleTemplate(template.id, true) },
                    onDelete = { showDeleteConfirmId = template.id }
                )
            }
        }
    }
    } // PullToRefreshBox

    showDeleteConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmId = null },
            title = { Text("Delete template") },
            text = { Text("Are you sure you want to delete this recurring template? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.deleteTemplate(id)
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
        accountNames.forEach { (accountId, accountName) ->
            FilterChip(
                selected = selectedAccountId == accountId,
                onClick = { onSelect(accountId) },
                label = { Text(accountName) }
            )
        }
    }
}

@Composable
private fun FilterRow(
    selectedFilter: RecurringTypeFilter,
    showInactiveTemplates: Boolean,
    onFilterSelected: (RecurringTypeFilter) -> Unit,
    onToggleInactive: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RecurringTypeFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        when (filter) {
                            RecurringTypeFilter.ALL -> "All"
                            RecurringTypeFilter.EXPENSE -> "Expenses"
                            RecurringTypeFilter.INCOME -> "Income"
                        }
                    )
                }
            )
        }
        FilterChip(
            selected = showInactiveTemplates,
            onClick = onToggleInactive,
            label = { Text(if (showInactiveTemplates) "Showing paused" else "Hide paused") }
        )
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
private fun SummaryRow(
    summary: RecurringSummary,
    currencyCode: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard(label = "Active templates", value = summary.activeCount.toString())
        SummaryCard(
            label = "Monthly obligation",
            value = formatCurrency(summary.monthlyExpenseTotal, currencyCode)
        )
        SummaryCard(
            label = "Recurring income",
            value = formatCurrency(summary.monthlyIncomeTotal, currencyCode)
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun RecurringTemplateItem(
    template: RecurringTemplateDto,
    accountName: String,
    categoryName: String,
    categoryColor: String? = null,
    isMutating: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = parseHexColor(categoryColor),
                            shape = CircleShape
                        )
                )
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = "$accountName • ${template.type.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }} • Day ${template.dayOfMonth}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(template.amount.toDoubleOrNull() ?: 0.0, template.currency),
                style = MaterialTheme.typography.bodyMedium,
                color = if (template.type.equals("INCOME", ignoreCase = true)) {
                    Color(0xFF1FA267)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = "Start ${template.startMonth.take(7)}${template.endMonth?.let { " • End ${it.take(7)}" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            template.description?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onToggle, enabled = !isMutating) {
                    Text(if (template.isActive) "Pause" else "Activate")
                }
                OutlinedButton(onClick = onDelete, enabled = !isMutating) {
                    Text("Delete")
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double, currencyCode: String): String {
    return runCatching {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        formatter.currency = Currency.getInstance(currencyCode.uppercase(Locale.ROOT))
        formatter.format(amount)
    }.getOrElse {
        String.format(Locale.US, "%.2f %s", amount, currencyCode.uppercase(Locale.ROOT))
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

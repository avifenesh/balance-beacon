package app.balancebeacon.mobileandroid.feature.holdings.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.feature.holdings.model.HoldingDto
import app.balancebeacon.mobileandroid.ui.components.SkeletonLine
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import app.balancebeacon.mobileandroid.ui.util.sanitizeError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingsScreen(
    viewModel: HoldingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current
    var showDeleteConfirmId by rememberSaveable { mutableStateOf<String?>(null) }
    val snapshot = remember(state.holdings) { portfolioSnapshot(state.holdings) }
    val canCreate = state.accountId.isNotBlank() &&
        state.formCategoryId.isNotBlank() &&
        state.formSymbol.isNotBlank() &&
        state.formQuantity.isNotBlank() &&
        state.formAverageCost.isNotBlank()
    val canUpdate = canCreate && state.selectedHoldingId != null

    val accountNames = remember(state.accounts) { state.accounts.associate { it.id to it.name } }
    val categoryNames = remember(state.categories) { state.categories.associate { it.id to it.name } }
    val selectedCategoryName = categoryNames[state.formCategoryId] ?: "Select category"

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
        if (state.isMutating) {
            Text("Applying changes...", style = MaterialTheme.typography.bodySmall)
        }
        state.statusMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
        state.error?.let {
            Text(sanitizeError(it), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add Holding", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = state.formSymbol,
                    onValueChange = viewModel::onFormSymbolChanged,
                    label = { Text("Symbol") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.formQuantity,
                    onValueChange = viewModel::onFormQuantityChanged,
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.formAverageCost,
                    onValueChange = viewModel::onFormAverageCostChanged,
                    label = { Text("Average Cost") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Currency", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("USD", "EUR", "ILS").forEach { currency ->
                        FilterChip(
                            selected = state.formCurrency.equals(currency, ignoreCase = true),
                            onClick = { viewModel.onFormCurrencyChanged(currency) },
                            label = { Text(currency) }
                        )
                    }
                }
                OutlinedTextField(
                    value = state.formNotes,
                    onValueChange = viewModel::onFormNotesChanged,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Account", style = MaterialTheme.typography.labelLarge)
                AccountSelectorRow(
                    accountNames = state.accounts.map { it.id to it.name },
                    selectedAccountId = state.accountId,
                    onSelect = viewModel::onAccountIdChanged
                )

                Text("Category", style = MaterialTheme.typography.labelLarge)
                CategorySelector(
                    selectedCategoryName = selectedCategoryName,
                    categories = state.categories,
                    selectedCategoryId = state.formCategoryId,
                    onSelectCategory = viewModel::onFormCategoryIdChanged
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = canCreate && !state.isMutating,
                        onClick = viewModel::createHolding
                    ) {
                        Text("Create")
                    }
                    OutlinedButton(
                        enabled = canUpdate && !state.isMutating,
                        onClick = viewModel::updateSelectedHolding
                    ) {
                        Text("Update Selected")
                    }
                    OutlinedButton(
                        enabled = state.selectedHoldingId != null && !state.isMutating,
                        onClick = viewModel::clearSelection
                    ) {
                        Text("Clear")
                    }
                }

                Text(
                    text = "Selected: ${state.selectedHoldingId ?: "none"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Manage", style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::load, enabled = !state.isMutating) {
                        Text("Load")
                    }
                    OutlinedButton(
                        onClick = viewModel::refreshPrices,
                        enabled = state.accountId.isNotBlank() && !state.isMutating
                    ) {
                        Text("Refresh Prices")
                    }
                }
            }
        }

        if (state.holdings.isNotEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Portfolio snapshot", style = MaterialTheme.typography.titleMedium)
                    Text("Market value: ${formatAmount(snapshot.totalMarketValue)}", style = MaterialTheme.typography.bodySmall)
                    Text("Cost basis: ${formatAmount(snapshot.totalCostBasis)}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Gain/Loss: ${formatSignedAmount(snapshot.totalGainLoss)} (${formatAmount(snapshot.totalGainLossPercent)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (snapshot.totalGainLoss >= 0.0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = state.holdings, key = { it.id }) { holding ->
                HoldingCard(
                    holding = holding,
                    onEdit = { viewModel.selectHolding(holding) },
                    onDelete = { showDeleteConfirmId = holding.id },
                    isMutating = state.isMutating
                )
            }
        }
    }
    } // PullToRefreshBox

    showDeleteConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmId = null },
            title = { Text("Delete holding") },
            text = { Text("Are you sure you want to delete this holding? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.deleteHolding(id)
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
private fun HoldingCard(
    holding: HoldingDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isMutating: Boolean
) {
    val metrics = remember(holding) { holdingMetrics(holding) }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = holding.symbol,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (holding.isStale == true) {
                    "Quantity: ${holding.quantity} • Price status: stale"
                } else {
                    "Quantity: ${holding.quantity}"
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text("Average Cost: ${formatAmount(metrics.averageCost)}", style = MaterialTheme.typography.bodySmall)
            metrics.currentPrice?.let {
                Text("Current Price: ${formatAmount(it)}", style = MaterialTheme.typography.bodySmall)
            }
            metrics.marketValue?.let {
                Text("Market Value: ${formatAmount(it)}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Cost Basis: ${formatAmount(metrics.costBasis)}", style = MaterialTheme.typography.bodySmall)
            metrics.gainLoss?.let {
                val percent = metrics.gainLossPercent ?: 0.0
                Text(
                    text = "Gain/Loss: ${formatSignedAmount(it)} (${formatAmount(percent)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it >= 0.0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Text(
                text = "Currency: ${holding.currencyCode ?: "USD"}",
                style = MaterialTheme.typography.bodySmall
            )
            holding.lastPriceUpdate?.let {
                Text(
                    text = "Last update: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            holding.notes?.takeIf { it.isNotBlank() }?.let {
                Text("Notes: $it", style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, enabled = !isMutating) {
                    Text("Edit")
                }
                OutlinedButton(onClick = onDelete, enabled = !isMutating) {
                    Text("Delete")
                }
            }
        }
    }
}

private fun formatAmount(value: Double): String {
    return "%.2f".format(value)
}

private fun formatSignedAmount(value: Double): String {
    return if (value >= 0.0) {
        "+${formatAmount(value)}"
    } else {
        formatAmount(value)
    }
}

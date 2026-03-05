package app.balancebeacon.mobileandroid.feature.holdings.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.holdings.model.HoldingDto
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Composable
fun HoldingsScreen(
    viewModel: HoldingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val totalMarketValue = remember(state.holdings) {
        state.holdings.sumOf { it.marketValue.toDoubleValue() ?: 0.0 }
    }
    val totalCostBasis = remember(state.holdings) {
        state.holdings.sumOf { holding ->
            val quantity = holding.quantity.toDoubleOrNull() ?: 0.0
            val averageCost = holding.averageCost.toDoubleOrNull() ?: 0.0
            quantity * averageCost
        }
    }
    val totalGainLoss = totalMarketValue - totalCostBasis
    val totalGainLossPercent = if (totalCostBasis > 0.0) {
        (totalGainLoss / totalCostBasis) * 100.0
    } else {
        0.0
    }
    val canCreate = state.accountId.isNotBlank() &&
        state.formCategoryId.isNotBlank() &&
        state.formSymbol.isNotBlank() &&
        state.formQuantity.isNotBlank() &&
        state.formAverageCost.isNotBlank()
    val canUpdate = canCreate && state.selectedHoldingId != null

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
                Text("Holdings", style = MaterialTheme.typography.headlineSmall)
                if (state.isLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text("Loading holdings...")
                    }
                }
                if (state.isMutating) {
                    Text("Applying changes...", style = MaterialTheme.typography.bodySmall)
                }
                state.statusMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = state.accountId,
                    onValueChange = viewModel::onAccountIdChanged,
                    label = { Text("Account ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.formCategoryId,
                    onValueChange = viewModel::onFormCategoryIdChanged,
                    label = { Text("Category ID") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                OutlinedTextField(
                    value = state.formCurrency,
                    onValueChange = viewModel::onFormCurrencyChanged,
                    label = { Text("Currency (optional, defaults to USD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.formNotes,
                    onValueChange = viewModel::onFormNotesChanged,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::load, enabled = !state.isMutating) {
                        Text("Load")
                    }
                    Button(
                        onClick = viewModel::refreshPrices,
                        enabled = state.accountId.isNotBlank() && !state.isMutating
                    ) {
                        Text("Refresh Prices")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = canCreate && !state.isMutating,
                        onClick = viewModel::createHolding
                    ) {
                        Text("Create")
                    }
                    Button(
                        enabled = canUpdate && !state.isMutating,
                        onClick = viewModel::updateSelectedHolding
                    ) {
                        Text("Update Selected")
                    }
                    Button(
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

        if (state.holdings.isNotEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Portfolio snapshot", style = MaterialTheme.typography.titleMedium)
                    Text("Market value: ${formatAmount(totalMarketValue)}", style = MaterialTheme.typography.bodySmall)
                    Text("Cost basis: ${formatAmount(totalCostBasis)}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Gain/Loss: ${formatSignedAmount(totalGainLoss)} (${formatAmount(totalGainLossPercent)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (totalGainLoss >= 0.0) {
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
                    onDelete = { viewModel.deleteHolding(holding.id) },
                    isMutating = state.isMutating
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
    val quantity = holding.quantity.toDoubleOrNull() ?: 0.0
    val averageCost = holding.averageCost.toDoubleOrNull() ?: 0.0
    val currentPrice = holding.currentPrice.toDoubleValue()
    val marketValue = holding.marketValue.toDoubleValue() ?: currentPrice?.let { it * quantity }
    val costBasis = quantity * averageCost
    val gainLoss = if (marketValue != null) marketValue - costBasis else null
    val gainLossPercent = if (gainLoss != null && costBasis > 0.0) {
        (gainLoss / costBasis) * 100.0
    } else {
        null
    }

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
            Text("Average Cost: ${formatAmount(averageCost)}", style = MaterialTheme.typography.bodySmall)
            currentPrice?.let {
                Text("Current Price: ${formatAmount(it)}", style = MaterialTheme.typography.bodySmall)
            }
            marketValue?.let {
                Text("Market Value: ${formatAmount(it)}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Cost Basis: ${formatAmount(costBasis)}", style = MaterialTheme.typography.bodySmall)
            gainLoss?.let {
                val percent = gainLossPercent ?: 0.0
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
                Button(onClick = onEdit, enabled = !isMutating) {
                    Text("Edit")
                }
                Button(onClick = onDelete, enabled = !isMutating) {
                    Text("Delete")
                }
            }
        }
    }
}

private fun JsonElement?.toDoubleValue(): Double? {
    val element = this ?: return null
    if (element is JsonNull) return null
    if (element is JsonPrimitive) {
        return if (element.isString) {
            element.content.toDoubleOrNull()
        } else {
            element.toString().toDoubleOrNull()
        }
    }
    return element.toString().toDoubleOrNull()
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

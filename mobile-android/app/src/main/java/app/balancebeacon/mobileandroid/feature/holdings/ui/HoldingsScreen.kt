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
    val currentPrice = holding.currentPrice.toDisplayString()
    val marketValue = holding.marketValue.toDisplayString()

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = holding.symbol,
                style = MaterialTheme.typography.titleMedium
            )
            Text("Quantity: ${holding.quantity}", style = MaterialTheme.typography.bodySmall)
            Text("Average Cost: ${holding.averageCost}", style = MaterialTheme.typography.bodySmall)
            currentPrice?.let {
                Text("Current Price: $it", style = MaterialTheme.typography.bodySmall)
            }
            marketValue?.let {
                Text("Market Value: $it", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "Currency: ${holding.currencyCode ?: "USD"}",
                style = MaterialTheme.typography.bodySmall
            )
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

private fun JsonElement?.toDisplayString(): String? {
    val element = this ?: return null
    if (element is JsonNull) return null
    if (element is JsonPrimitive) {
        return if (element.isString) element.content else element.toString()
    }
    return element.toString()
}

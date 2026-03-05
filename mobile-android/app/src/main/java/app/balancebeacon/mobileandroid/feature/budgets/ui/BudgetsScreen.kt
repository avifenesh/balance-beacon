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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.budgets.model.CreateBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.QuickBudgetRequest
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var accountId by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("") }
    var monthKey by remember { mutableStateOf("") }
    var planned by remember { mutableStateOf("") }
    var currencyCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val canSubmit = accountId.isNotBlank() &&
        categoryId.isNotBlank() &&
        monthKey.isNotBlank() &&
        planned.isNotBlank()

    LaunchedEffect(Unit) {
        viewModel.load()
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
                    onValueChange = { currencyCode = it },
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
                        onClick = {
                            viewModel.load(month = monthKey.ifBlank { null })
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
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = state.items,
                key = { item -> "${item.accountId}:${item.categoryId}:${item.monthKey}" }
            ) { item ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${item.monthKey} - ${item.amount}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text("Account: ${item.accountId}", style = MaterialTheme.typography.bodySmall)
                        Text("Category: ${item.categoryId}", style = MaterialTheme.typography.bodySmall)
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


package app.balancebeacon.mobileandroid.feature.recurring.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringTemplateDto
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel

@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recurring", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Manage monthly auto-repeat templates",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = state.accountId,
                        onValueChange = viewModel::onAccountIdChanged,
                        label = { Text("Account ID (list/create)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.monthKey,
                        onValueChange = viewModel::onMonthKeyChanged,
                        label = { Text("Month Key (YYYY-MM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.applyAccountId,
                        onValueChange = viewModel::onApplyAccountIdChanged,
                        label = { Text("Apply Account ID (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = when (state.filterIsActive) {
                            true -> "active"
                            false -> "inactive"
                            null -> "all"
                        },
                        onValueChange = viewModel::onFilterChanged,
                        label = { Text("Filter (all/active/inactive)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::load, enabled = !state.isLoading && !state.isMutating) {
                            Text("Load")
                        }
                        Button(onClick = viewModel::applyTemplates, enabled = !state.isMutating) {
                            Text("Apply Month")
                        }
                    }
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Upsert Template", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = state.formId,
                        onValueChange = viewModel::onFormIdChanged,
                        label = { Text("Template ID (optional, for update)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.formCategoryId,
                        onValueChange = viewModel::onFormCategoryIdChanged,
                        label = { Text("Category ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.formType,
                        onValueChange = viewModel::onFormTypeChanged,
                        label = { Text("Type (EXPENSE/INCOME)") },
                        modifier = Modifier.fillMaxWidth()
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
                        label = { Text("Currency (USD/EUR/ILS)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.formDayOfMonth,
                        onValueChange = viewModel::onFormDayOfMonthChanged,
                        label = { Text("Day Of Month (1-28)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.formStartMonthKey,
                        onValueChange = viewModel::onFormStartMonthKeyChanged,
                        label = { Text("Start Month Key (YYYY-MM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.formEndMonthKey,
                        onValueChange = viewModel::onFormEndMonthKeyChanged,
                        label = { Text("End Month Key (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.formDescription,
                        onValueChange = viewModel::onFormDescriptionChanged,
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = if (state.formIsActive) "true" else "false",
                        onValueChange = { viewModel.onFormIsActiveChanged(it.trim().lowercase() != "false") },
                        label = { Text("Is Active (true/false)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::saveTemplate, enabled = !state.isMutating) {
                            Text(if (state.isMutating) "Working..." else "Save Template")
                        }
                        OutlinedButton(onClick = viewModel::resetForm, enabled = !state.isMutating) {
                            Text("Reset Form")
                        }
                    }
                }
            }
        }

        if (state.isLoading) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text("Loading templates...")
                }
            }
        }

        state.statusMessage?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        state.error?.let { error ->
            item {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (state.templates.isEmpty() && !state.isLoading) {
            item {
                Text(
                    text = "No recurring templates loaded.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        items(state.templates, key = { it.id }) { template ->
            RecurringTemplateItem(
                template = template,
                isMutating = state.isMutating,
                onToggle = { viewModel.toggleTemplate(template.id, !template.isActive) }
            )
        }
    }
}

@Composable
private fun RecurringTemplateItem(
    template: RecurringTemplateDto,
    isMutating: Boolean,
    onToggle: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${template.type} ${template.amount} ${template.currency}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Category: ${template.categoryId} • Day: ${template.dayOfMonth}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Start: ${template.startMonth} • End: ${template.endMonth ?: "none"}",
                style = MaterialTheme.typography.bodySmall
            )
            template.description?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = onToggle,
                enabled = !isMutating
            ) {
                Text(if (template.isActive) "Pause" else "Activate")
            }
        }
    }
}

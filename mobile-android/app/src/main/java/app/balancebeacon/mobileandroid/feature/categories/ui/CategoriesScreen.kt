package app.balancebeacon.mobileandroid.feature.categories.ui

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
import androidx.compose.material3.TextButton
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
import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var visibleCount by rememberSaveable { mutableStateOf(DEFAULT_VISIBLE_COUNT) }
    val filteredItems = remember(state.items, searchQuery) {
        filterCategories(
            items = state.items,
            searchQuery = searchQuery
        )
    }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(state.items.size) {
        visibleCount = DEFAULT_VISIBLE_COUNT
    }

    if (state.isLoading && state.items.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Categories", style = MaterialTheme.typography.headlineSmall)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.filterType,
                        onValueChange = viewModel::onFilterTypeChanged,
                        label = { Text("Filter type (optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !state.isMutating
                    )
                    Button(
                        onClick = {
                            visibleCount = DEFAULT_VISIBLE_COUNT
                            viewModel.applyFilters()
                        },
                        enabled = !state.isMutating
                    ) {
                        Text("Apply")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        visibleCount = DEFAULT_VISIBLE_COUNT
                    },
                    label = { Text("Search loaded categories") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isMutating
                )

                TextButton(
                    onClick = {
                        viewModel.onIncludeArchivedChanged(!state.includeArchived)
                        viewModel.applyFilters()
                    },
                    enabled = !state.isMutating
                ) {
                    val label = if (state.includeArchived) "Hide archived" else "Show archived"
                    Text("$label (currently: ${if (state.includeArchived) "on" else "off"})")
                }
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.createName,
                    onValueChange = viewModel::onCreateNameChanged,
                    label = { Text("New category name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isMutating
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.createType,
                        onValueChange = viewModel::onCreateTypeChanged,
                        label = { Text("Type (EXPENSE/INCOME)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !state.isMutating
                    )
                    OutlinedTextField(
                        value = state.createColor,
                        onValueChange = viewModel::onCreateColorChanged,
                        label = { Text("Color hex (optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !state.isMutating
                    )
                }

                Button(
                    onClick = viewModel::createCategory,
                    enabled = !state.isMutating
                ) {
                    Text(if (state.isMutating) "Working..." else "Create category")
                }
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Bulk create", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = state.bulkNames,
                    onValueChange = viewModel::onBulkNamesChanged,
                    label = { Text("Names (comma or newline separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isMutating
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.bulkType,
                        onValueChange = viewModel::onBulkTypeChanged,
                        label = { Text("Bulk type") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !state.isMutating
                    )
                    OutlinedTextField(
                        value = state.bulkColor,
                        onValueChange = viewModel::onBulkColorChanged,
                        label = { Text("Bulk color (optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !state.isMutating
                    )
                }

                Button(
                    onClick = viewModel::bulkCreateCategories,
                    enabled = !state.isMutating
                ) {
                    Text("Bulk create categories")
                }
            }
        }

        if (state.editingCategoryId != null) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Edit category", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = state.editName,
                        onValueChange = viewModel::onEditNameChanged,
                        label = { Text("Edit name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isMutating
                    )

                    OutlinedTextField(
                        value = state.editColor,
                        onValueChange = viewModel::onEditColorChanged,
                        label = { Text("Edit color") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isMutating
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::updateCategory,
                            enabled = !state.isMutating
                        ) {
                            Text("Save")
                        }
                        TextButton(
                            onClick = viewModel::cancelEditing,
                            enabled = !state.isMutating
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        state.statusMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Text("Existing categories", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredItems.isEmpty()) {
                item {
                    Text(
                        text = if (state.items.isEmpty()) {
                            "No categories loaded"
                        } else {
                            "No categories match your search"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(items = filteredItems.take(visibleCount), key = { it.id }) { category ->
                CategoryItem(
                    category = category,
                    isMutating = state.isMutating,
                    onEdit = viewModel::startEditing,
                    onSetArchived = viewModel::setArchived
                )
            }

            if (filteredItems.size > visibleCount) {
                item {
                    OutlinedButton(
                        onClick = { visibleCount = nextVisibleCategoryCount(visibleCount) },
                        enabled = !state.isMutating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Load more (${remainingCategoryCount(filteredItems.size, visibleCount)} remaining)")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: CategoryDto,
    isMutating: Boolean,
    onEdit: (CategoryDto) -> Unit,
    onSetArchived: (String, Boolean) -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Type: ${category.type} | Status: ${if (category.isArchived) "archived" else "active"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Color: ${category.color ?: "-"}",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onEdit(category) },
                    enabled = !isMutating
                ) {
                    Text("Edit")
                }
                TextButton(
                    onClick = { onSetArchived(category.id, !category.isArchived) },
                    enabled = !isMutating
                ) {
                    Text(if (category.isArchived) "Unarchive" else "Archive")
                }
            }
        }
    }
}

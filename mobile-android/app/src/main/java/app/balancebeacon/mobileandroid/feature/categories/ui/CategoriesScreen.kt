package app.balancebeacon.mobileandroid.feature.categories.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.ui.theme.SkyBlue
import app.balancebeacon.mobileandroid.ui.components.SkeletonListScreen
import app.balancebeacon.mobileandroid.ui.util.sanitizeError
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel

private val Amber = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var visibleCount by rememberSaveable { mutableStateOf(DEFAULT_VISIBLE_COUNT) }
    var showArchiveConfirmId by rememberSaveable { mutableStateOf<String?>(null) }
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
        SkeletonListScreen(modifier = modifier)
        return
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
        // Filter section
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
        }

        // Create section
        item {
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("EXPENSE", "INCOME").forEach { option ->
                            FilterChip(
                                selected = state.createType == option,
                                onClick = { viewModel.onCreateTypeChanged(option) },
                                label = { Text(option) },
                                enabled = !state.isMutating
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.createColor,
                        onValueChange = viewModel::onCreateColorChanged,
                        label = { Text("Color hex (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isMutating
                    )

                    Button(
                        onClick = viewModel::createCategory,
                        enabled = !state.isMutating
                    ) {
                        Text(if (state.isMutating) "Working..." else "Create category")
                    }
                }
            }
        }

        // Bulk create section
        item {
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("EXPENSE", "INCOME").forEach { option ->
                            FilterChip(
                                selected = state.bulkType == option,
                                onClick = { viewModel.onBulkTypeChanged(option) },
                                label = { Text(option) },
                                enabled = !state.isMutating
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.bulkColor,
                        onValueChange = viewModel::onBulkColorChanged,
                        label = { Text("Bulk color (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isMutating
                    )

                    Button(
                        onClick = viewModel::bulkCreateCategories,
                        enabled = !state.isMutating
                    ) {
                        Text("Bulk create categories")
                    }
                }
            }
        }

        // Edit section (conditional)
        if (state.editingCategoryId != null) {
            item {
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
        }

        // Status and error messages + category list header
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Existing categories", style = MaterialTheme.typography.titleMedium)

                    state.statusMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }

                    state.error?.let {
                        Text(sanitizeError(it), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Category list items
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
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart && !category.isArchived) {
                        showArchiveConfirmId = category.id
                        false
                    } else false
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Amber, RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = Color.White
                        )
                    }
                },
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = !category.isArchived
            ) {
                CategoryItem(
                    category = category,
                    isMutating = state.isMutating,
                    onEdit = viewModel::startEditing,
                    onSetArchived = { id, archive ->
                        if (archive) {
                            showArchiveConfirmId = id
                        } else {
                            viewModel.setArchived(id, false)
                        }
                    }
                )
            }
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
    } // PullToRefreshBox

    showArchiveConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { showArchiveConfirmId = null },
            title = { Text("Archive category") },
            text = { Text("Are you sure you want to archive this category? It can be unarchived later.") },
            confirmButton = {
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.setArchived(id, true)
                    showArchiveConfirmId = null
                }) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirmId = null }) { Text("Cancel") }
            }
        )
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = parseHexColor(category.color),
                            shape = CircleShape
                        )
                )
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "Type: ${category.type} | Status: ${if (category.isArchived) "archived" else "active"}",
                style = MaterialTheme.typography.bodyMedium
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

private fun parseHexColor(hex: String?, default: Color = SkyBlue): Color {
    if (hex.isNullOrBlank()) return default
    return try {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    } catch (_: Exception) {
        default
    }
}

package app.balancebeacon.mobileandroid.feature.categories.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.categories.data.CategoriesRepository
import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.feature.categories.model.CreateCategoryRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class CategoriesUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val items: List<CategoryDto> = emptyList(),
    val filterType: String = "",
    val includeArchived: Boolean = false,
    val createName: String = "",
    val createType: String = "EXPENSE",
    val createColor: String = "",
    val bulkNames: String = "",
    val bulkType: String = "EXPENSE",
    val bulkColor: String = "",
    val editingCategoryId: String? = null,
    val editName: String = "",
    val editColor: String = "",
    val statusMessage: String? = null,
    val error: String? = null
)

class CategoriesViewModel(
    private val categoriesRepository: CategoriesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    fun load(type: String? = null, includeArchived: Boolean = false) {
        val normalizedType = normalizeFilterType(type ?: _uiState.value.filterType)
        viewModelScope.launch {
            fetchCategories(
                type = normalizedType,
                includeArchived = includeArchived,
                showLoading = true
            )
        }
    }

    fun onFilterTypeChanged(value: String) {
        _uiState.update { it.copy(filterType = value, error = null, statusMessage = null) }
    }

    fun onIncludeArchivedChanged(value: Boolean) {
        _uiState.update { it.copy(includeArchived = value, error = null, statusMessage = null) }
    }

    fun applyFilters() {
        load(
            type = _uiState.value.filterType,
            includeArchived = _uiState.value.includeArchived
        )
    }

    fun onCreateNameChanged(value: String) {
        _uiState.update { it.copy(createName = value, error = null, statusMessage = null) }
    }

    fun onCreateTypeChanged(value: String) {
        _uiState.update { it.copy(createType = value, error = null, statusMessage = null) }
    }

    fun onCreateColorChanged(value: String) {
        _uiState.update { it.copy(createColor = value, error = null, statusMessage = null) }
    }

    fun createCategory() {
        val state = _uiState.value
        if (state.createName.isBlank()) {
            _uiState.update { it.copy(error = "Category name is required") }
            return
        }

        val type = normalizeCategoryType(state.createType)
        if (type == null) {
            _uiState.update { it.copy(error = "Category type must be EXPENSE or INCOME") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (
                val result = categoriesRepository.createCategory(
                    name = state.createName,
                    type = type,
                    color = state.createColor.trim().ifBlank { null }
                )
            ) {
                is AppResult.Success -> refreshCategoriesAfterMutation(
                    statusMessage = "Category created",
                    resetCreateForm = true
                )

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun onBulkNamesChanged(value: String) {
        _uiState.update { it.copy(bulkNames = value, error = null, statusMessage = null) }
    }

    fun onBulkTypeChanged(value: String) {
        _uiState.update { it.copy(bulkType = value, error = null, statusMessage = null) }
    }

    fun onBulkColorChanged(value: String) {
        _uiState.update { it.copy(bulkColor = value, error = null, statusMessage = null) }
    }

    fun bulkCreateCategories() {
        val state = _uiState.value
        val categoryType = normalizeCategoryType(state.bulkType)
        if (categoryType == null) {
            _uiState.update { it.copy(error = "Bulk type must be EXPENSE or INCOME") }
            return
        }

        val names = state.bulkNames
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (names.isEmpty()) {
            _uiState.update {
                it.copy(error = "Enter one or more category names (comma/newline separated)")
            }
            return
        }

        val color = state.bulkColor.trim().ifBlank { null }
        val requests = names.map { name ->
            CreateCategoryRequest(
                name = name,
                type = categoryType,
                color = color
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (val result = categoriesRepository.bulkCreateCategories(requests)) {
                is AppResult.Success -> refreshCategoriesAfterMutation(
                    statusMessage = "Created ${result.value.categoriesCreated} categories",
                    resetBulkForm = true
                )

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun startEditing(category: CategoryDto) {
        _uiState.update {
            it.copy(
                editingCategoryId = category.id,
                editName = category.name,
                editColor = category.color.orEmpty(),
                error = null,
                statusMessage = null
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                editingCategoryId = null,
                editName = "",
                editColor = "",
                error = null,
                statusMessage = null
            )
        }
    }

    fun onEditNameChanged(value: String) {
        _uiState.update { it.copy(editName = value, error = null, statusMessage = null) }
    }

    fun onEditColorChanged(value: String) {
        _uiState.update { it.copy(editColor = value, error = null, statusMessage = null) }
    }

    fun updateCategory() {
        val state = _uiState.value
        val categoryId = state.editingCategoryId
        if (categoryId.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Select a category to edit") }
            return
        }

        if (state.editName.isBlank()) {
            _uiState.update { it.copy(error = "Category name is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (
                val result = categoriesRepository.updateCategory(
                    id = categoryId,
                    name = state.editName,
                    color = state.editColor.trim().ifBlank { null }
                )
            ) {
                is AppResult.Success -> refreshCategoriesAfterMutation(
                    statusMessage = "Category updated",
                    resetEditForm = true
                )

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun setArchived(id: String, isArchived: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (val result = categoriesRepository.archiveCategory(id, isArchived)) {
                is AppResult.Success -> {
                    val status = if (result.value.isArchived) {
                        "Category archived"
                    } else {
                        "Category unarchived"
                    }
                    val shouldResetEdit = _uiState.value.editingCategoryId == id
                    refreshCategoriesAfterMutation(
                        statusMessage = status,
                        resetEditForm = shouldResetEdit
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    private suspend fun fetchCategories(
        type: String?,
        includeArchived: Boolean,
        showLoading: Boolean
    ) {
        if (showLoading) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    filterType = type.orEmpty(),
                    includeArchived = includeArchived,
                    error = null
                )
            }
        }

        when (val result = categoriesRepository.getCategories(type, includeArchived)) {
            is AppResult.Success -> _uiState.update {
                it.copy(
                    isLoading = false,
                    isMutating = false,
                    items = result.value,
                    filterType = type.orEmpty(),
                    includeArchived = includeArchived,
                    error = null
                )
            }

            is AppResult.Failure -> _uiState.update {
                it.copy(
                    isLoading = false,
                    isMutating = false,
                    filterType = type.orEmpty(),
                    includeArchived = includeArchived,
                    error = result.error.message
                )
            }
        }
    }

    private suspend fun refreshCategoriesAfterMutation(
        statusMessage: String,
        resetCreateForm: Boolean = false,
        resetBulkForm: Boolean = false,
        resetEditForm: Boolean = false
    ) {
        val state = _uiState.value
        val filterType = normalizeFilterType(state.filterType)
        val includeArchived = state.includeArchived

        when (val result = categoriesRepository.getCategories(filterType, includeArchived)) {
            is AppResult.Success -> _uiState.update { current ->
                var next = current.copy(
                    isLoading = false,
                    isMutating = false,
                    items = result.value,
                    filterType = filterType.orEmpty(),
                    includeArchived = includeArchived,
                    statusMessage = statusMessage,
                    error = null
                )

                if (resetCreateForm) {
                    next = next.copy(
                        createName = "",
                        createType = "EXPENSE",
                        createColor = ""
                    )
                }

                if (resetBulkForm) {
                    next = next.copy(
                        bulkNames = "",
                        bulkType = "EXPENSE",
                        bulkColor = ""
                    )
                }

                if (resetEditForm) {
                    next = next.copy(
                        editingCategoryId = null,
                        editName = "",
                        editColor = ""
                    )
                }

                next
            }

            is AppResult.Failure -> _uiState.update {
                it.copy(
                    isMutating = false,
                    error = result.error.message
                )
            }
        }
    }

    private fun normalizeCategoryType(raw: String): String? {
        val normalized = raw.trim().uppercase(Locale.ROOT)
        return normalized.takeIf { it in VALID_CATEGORY_TYPES }
    }

    private fun normalizeFilterType(raw: String?): String? {
        if (raw == null) return null
        val normalized = raw.trim().uppercase(Locale.ROOT)
        if (normalized.isBlank()) return null
        return normalized.takeIf { it in VALID_CATEGORY_TYPES }
    }

    private companion object {
        val VALID_CATEGORY_TYPES = setOf(
            "EXPENSE",
            "INCOME"
        )
    }
}

package app.balancebeacon.mobileandroid.feature.recurring.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsRepository
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.feature.categories.data.CategoriesRepository
import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.feature.recurring.data.RecurringRepository
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringTemplateDto
import app.balancebeacon.mobileandroid.feature.recurring.model.UpsertRecurringTemplateRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecurringUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val accountId: String = "",
    val monthKey: String = currentMonthKey(),
    val templates: List<RecurringTemplateDto> = emptyList(),
    val typeFilter: RecurringTypeFilter = RecurringTypeFilter.ALL,
    val showInactiveTemplates: Boolean = false,
    val formCategoryId: String = "",
    val formType: String = "EXPENSE",
    val formAmount: String = "",
    val formCurrency: String = "USD",
    val formDayOfMonth: String = "1",
    val formDescription: String = "",
    val formStartMonthKey: String = currentMonthKey(),
    val formEndMonthKey: String = "",
    val statusMessage: String? = null,
    val error: String? = null
)

class RecurringViewModel(
    private val recurringRepository: RecurringRepository,
    private val accountsRepository: AccountsRepository,
    private val categoriesRepository: CategoriesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, statusMessage = null) }

            val accounts = when (val result = accountsRepository.getAccounts()) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                    return@launch
                }
            }

            val categories = when (val result = categoriesRepository.getCategories(includeArchived = false)) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                    return@launch
                }
            }

            val current = _uiState.value
            val resolvedAccountId = current.accountId.ifBlank { accounts.firstOrNull()?.id.orEmpty() }
            val resolvedCategoryId = resolveCategoryId(
                categories = categories,
                type = current.formType,
                currentCategoryId = current.formCategoryId
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    accounts = accounts,
                    categories = categories,
                    accountId = resolvedAccountId,
                    formCurrency = resolveAccountCurrency(accounts, resolvedAccountId),
                    formCategoryId = resolvedCategoryId,
                    error = if (accounts.isEmpty()) {
                        "Create an account first to manage recurring templates"
                    } else {
                        null
                    }
                )
            }

            if (resolvedAccountId.isNotBlank()) {
                load(accountId = resolvedAccountId)
            }
        }
    }

    fun load(accountId: String? = null) {
        val resolvedAccountId = (accountId ?: _uiState.value.accountId).trim()
        if (resolvedAccountId.isBlank()) {
            _uiState.update { it.copy(error = "Select an account to load recurring templates") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, statusMessage = null) }
            when (val result = recurringRepository.getRecurringTemplates(accountId = resolvedAccountId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        accountId = resolvedAccountId,
                        templates = result.value,
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun selectAccount(value: String) {
        _uiState.update {
            it.copy(
                accountId = value,
                formCurrency = resolveAccountCurrency(it.accounts, value),
                statusMessage = null,
                error = null
            )
        }
        load(accountId = value)
    }

    fun onMonthKeyChanged(value: String) {
        _uiState.update { it.copy(monthKey = value, statusMessage = null, error = null) }
    }

    fun onTypeFilterChanged(filter: RecurringTypeFilter) {
        _uiState.update { it.copy(typeFilter = filter, statusMessage = null, error = null) }
    }

    fun toggleShowInactiveTemplates() {
        _uiState.update {
            it.copy(
                showInactiveTemplates = !it.showInactiveTemplates,
                statusMessage = null,
                error = null
            )
        }
    }

    fun onFormCategoryIdChanged(value: String) {
        _uiState.update { it.copy(formCategoryId = value, statusMessage = null, error = null) }
    }

    fun onFormTypeChanged(value: String) {
        _uiState.update {
            it.copy(
                formType = normalizeType(value),
                formCategoryId = resolveCategoryId(
                    categories = it.categories,
                    type = value,
                    currentCategoryId = it.formCategoryId
                ),
                statusMessage = null,
                error = null
            )
        }
    }

    fun onFormAmountChanged(value: String) {
        _uiState.update { it.copy(formAmount = value, statusMessage = null, error = null) }
    }

    fun onFormCurrencyChanged(value: String) {
        _uiState.update { it.copy(formCurrency = value, statusMessage = null, error = null) }
    }

    fun onFormDayOfMonthChanged(value: String) {
        _uiState.update { it.copy(formDayOfMonth = value, statusMessage = null, error = null) }
    }

    fun onFormDescriptionChanged(value: String) {
        _uiState.update { it.copy(formDescription = value, statusMessage = null, error = null) }
    }

    fun onFormStartMonthKeyChanged(value: String) {
        _uiState.update { it.copy(formStartMonthKey = value, statusMessage = null, error = null) }
    }

    fun onFormEndMonthKeyChanged(value: String) {
        _uiState.update { it.copy(formEndMonthKey = value, statusMessage = null, error = null) }
    }

    fun saveTemplate() {
        val state = _uiState.value
        val amount = state.formAmount.toDoubleOrNull()
        val dayOfMonth = state.formDayOfMonth.toIntOrNull()

        if (state.accountId.isBlank()) {
            _uiState.update { it.copy(error = "Select an account first") }
            return
        }
        if (state.formCategoryId.isBlank()) {
            _uiState.update { it.copy(error = "Select a category") }
            return
        }
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Amount must be greater than 0") }
            return
        }
        if (dayOfMonth == null || dayOfMonth !in 1..28) {
            _uiState.update { it.copy(error = "Day of month must be between 1 and 28") }
            return
        }
        if (!MONTH_KEY_REGEX.matches(state.formStartMonthKey.trim())) {
            _uiState.update { it.copy(error = "Start month must use YYYY-MM") }
            return
        }
        if (state.formEndMonthKey.isNotBlank() && !MONTH_KEY_REGEX.matches(state.formEndMonthKey.trim())) {
            _uiState.update { it.copy(error = "End month must use YYYY-MM") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }

            val request = UpsertRecurringTemplateRequest(
                accountId = state.accountId.trim(),
                categoryId = state.formCategoryId.trim(),
                type = normalizeType(state.formType),
                amount = amount,
                currency = normalizeCurrency(state.formCurrency),
                dayOfMonth = dayOfMonth,
                description = state.formDescription.trim().ifBlank { null },
                startMonthKey = state.formStartMonthKey.trim(),
                endMonthKey = state.formEndMonthKey.trim().ifBlank { null },
                isActive = true
            )

            when (val result = recurringRepository.upsertRecurringTemplate(request)) {
                is AppResult.Success -> {
                    resetFormInternal()
                    reloadTemplatesAfterMutation(statusMessage = "Recurring template created")
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun toggleTemplate(id: String, isActive: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (val result = recurringRepository.toggleRecurringTemplate(id = id, isActive = isActive)) {
                is AppResult.Success -> reloadTemplatesAfterMutation(
                    statusMessage = if (result.value.isActive) "Template activated" else "Template paused"
                )

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (val result = recurringRepository.deleteRecurringTemplate(id = id)) {
                is AppResult.Success -> reloadTemplatesAfterMutation(
                    statusMessage = if (result.value.deleted) "Template deleted" else "Template updated"
                )

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun applyTemplates() {
        val state = _uiState.value
        if (!MONTH_KEY_REGEX.matches(state.monthKey.trim())) {
            _uiState.update { it.copy(error = "Month must use YYYY-MM") }
            return
        }
        if (state.accountId.isBlank()) {
            _uiState.update { it.copy(error = "Select an account before applying templates") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (
                val result = recurringRepository.applyRecurringTemplates(
                    monthKey = state.monthKey.trim(),
                    accountId = state.accountId.trim()
                )
            ) {
                is AppResult.Success -> {
                    val status = buildString {
                        append("Applied recurring templates: created ${result.value.created}, skipped ${result.value.skipped}")
                        if (result.value.errors.isNotEmpty()) {
                            append(" (${result.value.errors.size} errors)")
                        }
                    }
                    reloadTemplatesAfterMutation(statusMessage = status)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun resetForm() {
        resetFormInternal()
    }

    private suspend fun reloadTemplatesAfterMutation(statusMessage: String) {
        val accountId = _uiState.value.accountId.trim()
        if (accountId.isBlank()) {
            _uiState.update { it.copy(isMutating = false, statusMessage = statusMessage, error = null) }
            return
        }

        when (val loadResult = recurringRepository.getRecurringTemplates(accountId = accountId)) {
            is AppResult.Success -> _uiState.update {
                it.copy(
                    isMutating = false,
                    templates = loadResult.value,
                    statusMessage = statusMessage,
                    error = null
                )
            }

            is AppResult.Failure -> _uiState.update {
                it.copy(isMutating = false, error = loadResult.error.message)
            }
        }
    }

    private fun resetFormInternal() {
        _uiState.update {
            it.copy(
                formCategoryId = resolveCategoryId(
                    categories = it.categories,
                    type = "EXPENSE",
                    currentCategoryId = ""
                ),
                formType = "EXPENSE",
                formAmount = "",
                formCurrency = resolveAccountCurrency(it.accounts, it.accountId),
                formDayOfMonth = "1",
                formDescription = "",
                formStartMonthKey = currentMonthKey(),
                formEndMonthKey = "",
                statusMessage = null,
                error = null
            )
        }
    }

    private fun resolveCategoryId(
        categories: List<CategoryDto>,
        type: String,
        currentCategoryId: String
    ): String {
        val eligibleCategories = filterRecurringCategories(categories, type)
        return eligibleCategories.firstOrNull { it.id == currentCategoryId }?.id
            ?: eligibleCategories.firstOrNull()?.id
            ?: ""
    }

    private fun normalizeType(value: String): String {
        return if (value.trim().equals("INCOME", ignoreCase = true)) "INCOME" else "EXPENSE"
    }

    private fun normalizeCurrency(value: String): String {
        val normalized = value.trim().uppercase(Locale.ROOT)
        return if (normalized.isBlank()) "USD" else normalized
    }

    private fun resolveAccountCurrency(accounts: List<AccountDto>, accountId: String): String {
        return accounts.firstOrNull { it.id == accountId }?.preferredCurrency?.takeIf { it.isNotBlank() } ?: "USD"
    }

    private companion object {
        val MONTH_KEY_REGEX = Regex("^\\d{4}-\\d{2}$")
    }
}

private fun currentMonthKey(): String {
    val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date())
}

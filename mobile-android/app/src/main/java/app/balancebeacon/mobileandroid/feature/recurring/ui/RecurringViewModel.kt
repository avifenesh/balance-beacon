package app.balancebeacon.mobileandroid.feature.recurring.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.recurring.data.RecurringRepository
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringTemplateDto
import app.balancebeacon.mobileandroid.feature.recurring.model.UpsertRecurringTemplateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class RecurringUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val accountId: String = "",
    val filterIsActive: Boolean? = null,
    val applyAccountId: String = "",
    val monthKey: String = "",
    val templates: List<RecurringTemplateDto> = emptyList(),
    val formId: String = "",
    val formCategoryId: String = "",
    val formType: String = "EXPENSE",
    val formAmount: String = "",
    val formCurrency: String = "USD",
    val formDayOfMonth: String = "1",
    val formDescription: String = "",
    val formStartMonthKey: String = "",
    val formEndMonthKey: String = "",
    val formIsActive: Boolean = true,
    val statusMessage: String? = null,
    val error: String? = null
)

class RecurringViewModel(
    private val recurringRepository: RecurringRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    fun load() {
        val state = _uiState.value
        if (state.accountId.isBlank()) {
            _uiState.update { it.copy(error = "Account ID is required to load recurring templates") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (
                val result = recurringRepository.getRecurringTemplates(
                    accountId = state.accountId,
                    isActive = state.filterIsActive
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoading = false, templates = result.value, error = null)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun onAccountIdChanged(value: String) {
        _uiState.update { it.copy(accountId = value, statusMessage = null, error = null) }
    }

    fun onMonthKeyChanged(value: String) {
        _uiState.update { it.copy(monthKey = value, statusMessage = null, error = null) }
    }

    fun onApplyAccountIdChanged(value: String) {
        _uiState.update { it.copy(applyAccountId = value, statusMessage = null, error = null) }
    }

    fun onFilterChanged(value: String) {
        val normalized = value.trim().lowercase(Locale.ROOT)
        val filter = when (normalized) {
            "active" -> true
            "inactive" -> false
            else -> null
        }
        _uiState.update { it.copy(filterIsActive = filter, statusMessage = null, error = null) }
    }

    fun onFormIdChanged(value: String) {
        _uiState.update { it.copy(formId = value, statusMessage = null, error = null) }
    }

    fun onFormCategoryIdChanged(value: String) {
        _uiState.update { it.copy(formCategoryId = value, statusMessage = null, error = null) }
    }

    fun onFormTypeChanged(value: String) {
        _uiState.update { it.copy(formType = value, statusMessage = null, error = null) }
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

    fun onFormIsActiveChanged(value: Boolean) {
        _uiState.update { it.copy(formIsActive = value, statusMessage = null, error = null) }
    }

    fun saveTemplate() {
        val state = _uiState.value
        val amount = state.formAmount.toDoubleOrNull()
        val dayOfMonth = state.formDayOfMonth.toIntOrNull()

        if (state.accountId.isBlank()) {
            _uiState.update { it.copy(error = "Account ID is required") }
            return
        }
        if (state.formCategoryId.isBlank()) {
            _uiState.update { it.copy(error = "Category ID is required") }
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
            _uiState.update { it.copy(error = "Start month key must be YYYY-MM") }
            return
        }
        if (state.formEndMonthKey.isNotBlank() && !MONTH_KEY_REGEX.matches(state.formEndMonthKey.trim())) {
            _uiState.update { it.copy(error = "End month key must be YYYY-MM") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }

            val request = UpsertRecurringTemplateRequest(
                id = state.formId.trim().ifBlank { null },
                accountId = state.accountId.trim(),
                categoryId = state.formCategoryId.trim(),
                type = normalizeType(state.formType),
                amount = amount,
                currency = normalizeCurrency(state.formCurrency),
                dayOfMonth = dayOfMonth,
                description = state.formDescription.trim().ifBlank { null },
                startMonthKey = state.formStartMonthKey.trim(),
                endMonthKey = state.formEndMonthKey.trim().ifBlank { null },
                isActive = state.formIsActive
            )

            when (val result = recurringRepository.upsertRecurringTemplate(request)) {
                is AppResult.Success -> {
                    refreshTemplatesAfterMutation(
                        statusMessage = if (request.id == null) "Recurring template created" else "Recurring template updated"
                    )
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
                is AppResult.Success -> refreshTemplatesAfterMutation(
                    statusMessage = if (result.value.isActive) "Template activated" else "Template paused"
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
            _uiState.update { it.copy(error = "Month key must be YYYY-MM") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (
                val result = recurringRepository.applyRecurringTemplates(
                    monthKey = state.monthKey.trim(),
                    accountId = state.applyAccountId.trim().ifBlank { null }
                )
            ) {
                is AppResult.Success -> {
                    val status = buildString {
                        append("Applied recurring templates: created ${result.value.created}, skipped ${result.value.skipped}")
                        if (result.value.errors.isNotEmpty()) {
                            append(" (${result.value.errors.size} errors)")
                        }
                    }
                    refreshTemplatesAfterMutation(statusMessage = status)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun resetForm() {
        _uiState.update {
            it.copy(
                formId = "",
                formCategoryId = "",
                formType = "EXPENSE",
                formAmount = "",
                formCurrency = "USD",
                formDayOfMonth = "1",
                formDescription = "",
                formStartMonthKey = "",
                formEndMonthKey = "",
                formIsActive = true,
                statusMessage = null,
                error = null
            )
        }
    }

    private suspend fun refreshTemplatesAfterMutation(statusMessage: String) {
        val state = _uiState.value
        when (
            val reloadResult = recurringRepository.getRecurringTemplates(
                accountId = state.accountId,
                isActive = state.filterIsActive
            )
        ) {
            is AppResult.Success -> _uiState.update {
                it.copy(
                    isMutating = false,
                    templates = reloadResult.value,
                    statusMessage = statusMessage,
                    error = null
                )
            }

            is AppResult.Failure -> _uiState.update {
                it.copy(
                    isMutating = false,
                    error = reloadResult.error.message
                )
            }
        }
    }

    private fun normalizeType(value: String): String {
        val normalized = value.trim().uppercase(Locale.ROOT)
        return if (normalized == "INCOME") "INCOME" else "EXPENSE"
    }

    private fun normalizeCurrency(value: String): String {
        val normalized = value.trim().uppercase(Locale.ROOT)
        return if (normalized.isBlank()) "USD" else normalized
    }

    private companion object {
        val MONTH_KEY_REGEX = Regex("^\\d{4}-\\d{2}$")
    }
}

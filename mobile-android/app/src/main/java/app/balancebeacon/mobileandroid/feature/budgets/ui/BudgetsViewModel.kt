package app.balancebeacon.mobileandroid.feature.budgets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsRepository
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.feature.budgets.data.BudgetsRepository
import app.balancebeacon.mobileandroid.feature.budgets.model.BudgetDto
import app.balancebeacon.mobileandroid.feature.budgets.model.CreateBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.MonthlyIncomeGoalDto
import app.balancebeacon.mobileandroid.feature.budgets.model.QuickBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.UpsertMonthlyIncomeGoalRequest
import app.balancebeacon.mobileandroid.feature.categories.data.CategoriesRepository
import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class BudgetsUiState(
    val isLoading: Boolean = false,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val selectedAccountId: String = "",
    val selectedMonthKey: String = "",
    val items: List<BudgetDto> = emptyList(),
    val incomeGoal: MonthlyIncomeGoalDto? = null,
    val actualIncome: String = "0.00",
    val statusMessage: String? = null,
    val error: String? = null
)

class BudgetsViewModel(
    private val budgetsRepository: BudgetsRepository,
    private val accountsRepository: AccountsRepository,
    private val categoriesRepository: CategoriesRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        viewModelScope.launch {
            val accounts = when (val result = accountsRepository.getAccounts()) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> emptyList()
            }
            val categories = when (val result = categoriesRepository?.getCategories(includeArchived = false)) {
                is AppResult.Success -> result.value.filter { !it.isHolding }
                else -> emptyList()
            }
            val resolvedAccountId = _uiState.value.selectedAccountId.ifBlank {
                accounts.firstOrNull()?.id.orEmpty()
            }
            _uiState.update {
                it.copy(
                    accounts = accounts,
                    categories = categories,
                    selectedAccountId = resolvedAccountId
                )
            }
        }
    }

    fun onAccountIdChanged(value: String) {
        _uiState.update {
            it.copy(
                selectedAccountId = value.trim(),
                statusMessage = null,
                error = null
            )
        }
    }

    fun previousMonth() {
        val current = parseMonthKey(_uiState.value.selectedMonthKey)
        val newMonth = current.minusMonths(1)
        val newKey = newMonth.format(MONTH_KEY_FORMATTER)
        _uiState.update { it.copy(selectedMonthKey = newKey, statusMessage = null, error = null) }
    }

    fun nextMonth() {
        val current = parseMonthKey(_uiState.value.selectedMonthKey)
        val newMonth = current.plusMonths(1)
        val newKey = newMonth.format(MONTH_KEY_FORMATTER)
        _uiState.update { it.copy(selectedMonthKey = newKey, statusMessage = null, error = null) }
    }

    private fun parseMonthKey(key: String): YearMonth {
        val trimmed = key.trim()
        return if (trimmed.isBlank() || !MONTH_KEY_REGEX.matches(trimmed)) {
            YearMonth.now()
        } else {
            YearMonth.parse(trimmed, MONTH_KEY_FORMATTER)
        }
    }

    fun load(accountId: String? = null, month: String? = null) {
        val normalizedMonth = month?.trim()?.ifBlank { null }
            ?: _uiState.value.selectedMonthKey.trim().ifBlank { null }

        if (normalizedMonth != null && !MONTH_KEY_REGEX.matches(normalizedMonth)) {
            _uiState.update { it.copy(error = "Month key must use YYYY-MM format") }
            return
        }

        viewModelScope.launch {
            val resolvedAccountId = resolveAccountId(accountId)
            if (resolvedAccountId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = emptyList(),
                        error = "Account ID is required. Please pick or enter an account."
                    )
                }
                return@launch
            }

            refreshBudgets(
                accountId = resolvedAccountId,
                month = normalizedMonth
            )
        }
    }

    fun createBudget(request: CreateBudgetRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null, error = null) }
            when (val result = budgetsRepository.createBudget(request = request)) {
                is AppResult.Success -> _uiState.update { state ->
                    val updatedItems = upsertBudget(state.items, result.value)
                    state.copy(
                        isLoading = false,
                        selectedAccountId = request.accountId,
                        selectedMonthKey = request.monthKey,
                        items = updatedItems,
                        statusMessage = "Budget saved",
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun deleteBudget(
        accountId: String,
        categoryId: String,
        monthKey: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null, error = null) }
            when (
                val result = budgetsRepository.deleteBudget(
                    accountId = accountId,
                    categoryId = categoryId,
                    monthKey = monthKey
                )
            ) {
                is AppResult.Success -> _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        items = state.items.filterNot { budget ->
                            budget.accountId == accountId &&
                                budget.categoryId == categoryId &&
                                normalizeMonth(budget.monthKey) == normalizeMonth(monthKey)
                        },
                        statusMessage = "Budget removed",
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun createQuickBudget(request: QuickBudgetRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null, error = null) }
            when (val result = budgetsRepository.createQuickBudget(request = request)) {
                is AppResult.Success -> refreshBudgets(
                    accountId = request.accountId,
                    month = request.monthKey,
                    statusMessage = "Quick budget applied"
                )

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun upsertIncomeGoal(
        accountId: String,
        monthKey: String,
        amount: String,
        currency: String,
        setAsDefault: Boolean
    ) {
        val normalizedAccountId = accountId.trim()
        val normalizedMonthKey = monthKey.trim()
        val parsedAmount = amount.toDoubleOrNull()
        val normalizedCurrency = currency.trim().uppercase().ifBlank { "USD" }

        if (normalizedAccountId.isBlank()) {
            _uiState.update { it.copy(error = "Account ID is required") }
            return
        }
        if (!MONTH_KEY_REGEX.matches(normalizedMonthKey)) {
            _uiState.update { it.copy(error = "Month key must use YYYY-MM format") }
            return
        }
        if (parsedAmount == null || parsedAmount <= 0.0) {
            _uiState.update { it.copy(error = "Income goal must be greater than 0") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null, error = null) }
            when (
                val result = budgetsRepository.upsertIncomeGoal(
                    UpsertMonthlyIncomeGoalRequest(
                        accountId = normalizedAccountId,
                        monthKey = normalizedMonthKey,
                        amount = parsedAmount,
                        currency = normalizedCurrency,
                        setAsDefault = setAsDefault
                    )
                )
            ) {
                is AppResult.Success -> refreshBudgets(
                    accountId = normalizedAccountId,
                    month = normalizedMonthKey,
                    statusMessage = if (result.value.isDefault) {
                        "Default income goal saved"
                    } else {
                        "Monthly income goal saved"
                    }
                )

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun deleteIncomeGoal(
        accountId: String,
        monthKey: String
    ) {
        val normalizedAccountId = accountId.trim()
        val normalizedMonthKey = monthKey.trim()

        if (normalizedAccountId.isBlank()) {
            _uiState.update { it.copy(error = "Account ID is required") }
            return
        }
        if (!MONTH_KEY_REGEX.matches(normalizedMonthKey)) {
            _uiState.update { it.copy(error = "Month key must use YYYY-MM format") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null, error = null) }
            when (
                val result = budgetsRepository.deleteIncomeGoal(
                    accountId = normalizedAccountId,
                    monthKey = normalizedMonthKey
                )
            ) {
                is AppResult.Success -> {
                    if (result.value.deleted) {
                        refreshBudgets(
                            accountId = normalizedAccountId,
                            month = normalizedMonthKey,
                            statusMessage = "Monthly income goal removed"
                        )
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Unable to delete monthly income goal"
                            )
                        }
                    }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    private fun upsertBudget(items: List<BudgetDto>, budget: BudgetDto): List<BudgetDto> {
        val existingIndex = items.indexOfFirst {
            it.accountId == budget.accountId &&
                it.categoryId == budget.categoryId &&
                normalizeMonth(it.monthKey) == normalizeMonth(budget.monthKey)
        }

        if (existingIndex == -1) {
            return listOf(budget) + items
        }

        val mutableItems = items.toMutableList()
        mutableItems[existingIndex] = budget
        return mutableItems
    }

    private fun normalizeMonth(value: String): String {
        return if (value.length >= 7) value.substring(0, 7) else value
    }

    private suspend fun refreshBudgets(
        accountId: String,
        month: String? = null,
        statusMessage: String? = null
    ) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        when (val result = budgetsRepository.getBudgets(accountId = accountId, month = month)) {
            is AppResult.Success -> {
                val incomeGoalResult = month?.takeIf { it.isNotBlank() }?.let {
                    budgetsRepository.getIncomeGoalProgress(accountId = accountId, monthKey = it)
                }
                val incomeGoal = when (incomeGoalResult) {
                    is AppResult.Success -> incomeGoalResult.value.incomeGoal
                    else -> null
                }
                val actualIncome = when (incomeGoalResult) {
                    is AppResult.Success -> incomeGoalResult.value.actualIncome
                    else -> "0.00"
                }
                val incomeGoalError = when (incomeGoalResult) {
                    is AppResult.Failure -> incomeGoalResult.error.message
                    else -> null
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedAccountId = accountId,
                        selectedMonthKey = month.orEmpty(),
                        items = result.value,
                        incomeGoal = incomeGoal,
                        actualIncome = actualIncome,
                        statusMessage = statusMessage,
                        error = incomeGoalError
                    )
                }
            }

            is AppResult.Failure -> _uiState.update {
                it.copy(isLoading = false, error = result.error.message)
            }
        }
    }

    private suspend fun resolveAccountId(accountId: String?): String? {
        val accountFromArg = accountId?.trim()?.ifBlank { null }
        if (accountFromArg != null) {
            return accountFromArg
        }

        val accountFromState = _uiState.value.selectedAccountId.trim().ifBlank { null }
        if (accountFromState != null) {
            return accountFromState
        }

        return when (val accounts = accountsRepository.getAccounts()) {
            is AppResult.Success -> {
                val fallbackAccountId = accounts.value.firstOrNull()?.id
                if (fallbackAccountId != null) {
                    _uiState.update { it.copy(selectedAccountId = fallbackAccountId) }
                }
                fallbackAccountId
            }

            is AppResult.Failure -> null
        }
    }

    private companion object {
        val MONTH_KEY_REGEX = Regex("^\\d{4}-\\d{2}$")
        val MONTH_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    }
}

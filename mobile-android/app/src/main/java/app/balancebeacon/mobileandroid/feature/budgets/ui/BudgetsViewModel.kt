package app.balancebeacon.mobileandroid.feature.budgets.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsRepository
import app.balancebeacon.mobileandroid.feature.budgets.data.BudgetsRepository
import app.balancebeacon.mobileandroid.feature.budgets.model.BudgetDto
import app.balancebeacon.mobileandroid.feature.budgets.model.CreateBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.QuickBudgetRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetsUiState(
    val isLoading: Boolean = false,
    val selectedAccountId: String = "",
    val items: List<BudgetDto> = emptyList(),
    val error: String? = null
)

class BudgetsViewModel(
    private val budgetsRepository: BudgetsRepository,
    private val accountsRepository: AccountsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    fun onAccountIdChanged(value: String) {
        _uiState.update { it.copy(selectedAccountId = value.trim(), error = null) }
    }

    fun load(accountId: String? = null, month: String? = null) {
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
                month = month
            )
        }
    }

    fun createBudget(request: CreateBudgetRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = budgetsRepository.createBudget(request = request)) {
                is AppResult.Success -> _uiState.update { state ->
                    val updatedItems = upsertBudget(state.items, result.value)
                    state.copy(
                        isLoading = false,
                        selectedAccountId = request.accountId,
                        items = updatedItems,
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
            _uiState.update { it.copy(isLoading = true, error = null) }
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = budgetsRepository.createQuickBudget(request = request)) {
                is AppResult.Success -> refreshBudgets(
                    accountId = request.accountId,
                    month = request.monthKey
                )
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
        month: String? = null
    ) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        when (val result = budgetsRepository.getBudgets(accountId = accountId, month = month)) {
            is AppResult.Success -> _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedAccountId = accountId,
                    items = result.value,
                    error = null
                )
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
}

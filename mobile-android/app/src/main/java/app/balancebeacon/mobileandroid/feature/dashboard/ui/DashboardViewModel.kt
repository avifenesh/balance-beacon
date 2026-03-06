package app.balancebeacon.mobileandroid.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsRepository
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.feature.dashboard.data.DashboardRepository
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardResponse
import app.balancebeacon.mobileandroid.feature.transactions.data.TransactionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

data class DashboardOverviewUiState(
    val isLoading: Boolean = false,
    val isRefreshingRates: Boolean = false,
    val requestActionInProgressId: String? = null,
    val accounts: List<AccountDto> = emptyList(),
    val accountId: String = "",
    val monthKey: String = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),
    val data: DashboardResponse? = null,
    val insightText: String? = null,
    val statusMessage: String? = null,
    val error: String? = null
)

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val transactionsRepository: TransactionsRepository,
    private val accountsRepository: AccountsRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardOverviewUiState())
    val uiState: StateFlow<DashboardOverviewUiState> = _uiState.asStateFlow()

    private var accountsInitialized = false

    fun initializeAccounts() {
        if (accountsInitialized) return
        accountsInitialized = true

        viewModelScope.launch {
            val accounts = when (val result = accountsRepository?.getAccounts()) {
                is AppResult.Success -> result.value
                else -> emptyList()
            }
            _uiState.update { it.copy(accounts = accounts) }
        }
    }

    fun selectAccount(accountId: String) {
        _uiState.update { it.copy(accountId = accountId, statusMessage = null, error = null) }
        loadDashboard(accountIdOverride = accountId)
    }

    fun onAccountIdChanged(value: String) {
        _uiState.update { it.copy(accountId = value, statusMessage = null, error = null) }
    }

    fun onMonthKeyChanged(value: String) {
        _uiState.update { it.copy(monthKey = value, statusMessage = null, error = null) }
    }

    fun previousMonth() {
        val current = parseMonthKey(_uiState.value.monthKey)
        val newMonth = current.minusMonths(1)
        val newKey = newMonth.format(MONTH_KEY_FORMATTER)
        _uiState.update { it.copy(monthKey = newKey, statusMessage = null, error = null) }
        loadDashboard(monthKeyOverride = newKey)
    }

    fun nextMonth() {
        val current = parseMonthKey(_uiState.value.monthKey)
        val newMonth = current.plusMonths(1)
        val newKey = newMonth.format(MONTH_KEY_FORMATTER)
        _uiState.update { it.copy(monthKey = newKey, statusMessage = null, error = null) }
        loadDashboard(monthKeyOverride = newKey)
    }

    fun loadDashboard(
        accountIdOverride: String? = null,
        monthKeyOverride: String? = null
    ) {
        val accountId = (accountIdOverride ?: _uiState.value.accountId).trim()
        val monthKey = (monthKeyOverride ?: _uiState.value.monthKey).trim().ifBlank { null }

        if (monthKey != null && !MONTH_KEY_REGEX.matches(monthKey)) {
            _uiState.update {
                it.copy(
                    statusMessage = null,
                    error = "Month key must use YYYY-MM format"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (
                val result = dashboardRepository.getDashboard(
                    accountId = accountId.ifBlank { null },
                    month = monthKey
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        requestActionInProgressId = null,
                        accountId = accountId,
                        monthKey = result.value.month,
                        data = result.value,
                        insightText = generateInsight(result.value),
                        statusMessage = null,
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, statusMessage = null, error = result.error.message)
                }
            }
        }
    }

    fun refreshExchangeRates() {
        if (_uiState.value.isRefreshingRates) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRates = true, error = null) }
            when (val result = dashboardRepository.refreshExchangeRates()) {
                is AppResult.Success -> _uiState.update { state ->
                    state.copy(
                        isRefreshingRates = false,
                        data = state.data?.copy(
                            exchangeRateLastUpdate = result.value.updatedAt
                        ),
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        isRefreshingRates = false,
                        error = result.error.message
                    )
                }
            }
        }
    }

    fun approveTransactionRequest(requestId: String) {
        mutateTransactionRequest(
            requestId = requestId,
            action = { transactionsRepository.approveTransactionRequest(requestId) },
            successMessage = "Request approved"
        )
    }

    fun rejectTransactionRequest(requestId: String) {
        mutateTransactionRequest(
            requestId = requestId,
            action = { transactionsRepository.rejectTransactionRequest(requestId) },
            successMessage = "Request rejected"
        )
    }

    private fun mutateTransactionRequest(
        requestId: String,
        action: suspend () -> AppResult<*>,
        successMessage: String
    ) {
        val normalizedRequestId = requestId.trim()
        if (normalizedRequestId.isBlank()) {
            _uiState.update { it.copy(statusMessage = null, error = "Request ID is required") }
            return
        }

        if (_uiState.value.requestActionInProgressId != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    requestActionInProgressId = normalizedRequestId,
                    statusMessage = null,
                    error = null
                )
            }

            when (val result = action()) {
                is AppResult.Success -> _uiState.update { state ->
                    state.copy(
                        requestActionInProgressId = null,
                        data = state.data?.copy(
                            transactionRequests = state.data.transactionRequests.filterNot { request ->
                                request.id == normalizedRequestId
                            }
                        ),
                        statusMessage = successMessage,
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        requestActionInProgressId = null,
                        statusMessage = null,
                        error = result.error.message
                    )
                }
            }
        }
    }

    private fun generateInsight(data: DashboardResponse): String? {
        val stats = data.stats
        if (stats.isEmpty()) return null

        val topExpense = stats.filter { it.variant == "negative" }.maxByOrNull { it.amount }
        val netResult = data.summary.netResult.toDoubleOrNull() ?: 0.0

        return when {
            topExpense != null -> {
                val formatted = NumberFormat.getCurrencyInstance(Locale.US).apply {
                    currency = Currency.getInstance(data.preferredCurrency?.ifBlank { null } ?: "USD")
                }.format(topExpense.amount)
                "Your top expense this month is ${topExpense.label} at $formatted"
            }
            netResult > 0 -> "You're in the green this month! Keep it up."
            netResult < 0 -> "Your expenses are exceeding your income this month."
            else -> "Start tracking to get personalized insights."
        }
    }

    private fun parseMonthKey(key: String): YearMonth {
        val trimmed = key.trim()
        return if (trimmed.isBlank() || !MONTH_KEY_REGEX.matches(trimmed)) {
            YearMonth.now()
        } else {
            YearMonth.parse(trimmed, MONTH_KEY_FORMATTER)
        }
    }

    private companion object {
        val MONTH_KEY_REGEX = Regex("^\\d{4}-\\d{2}$")
        val MONTH_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    }
}

package app.balancebeacon.mobileandroid.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.dashboard.data.DashboardRepository
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardResponse
import app.balancebeacon.mobileandroid.feature.transactions.data.TransactionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardOverviewUiState(
    val isLoading: Boolean = false,
    val requestActionInProgressId: String? = null,
    val accountId: String = "",
    val monthKey: String = "",
    val data: DashboardResponse? = null,
    val statusMessage: String? = null,
    val error: String? = null
)

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val transactionsRepository: TransactionsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardOverviewUiState())
    val uiState: StateFlow<DashboardOverviewUiState> = _uiState.asStateFlow()

    fun onAccountIdChanged(value: String) {
        _uiState.update { it.copy(accountId = value, statusMessage = null, error = null) }
    }

    fun onMonthKeyChanged(value: String) {
        _uiState.update { it.copy(monthKey = value, statusMessage = null, error = null) }
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

    private companion object {
        val MONTH_KEY_REGEX = Regex("^\\d{4}-\\d{2}$")
    }
}

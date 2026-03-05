package app.balancebeacon.mobileandroid.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.dashboard.data.DashboardRepository
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardOverviewUiState(
    val isLoading: Boolean = false,
    val accountId: String = "",
    val monthKey: String = "",
    val data: DashboardResponse? = null,
    val error: String? = null
)

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardOverviewUiState())
    val uiState: StateFlow<DashboardOverviewUiState> = _uiState.asStateFlow()

    fun onAccountIdChanged(value: String) {
        _uiState.update { it.copy(accountId = value, error = null) }
    }

    fun onMonthKeyChanged(value: String) {
        _uiState.update { it.copy(monthKey = value, error = null) }
    }

    fun loadDashboard(
        accountIdOverride: String? = null,
        monthKeyOverride: String? = null
    ) {
        val accountId = (accountIdOverride ?: _uiState.value.accountId).trim()
        val monthKey = (monthKeyOverride ?: _uiState.value.monthKey).trim().ifBlank { null }

        if (monthKey != null && !MONTH_KEY_REGEX.matches(monthKey)) {
            _uiState.update { it.copy(error = "Month key must use YYYY-MM format") }
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
                        accountId = accountId,
                        monthKey = result.value.month,
                        data = result.value,
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    private companion object {
        val MONTH_KEY_REGEX = Regex("^\\d{4}-\\d{2}$")
    }
}

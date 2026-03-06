package app.balancebeacon.mobileandroid.feature.dashboard.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardResponse
import app.balancebeacon.mobileandroid.feature.dashboard.model.ExchangeRateRefreshResponse

class DashboardRepository(
    private val dashboardApi: DashboardApi
) {
    suspend fun getDashboard(
        accountId: String? = null,
        month: String? = null
    ): AppResult<DashboardResponse> {
        val normalizedAccountId = accountId?.trim()?.ifBlank { null }
        val normalizedMonth = month?.trim()?.ifBlank { null }
        return runAppResult {
            dashboardApi.getDashboard(
                accountId = normalizedAccountId,
                month = normalizedMonth
            ).data
        }
    }

    suspend fun refreshExchangeRates(): AppResult<ExchangeRateRefreshResponse> {
        return runAppResult { dashboardApi.refreshExchangeRates().data }
    }
}

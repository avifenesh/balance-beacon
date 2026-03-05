package app.balancebeacon.mobileandroid.feature.dashboard.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardResponse

class DashboardRepository(
    private val dashboardApi: DashboardApi
) {
    suspend fun getDashboard(
        accountId: String,
        month: String? = null
    ): AppResult<DashboardResponse> {
        val normalizedMonth = month?.trim()?.ifBlank { null }
        return runAppResult {
            dashboardApi.getDashboard(
                accountId = accountId.trim(),
                month = normalizedMonth
            )
        }
    }
}

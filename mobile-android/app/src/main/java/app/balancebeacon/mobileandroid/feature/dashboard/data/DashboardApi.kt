package app.balancebeacon.mobileandroid.feature.dashboard.data

import app.balancebeacon.mobileandroid.core.network.ApiEnvelope
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApi {
    @GET("dashboard")
    suspend fun getDashboard(
        @Query("accountId") accountId: String? = null,
        @Query("month") month: String? = null
    ): ApiEnvelope<DashboardResponse>
}

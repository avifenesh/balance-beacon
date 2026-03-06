package app.balancebeacon.mobileandroid.feature.dashboard.data

import app.balancebeacon.mobileandroid.core.network.ApiEnvelope
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardResponse
import app.balancebeacon.mobileandroid.feature.dashboard.model.ExchangeRateRefreshResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DashboardApi {
    @GET("dashboard")
    suspend fun getDashboard(
        @Query("accountId") accountId: String? = null,
        @Query("month") month: String? = null
    ): ApiEnvelope<DashboardResponse>

    @POST("exchange-rates/refresh")
    suspend fun refreshExchangeRates(): ApiEnvelope<ExchangeRateRefreshResponse>
}

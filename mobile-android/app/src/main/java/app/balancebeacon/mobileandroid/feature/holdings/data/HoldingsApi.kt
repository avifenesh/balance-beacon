package app.balancebeacon.mobileandroid.feature.holdings.data

import app.balancebeacon.mobileandroid.feature.holdings.model.CreateHoldingRequest
import app.balancebeacon.mobileandroid.feature.holdings.model.DeleteHoldingResponse
import app.balancebeacon.mobileandroid.feature.holdings.model.HoldingDto
import app.balancebeacon.mobileandroid.feature.holdings.model.HoldingsResponse
import app.balancebeacon.mobileandroid.feature.holdings.model.RefreshHoldingRequest
import app.balancebeacon.mobileandroid.feature.holdings.model.RefreshHoldingResponse
import app.balancebeacon.mobileandroid.feature.holdings.model.UpdateHoldingRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface HoldingsApi {
    @GET("holdings")
    suspend fun getHoldings(
        @Query("accountId") accountId: String
    ): HoldingsResponse

    @POST("holdings")
    suspend fun createHolding(
        @Body request: CreateHoldingRequest
    ): HoldingDto

    @PUT("holdings/{id}")
    suspend fun updateHolding(
        @Path("id") id: String,
        @Body request: UpdateHoldingRequest
    ): HoldingDto

    @DELETE("holdings/{id}")
    suspend fun deleteHolding(
        @Path("id") id: String
    ): DeleteHoldingResponse

    @POST("holdings/refresh")
    suspend fun refreshHoldingPrices(
        @Body request: RefreshHoldingRequest
    ): RefreshHoldingResponse
}

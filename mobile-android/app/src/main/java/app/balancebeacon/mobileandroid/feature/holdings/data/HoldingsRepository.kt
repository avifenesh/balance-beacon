package app.balancebeacon.mobileandroid.feature.holdings.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.holdings.model.CreateHoldingRequest
import app.balancebeacon.mobileandroid.feature.holdings.model.DeleteHoldingResponse
import app.balancebeacon.mobileandroid.feature.holdings.model.HoldingDto
import app.balancebeacon.mobileandroid.feature.holdings.model.RefreshHoldingRequest
import app.balancebeacon.mobileandroid.feature.holdings.model.RefreshHoldingResponse
import app.balancebeacon.mobileandroid.feature.holdings.model.UpdateHoldingRequest

class HoldingsRepository(
    private val holdingsApi: HoldingsApi
) {
    suspend fun getHoldings(accountId: String): AppResult<List<HoldingDto>> {
        return runAppResult { holdingsApi.getHoldings(accountId = accountId).holdings }
    }

    suspend fun createHolding(request: CreateHoldingRequest): AppResult<HoldingDto> {
        return runAppResult { holdingsApi.createHolding(request = request) }
    }

    suspend fun updateHolding(
        id: String,
        request: UpdateHoldingRequest
    ): AppResult<HoldingDto> {
        return runAppResult { holdingsApi.updateHolding(id = id, request = request) }
    }

    suspend fun deleteHolding(id: String): AppResult<DeleteHoldingResponse> {
        return runAppResult { holdingsApi.deleteHolding(id = id) }
    }

    suspend fun refreshHoldingPrices(accountId: String): AppResult<RefreshHoldingResponse> {
        return runAppResult {
            holdingsApi.refreshHoldingPrices(
                request = RefreshHoldingRequest(accountId = accountId)
            )
        }
    }
}

package app.balancebeacon.mobileandroid.feature.dashboard.data

import app.balancebeacon.mobileandroid.core.network.ApiEnvelope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardResponse
import app.balancebeacon.mobileandroid.feature.dashboard.model.ExchangeRateRefreshResponse
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardRepositoryTest {
    @Test
    fun getDashboard_normalizesBlankFiltersAndReturnsData() = runBlocking {
        val expected = DashboardResponse(month = "2026-03")
        val api = FakeDashboardApi(
            response = ApiEnvelope(success = true, data = expected),
            shouldThrow = false
        )
        val repository = DashboardRepository(api)

        val result = repository.getDashboard(
            accountId = "   ",
            month = "   "
        )

        assertTrue(result is AppResult.Success)
        val success = result as AppResult.Success
        assertEquals(expected, success.value)
        assertEquals(null, api.lastAccountId)
        assertEquals(null, api.lastMonth)
    }

    @Test
    fun getDashboard_returnsFailureWhenApiThrows() = runBlocking {
        val api = FakeDashboardApi(
            response = ApiEnvelope(success = true, data = DashboardResponse(month = "2026-03")),
            shouldThrow = true
        )
        val repository = DashboardRepository(api)

        val result = repository.getDashboard(accountId = "acc_1", month = "2026-03")

        assertTrue(result is AppResult.Failure)
        val failure = result as AppResult.Failure
        assertEquals("Network request failed", failure.error.message)
    }

    private class FakeDashboardApi(
        private val response: ApiEnvelope<DashboardResponse>,
        private val shouldThrow: Boolean
    ) : DashboardApi {
        var lastAccountId: String? = null
        var lastMonth: String? = null

        override suspend fun getDashboard(
            accountId: String?,
            month: String?
        ): ApiEnvelope<DashboardResponse> {
            if (shouldThrow) {
                throw IOException("network down")
            }
            lastAccountId = accountId
            lastMonth = month
            return response
        }

        override suspend fun refreshExchangeRates(): ApiEnvelope<ExchangeRateRefreshResponse> {
            if (shouldThrow) {
                throw IOException("network down")
            }
            return ApiEnvelope(
                success = true,
                data = ExchangeRateRefreshResponse(updatedAt = "2026-03-06T12:00:00Z")
            )
        }
    }
}

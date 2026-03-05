package app.balancebeacon.mobileandroid.feature.holdings.ui

import app.balancebeacon.mobileandroid.feature.holdings.model.HoldingDto
import kotlin.math.abs
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldingsAnalyticsTest {
    @Test
    fun holdingMetrics_usesCurrentPriceFallbackWhenMarketValueMissing() {
        val holding = HoldingDto(
            id = "hold_1",
            accountId = "acc_1",
            categoryId = "cat_1",
            symbol = "VOO",
            quantity = "2",
            averageCost = "100",
            currentPrice = JsonPrimitive(125.0),
            marketValue = null
        )

        val metrics = holdingMetrics(holding)

        assertEquals(250.0, metrics.marketValue ?: 0.0, 0.001)
        assertEquals(200.0, metrics.costBasis, 0.001)
        assertEquals(50.0, metrics.gainLoss ?: 0.0, 0.001)
        assertEquals(25.0, metrics.gainLossPercent ?: 0.0, 0.001)
    }

    @Test
    fun holdingMetrics_leavesGainLossNullWithoutPricingData() {
        val holding = HoldingDto(
            id = "hold_2",
            accountId = "acc_1",
            categoryId = "cat_1",
            symbol = "CASH",
            quantity = "10",
            averageCost = "1.5"
        )

        val metrics = holdingMetrics(holding)

        assertNull(metrics.currentPrice)
        assertNull(metrics.marketValue)
        assertNull(metrics.gainLoss)
        assertNull(metrics.gainLossPercent)
        assertEquals(15.0, metrics.costBasis, 0.001)
    }

    @Test
    fun portfolioSnapshot_aggregatesAcrossHoldings() {
        val holdings = listOf(
            HoldingDto(
                id = "hold_1",
                accountId = "acc_1",
                categoryId = "cat_1",
                symbol = "VOO",
                quantity = "2",
                averageCost = "100",
                marketValue = JsonPrimitive("250.5")
            ),
            HoldingDto(
                id = "hold_2",
                accountId = "acc_1",
                categoryId = "cat_2",
                symbol = "VXUS",
                quantity = "1",
                averageCost = "80",
                currentPrice = JsonPrimitive(100.0)
            )
        )

        val snapshot = portfolioSnapshot(holdings)

        assertEquals(350.5, snapshot.totalMarketValue, 0.001)
        assertEquals(280.0, snapshot.totalCostBasis, 0.001)
        assertEquals(70.5, snapshot.totalGainLoss, 0.001)
        assertTrue(abs(snapshot.totalGainLossPercent - 25.178571) < 0.001)
    }
}

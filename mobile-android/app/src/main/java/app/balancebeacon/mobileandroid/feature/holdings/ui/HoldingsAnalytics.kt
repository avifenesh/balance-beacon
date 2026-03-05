package app.balancebeacon.mobileandroid.feature.holdings.ui

import app.balancebeacon.mobileandroid.feature.holdings.model.HoldingDto
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

internal data class PortfolioSnapshot(
    val totalMarketValue: Double,
    val totalCostBasis: Double,
    val totalGainLoss: Double,
    val totalGainLossPercent: Double
)

internal data class HoldingMetrics(
    val quantity: Double,
    val averageCost: Double,
    val currentPrice: Double?,
    val marketValue: Double?,
    val costBasis: Double,
    val gainLoss: Double?,
    val gainLossPercent: Double?
)

internal fun portfolioSnapshot(holdings: List<HoldingDto>): PortfolioSnapshot {
    val metrics = holdings.map(::holdingMetrics)
    val totalMarketValue = metrics.sumOf { it.marketValue ?: 0.0 }
    val totalCostBasis = metrics.sumOf { it.costBasis }
    val totalGainLoss = totalMarketValue - totalCostBasis
    val totalGainLossPercent = if (totalCostBasis > 0.0) {
        (totalGainLoss / totalCostBasis) * 100.0
    } else {
        0.0
    }

    return PortfolioSnapshot(
        totalMarketValue = totalMarketValue,
        totalCostBasis = totalCostBasis,
        totalGainLoss = totalGainLoss,
        totalGainLossPercent = totalGainLossPercent
    )
}

internal fun holdingMetrics(holding: HoldingDto): HoldingMetrics {
    val quantity = holding.quantity.toDoubleOrNull() ?: 0.0
    val averageCost = holding.averageCost.toDoubleOrNull() ?: 0.0
    val currentPrice = holding.currentPrice.toDoubleValue()
    val marketValue = holding.marketValue.toDoubleValue() ?: currentPrice?.let { it * quantity }
    val costBasis = quantity * averageCost
    val gainLoss = if (marketValue != null) marketValue - costBasis else null
    val gainLossPercent = if (gainLoss != null && costBasis > 0.0) {
        (gainLoss / costBasis) * 100.0
    } else {
        null
    }

    return HoldingMetrics(
        quantity = quantity,
        averageCost = averageCost,
        currentPrice = currentPrice,
        marketValue = marketValue,
        costBasis = costBasis,
        gainLoss = gainLoss,
        gainLossPercent = gainLossPercent
    )
}

internal fun JsonElement?.toDoubleValue(): Double? {
    val element = this ?: return null
    if (element is JsonNull) return null
    if (element is JsonPrimitive) {
        return if (element.isString) {
            element.content.toDoubleOrNull()
        } else {
            element.toString().toDoubleOrNull()
        }
    }
    return element.toString().toDoubleOrNull()
}

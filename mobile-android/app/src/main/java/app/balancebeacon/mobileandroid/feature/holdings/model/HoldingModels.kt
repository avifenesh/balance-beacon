package app.balancebeacon.mobileandroid.feature.holdings.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HoldingCategoryDto(
    val id: String,
    val name: String,
    val type: String? = null,
    val color: String? = null
)

@Serializable
data class HoldingDto(
    val id: String,
    val accountId: String,
    val categoryId: String,
    val symbol: String,
    val quantity: String,
    val averageCost: String,
    @SerialName("currency") val currencyCode: String? = null,
    val notes: String? = null,
    val category: HoldingCategoryDto? = null,
    val currentPrice: JsonElement? = null,
    val marketValue: JsonElement? = null,
    val lastPriceUpdate: String? = null,
    val priceAge: String? = null,
    val isStale: Boolean? = null
)

@Serializable
data class HoldingsResponse(
    val holdings: List<HoldingDto> = emptyList()
)

@Serializable
data class CreateHoldingRequest(
    val accountId: String,
    val categoryId: String,
    val symbol: String,
    val quantity: Double,
    val averageCost: Double,
    @SerialName("currency") val currencyCode: String = "USD",
    val notes: String? = null
)

@Serializable
data class UpdateHoldingRequest(
    val quantity: Double,
    val averageCost: Double,
    val notes: String? = null
)

@Serializable
data class DeleteHoldingResponse(
    val id: String? = null,
    val deleted: Boolean? = null,
    val message: String? = null
)

@Serializable
data class RefreshHoldingRequest(
    val accountId: String
)

@Serializable
data class RefreshHoldingResponse(
    val updated: Int = 0,
    val skipped: Int = 0,
    val errors: List<String> = emptyList()
)

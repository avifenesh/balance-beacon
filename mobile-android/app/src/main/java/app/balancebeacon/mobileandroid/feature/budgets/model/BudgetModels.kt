package app.balancebeacon.mobileandroid.feature.budgets.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class BudgetDto(
    val id: String? = null,
    val accountId: String,
    val categoryId: String,
    @SerialName("month") val monthKey: String,
    @SerialName("planned") val amount: String,
    @SerialName("currency") val currencyCode: String? = null,
    val notes: String? = null
)

@Serializable
data class BudgetsResponse(
    val budgets: List<BudgetDto> = emptyList()
)

@Serializable
data class CreateBudgetRequest(
    val accountId: String,
    val categoryId: String,
    val monthKey: String,
    @SerialName("planned") val planned: String,
    @SerialName("currency") val currencyCode: String? = null,
    val notes: String? = null
)

@Serializable
data class QuickBudgetRequest(
    val accountId: String,
    val categoryId: String,
    val monthKey: String,
    val planned: String,
    @SerialName("currency") val currencyCode: String? = null
)

@Serializable
data class QuickBudgetResponse(
    val success: Boolean = true,
    val message: String? = null
)

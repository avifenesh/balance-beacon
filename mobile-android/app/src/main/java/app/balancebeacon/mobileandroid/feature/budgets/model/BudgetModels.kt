package app.balancebeacon.mobileandroid.feature.budgets.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class BudgetCategoryDto(
    val id: String,
    val name: String,
    val type: String? = null,
    val color: String? = null
)

@Serializable
data class BudgetDto(
    val id: String? = null,
    val accountId: String,
    val categoryId: String,
    @SerialName("month") val monthKey: String,
    @SerialName("planned") val amount: String,
    val spent: String? = null,
    val percentUsed: Int? = null,
    @SerialName("currency") val currencyCode: String? = null,
    val notes: String? = null,
    val category: BudgetCategoryDto? = null
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

@Serializable
data class MonthlyIncomeGoalDto(
    val accountId: String,
    val month: String,
    val amount: String,
    val currency: String,
    val notes: String? = null,
    val isDefault: Boolean = false
)

@Serializable
data class MonthlyIncomeGoalProgressResponse(
    val incomeGoal: MonthlyIncomeGoalDto? = null,
    val actualIncome: String = "0.00"
)

@Serializable
data class UpsertMonthlyIncomeGoalRequest(
    val accountId: String,
    val monthKey: String,
    val amount: Double,
    val currency: String,
    val notes: String? = null,
    val setAsDefault: Boolean = false
)

@Serializable
data class DeleteMonthlyIncomeGoalResponse(
    val deleted: Boolean = false
)

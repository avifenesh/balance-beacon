package app.balancebeacon.mobileandroid.feature.dashboard.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardSummaryDto(
    val totalIncome: String = "0.00",
    val totalExpenses: String = "0.00",
    val netResult: String = "0.00"
)

@Serializable
data class DashboardBudgetProgressDto(
    val categoryId: String,
    val categoryName: String,
    val categoryType: String? = null,
    val budgeted: String,
    val spent: String,
    val remaining: String,
    val percentUsed: Int = 0
)

@Serializable
data class DashboardStatCategoryDto(
    val id: String,
    val name: String,
    val planned: Double = 0.0,
    val actual: Double = 0.0,
    val remaining: Double = 0.0
)

@Serializable
data class DashboardStatBreakdownDto(
    val type: String,
    val income: Double? = null,
    val expense: Double? = null,
    val net: Double? = null,
    val actualIncome: Double? = null,
    val actualExpense: Double? = null,
    val expectedRemainingIncome: Double? = null,
    val remainingBudgetedExpense: Double? = null,
    val incomeSource: String? = null,
    val projected: Double? = null,
    val totalPlanned: Double? = null,
    val totalActual: Double? = null,
    val totalRemaining: Double? = null,
    val categories: List<DashboardStatCategoryDto> = emptyList(),
    val plannedIncome: Double? = null,
    val plannedExpense: Double? = null,
    val target: Double? = null
)

@Serializable
data class DashboardStatDto(
    val label: String,
    val amount: Double,
    val variant: String? = null,
    val helper: String? = null,
    val breakdown: DashboardStatBreakdownDto? = null
)

@Serializable
data class DashboardRecentTransactionCategoryDto(
    val name: String,
    val color: String? = null
)

@Serializable
data class DashboardRecentTransactionDto(
    val id: String,
    val amount: String,
    val description: String? = null,
    val date: String,
    val category: DashboardRecentTransactionCategoryDto? = null
)

@Serializable
data class DashboardRequestFromDto(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null
)

@Serializable
data class DashboardRequestCategoryDto(
    val id: String? = null,
    val name: String? = null
)

@Serializable
data class DashboardTransactionRequestDto(
    val id: String,
    val amount: String,
    val currency: String,
    val date: String,
    val description: String? = null,
    val from: DashboardRequestFromDto? = null,
    val category: DashboardRequestCategoryDto? = null
)

@Serializable
data class DashboardComparisonDto(
    val previousMonth: String = "",
    val previousNet: Double = 0.0,
    val change: Double = 0.0
)

@Serializable
data class DashboardHistoryPointDto(
    val month: String,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0
)

@Serializable
data class DashboardPaymentHistoryItemDto(
    val participantId: String,
    val userDisplayName: String,
    val userEmail: String,
    val amount: String,
    val currency: String,
    val paidAt: String,
    val direction: String
)

@Serializable
data class ExchangeRateRefreshResponse(val updatedAt: String)

@Serializable
data class DashboardResponse(
    val month: String,
    val preferredCurrency: String? = null,
    val summary: DashboardSummaryDto = DashboardSummaryDto(),
    val stats: List<DashboardStatDto> = emptyList(),
    val budgetProgress: List<DashboardBudgetProgressDto> = emptyList(),
    val recentTransactions: List<DashboardRecentTransactionDto> = emptyList(),
    val pendingSharedExpenses: Int = 0,
    val transactionRequests: List<DashboardTransactionRequestDto> = emptyList(),
    val paymentHistory: List<DashboardPaymentHistoryItemDto> = emptyList(),
    val comparison: DashboardComparisonDto? = null,
    val history: List<DashboardHistoryPointDto> = emptyList(),
    val exchangeRateLastUpdate: String? = null
)

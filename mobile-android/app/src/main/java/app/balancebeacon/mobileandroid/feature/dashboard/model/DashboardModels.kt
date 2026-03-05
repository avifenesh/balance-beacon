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
    val budgeted: String,
    val spent: String,
    val remaining: String,
    val percentUsed: Int = 0
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
data class DashboardResponse(
    val month: String,
    val summary: DashboardSummaryDto = DashboardSummaryDto(),
    val budgetProgress: List<DashboardBudgetProgressDto> = emptyList(),
    val recentTransactions: List<DashboardRecentTransactionDto> = emptyList(),
    val pendingSharedExpenses: Int = 0,
    val transactionRequests: List<DashboardTransactionRequestDto> = emptyList()
)

package app.balancebeacon.mobileandroid.feature.sharing.model

import kotlinx.serialization.Serializable

@Serializable
data class ShareUserDto(
    val id: String? = null,
    val email: String,
    val displayName: String? = null
)

@Serializable
data class ShareParticipantDto(
    val id: String,
    val shareAmount: String,
    val sharePercentage: String? = null,
    val status: String,
    val paidAt: String? = null,
    val reminderSentAt: String? = null,
    val participant: ShareUserDto? = null
)

@Serializable
data class SharedExpenseTransactionCategoryDto(
    val id: String,
    val name: String
)

@Serializable
data class SharedExpenseTransactionDto(
    val id: String,
    val date: String,
    val description: String? = null,
    val category: SharedExpenseTransactionCategoryDto? = null
)

@Serializable
data class SharedExpenseDto(
    val id: String,
    val transactionId: String,
    val splitType: String,
    val description: String? = null,
    val totalAmount: String,
    val currency: String,
    val createdAt: String,
    val transaction: SharedExpenseTransactionDto? = null,
    val participants: List<ShareParticipantDto> = emptyList()
)

@Serializable
data class SharedWithMeParticipationDto(
    val id: String,
    val shareAmount: String,
    val sharePercentage: String? = null,
    val status: String,
    val paidAt: String? = null,
    val sharedExpense: SharedExpenseDto? = null
)

@Serializable
data class SettlementBalanceDto(
    val userId: String,
    val userEmail: String,
    val userDisplayName: String? = null,
    val currency: String,
    val youOwe: String,
    val theyOwe: String,
    val netBalance: String
)

@Serializable
data class PaymentHistoryItemDto(
    val participantId: String,
    val userDisplayName: String,
    val userEmail: String,
    val amount: String,
    val currency: String,
    val paidAt: String,
    val direction: String
)

@Serializable
data class SharingResponse(
    val sharedExpenses: List<SharedExpenseDto> = emptyList(),
    val expensesSharedWithMe: List<SharedWithMeParticipationDto> = emptyList(),
    val settlementBalances: List<SettlementBalanceDto> = emptyList(),
    val paymentHistory: List<PaymentHistoryItemDto> = emptyList()
)

@Serializable
data class CreateSharedExpenseParticipantRequest(
    val email: String,
    val shareAmount: Double? = null,
    val sharePercentage: Double? = null
)

@Serializable
data class CreateSharedExpenseRequest(
    val transactionId: String,
    val splitType: String,
    val description: String? = null,
    val participants: List<CreateSharedExpenseParticipantRequest>
)

@Serializable
data class MarkPaidResponse(
    val id: String,
    val status: String,
    val paidAt: String? = null
)

@Serializable
data class DeclineShareResponse(
    val id: String,
    val status: String
)

@Serializable
data class UserLookupResponse(
    val user: ShareUserDto
)

@Serializable
data class SettleAllRequest(
    val targetUserId: String,
    val currency: String
)

@Serializable
data class SettleAllResponse(
    val settledCount: Int = 0
)

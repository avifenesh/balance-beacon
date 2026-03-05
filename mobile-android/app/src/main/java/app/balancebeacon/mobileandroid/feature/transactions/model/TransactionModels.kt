package app.balancebeacon.mobileandroid.feature.transactions.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: String,
    val amount: String,
    val type: String,
    val description: String? = null,
    val categoryId: String? = null,
    val accountId: String,
    val date: String,
    @SerialName("currency") val currencyCode: String? = null
)

@Serializable
data class TransactionsResponse(
    val transactions: List<TransactionDto> = emptyList(),
    val total: Int = 0
)

@Serializable
data class CreateTransactionRequest(
    val amount: String,
    val type: String,
    val description: String? = null,
    val categoryId: String? = null,
    val accountId: String,
    val date: String
)

@Serializable
data class UpdateTransactionRequest(
    val amount: String,
    val type: String,
    val description: String? = null,
    val categoryId: String? = null,
    val accountId: String,
    val date: String,
    @SerialName("currency") val currencyCode: String? = null,
    val isRecurring: Boolean? = null
)

@Serializable
data class DeleteTransactionResponse(
    val id: String? = null,
    val deleted: Boolean? = null,
    val message: String? = null
)

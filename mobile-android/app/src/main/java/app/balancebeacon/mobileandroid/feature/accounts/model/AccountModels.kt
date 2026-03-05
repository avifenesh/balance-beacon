package app.balancebeacon.mobileandroid.feature.accounts.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val type: String,
    val preferredCurrency: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val description: String? = null,
    val balance: Double = 0.0
)

@Serializable
data class AccountsResponse(
    val accounts: List<AccountDto> = emptyList()
)

@Serializable
data class CreateAccountRequest(
    val name: String,
    val type: String,
    val color: String? = null,
    val preferredCurrency: String? = null
)

@Serializable
data class UpdateAccountRequest(
    val name: String,
    val type: String? = null,
    val color: String? = null,
    val preferredCurrency: String? = null
)

@Serializable
data class DeleteAccountResponse(
    val deleted: Boolean
)

@Serializable
data class ActivateAccountResponse(
    val activeAccountId: String
)

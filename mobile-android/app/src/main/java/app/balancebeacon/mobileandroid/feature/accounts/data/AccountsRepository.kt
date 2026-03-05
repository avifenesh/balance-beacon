package app.balancebeacon.mobileandroid.feature.accounts.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.feature.accounts.model.ActivateAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.CreateAccountRequest
import app.balancebeacon.mobileandroid.feature.accounts.model.DeleteAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.UpdateAccountRequest

class AccountsRepository(
    private val accountsApi: AccountsApi
) {
    suspend fun getAccounts(): AppResult<List<AccountDto>> {
        return runAppResult { accountsApi.getAccounts().accounts }
    }

    suspend fun createAccount(
        name: String,
        type: String,
        color: String? = null,
        preferredCurrency: String? = null
    ): AppResult<AccountDto> {
        return runAppResult {
            accountsApi.createAccount(
                CreateAccountRequest(
                    name = name.trim(),
                    type = type,
                    color = color,
                    preferredCurrency = preferredCurrency
                )
            )
        }
    }

    suspend fun updateAccount(
        id: String,
        name: String,
        type: String? = null,
        color: String? = null,
        preferredCurrency: String? = null
    ): AppResult<AccountDto> {
        return runAppResult {
            accountsApi.updateAccount(
                id = id,
                request = UpdateAccountRequest(
                    name = name.trim(),
                    type = type,
                    color = color,
                    preferredCurrency = preferredCurrency
                )
            )
        }
    }

    suspend fun deleteAccount(id: String): AppResult<DeleteAccountResponse> {
        return runAppResult { accountsApi.deleteAccount(id = id) }
    }

    suspend fun activateAccount(id: String): AppResult<ActivateAccountResponse> {
        return runAppResult { accountsApi.activateAccount(id = id) }
    }
}

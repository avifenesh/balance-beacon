package app.balancebeacon.mobileandroid.feature.accounts.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountsResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.ActivateAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.CreateAccountRequest
import app.balancebeacon.mobileandroid.feature.accounts.model.DeleteAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.UpdateAccountRequest
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountsRepositoryTest {
    @Test
    fun createAccount_trimsNameBeforeRequest() = runBlocking {
        val api = FakeAccountsApi()
        val repository = AccountsRepository(api)

        val result = repository.createAccount(
            name = "  Team Wallet  ",
            type = "cash",
            color = "#00AA00",
            preferredCurrency = "USD"
        )

        assertTrue(result is AppResult.Success)
        assertEquals("Team Wallet", api.lastCreateRequest?.name)
    }

    @Test
    fun updateAccount_trimsNameBeforeRequest() = runBlocking {
        val api = FakeAccountsApi()
        val repository = AccountsRepository(api)

        val result = repository.updateAccount(
            id = "acc_1",
            name = "  Updated Name  ",
            type = "bank"
        )

        assertTrue(result is AppResult.Success)
        assertEquals("Updated Name", api.lastUpdateRequest?.name)
    }

    @Test
    fun getAccounts_returnsFailureWhenApiThrows() = runBlocking {
        val api = FakeAccountsApi(shouldThrowOnGet = true)
        val repository = AccountsRepository(api)

        val result = repository.getAccounts()

        assertTrue(result is AppResult.Failure)
        val failure = result as AppResult.Failure
        assertEquals("Network request failed", failure.error.message)
    }

    private class FakeAccountsApi(
        private val shouldThrowOnGet: Boolean = false
    ) : AccountsApi {
        var lastCreateRequest: CreateAccountRequest? = null
        var lastUpdateRequest: UpdateAccountRequest? = null

        override suspend fun getAccounts(): AccountsResponse {
            if (shouldThrowOnGet) {
                throw IOException("network down")
            }
            return AccountsResponse(
                accounts = listOf(
                    AccountDto(
                        id = "acc_1",
                        name = "Main",
                        type = "cash"
                    )
                )
            )
        }

        override suspend fun createAccount(request: CreateAccountRequest): AccountDto {
            lastCreateRequest = request
            return AccountDto(
                id = "acc_created",
                name = request.name,
                type = request.type,
                preferredCurrency = request.preferredCurrency,
                color = request.color
            )
        }

        override suspend fun updateAccount(id: String, request: UpdateAccountRequest): AccountDto {
            lastUpdateRequest = request
            return AccountDto(
                id = id,
                name = request.name,
                type = request.type ?: "cash",
                preferredCurrency = request.preferredCurrency,
                color = request.color
            )
        }

        override suspend fun deleteAccount(id: String): DeleteAccountResponse {
            return DeleteAccountResponse(deleted = true)
        }

        override suspend fun activateAccount(
            id: String,
            body: Map<String, String>
        ): ActivateAccountResponse {
            return ActivateAccountResponse(activeAccountId = id)
        }
    }
}

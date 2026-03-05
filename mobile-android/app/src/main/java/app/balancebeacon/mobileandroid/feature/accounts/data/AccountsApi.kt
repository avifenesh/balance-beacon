package app.balancebeacon.mobileandroid.feature.accounts.data

import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.feature.accounts.model.ActivateAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountsResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.CreateAccountRequest
import app.balancebeacon.mobileandroid.feature.accounts.model.DeleteAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.UpdateAccountRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AccountsApi {
    @GET("accounts")
    suspend fun getAccounts(): AccountsResponse

    @POST("accounts")
    suspend fun createAccount(
        @Body request: CreateAccountRequest
    ): AccountDto

    @PUT("accounts/{id}")
    suspend fun updateAccount(
        @Path("id") id: String,
        @Body request: UpdateAccountRequest
    ): AccountDto

    @DELETE("accounts/{id}")
    suspend fun deleteAccount(
        @Path("id") id: String
    ): DeleteAccountResponse

    @PATCH("accounts/{id}/activate")
    suspend fun activateAccount(
        @Path("id") id: String,
        @Body body: Map<String, String> = emptyMap()
    ): ActivateAccountResponse
}

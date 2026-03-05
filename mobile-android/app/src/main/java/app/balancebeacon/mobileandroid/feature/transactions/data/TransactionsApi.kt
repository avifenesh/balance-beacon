package app.balancebeacon.mobileandroid.feature.transactions.data

import app.balancebeacon.mobileandroid.feature.transactions.model.CreateTransactionRequest
import app.balancebeacon.mobileandroid.feature.transactions.model.DeleteTransactionResponse
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionDto
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionRequestActionResponse
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionsResponse
import app.balancebeacon.mobileandroid.feature.transactions.model.UpdateTransactionRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionsApi {
    @GET("transactions")
    suspend fun getTransactions(
        @Query("accountId") accountId: String? = null,
        @Query("month") month: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("type") type: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): TransactionsResponse

    @POST("transactions")
    suspend fun createTransaction(
        @Body request: CreateTransactionRequest
    ): TransactionDto

    @GET("transactions/{id}")
    suspend fun getTransactionById(
        @Path("id") id: String
    ): TransactionDto

    @PUT("transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body request: UpdateTransactionRequest
    ): TransactionDto

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: String
    ): DeleteTransactionResponse

    @POST("transactions/requests/{id}/approve")
    suspend fun approveTransactionRequest(
        @Path("id") id: String
    ): TransactionRequestActionResponse

    @POST("transactions/requests/{id}/reject")
    suspend fun rejectTransactionRequest(
        @Path("id") id: String
    ): TransactionRequestActionResponse
}

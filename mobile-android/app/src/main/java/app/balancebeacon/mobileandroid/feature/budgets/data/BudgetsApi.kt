package app.balancebeacon.mobileandroid.feature.budgets.data

import app.balancebeacon.mobileandroid.feature.auth.model.MessageResponse
import app.balancebeacon.mobileandroid.feature.budgets.model.BudgetDto
import app.balancebeacon.mobileandroid.feature.budgets.model.BudgetsResponse
import app.balancebeacon.mobileandroid.feature.budgets.model.CreateBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.QuickBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.QuickBudgetResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface BudgetsApi {
    @GET("budgets")
    suspend fun getBudgets(
        @Query("accountId") accountId: String? = null,
        @Query("month") month: String? = null
    ): BudgetsResponse

    @POST("budgets")
    suspend fun createBudget(
        @Body request: CreateBudgetRequest
    ): BudgetDto

    @POST("budgets/quick")
    suspend fun createQuickBudget(
        @Body request: QuickBudgetRequest
    ): QuickBudgetResponse

    @DELETE("budgets")
    suspend fun deleteBudget(
        @Query("accountId") accountId: String,
        @Query("categoryId") categoryId: String,
        @Query("monthKey") monthKey: String
    ): MessageResponse
}

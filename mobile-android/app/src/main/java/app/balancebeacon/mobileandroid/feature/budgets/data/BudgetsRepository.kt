package app.balancebeacon.mobileandroid.feature.budgets.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.auth.model.MessageResponse
import app.balancebeacon.mobileandroid.feature.budgets.model.BudgetDto
import app.balancebeacon.mobileandroid.feature.budgets.model.CreateBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.QuickBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.QuickBudgetResponse

class BudgetsRepository(
    private val budgetsApi: BudgetsApi
) {
    suspend fun getBudgets(accountId: String? = null, month: String? = null): AppResult<List<BudgetDto>> {
        return runAppResult {
            budgetsApi.getBudgets(accountId = accountId, month = month).budgets
        }
    }

    suspend fun createBudget(request: CreateBudgetRequest): AppResult<BudgetDto> {
        return runAppResult { budgetsApi.createBudget(request = request) }
    }

    suspend fun deleteBudget(
        accountId: String,
        categoryId: String,
        monthKey: String
    ): AppResult<MessageResponse> {
        return runAppResult {
            budgetsApi.deleteBudget(
                accountId = accountId,
                categoryId = categoryId,
                monthKey = monthKey
            )
        }
    }

    suspend fun createQuickBudget(request: QuickBudgetRequest): AppResult<QuickBudgetResponse> {
        return runAppResult { budgetsApi.createQuickBudget(request = request) }
    }
}

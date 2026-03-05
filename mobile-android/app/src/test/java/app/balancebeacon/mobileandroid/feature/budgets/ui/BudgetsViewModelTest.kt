package app.balancebeacon.mobileandroid.feature.budgets.ui

import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsApi
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsRepository
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountsResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.ActivateAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.CreateAccountRequest
import app.balancebeacon.mobileandroid.feature.accounts.model.DeleteAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.UpdateAccountRequest
import app.balancebeacon.mobileandroid.feature.auth.model.MessageResponse
import app.balancebeacon.mobileandroid.feature.budgets.data.BudgetsApi
import app.balancebeacon.mobileandroid.feature.budgets.data.BudgetsRepository
import app.balancebeacon.mobileandroid.feature.budgets.model.BudgetDto
import app.balancebeacon.mobileandroid.feature.budgets.model.BudgetsResponse
import app.balancebeacon.mobileandroid.feature.budgets.model.CreateBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.DeleteMonthlyIncomeGoalResponse
import app.balancebeacon.mobileandroid.feature.budgets.model.MonthlyIncomeGoalDto
import app.balancebeacon.mobileandroid.feature.budgets.model.MonthlyIncomeGoalProgressResponse
import app.balancebeacon.mobileandroid.feature.budgets.model.QuickBudgetRequest
import app.balancebeacon.mobileandroid.feature.budgets.model.QuickBudgetResponse
import app.balancebeacon.mobileandroid.feature.budgets.model.UpsertMonthlyIncomeGoalRequest
import app.balancebeacon.mobileandroid.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_resolvesFirstAccountWhenAccountIdMissing() = runTest {
        val budgetsApi = FakeBudgetsApi()
        val accountsApi = FakeAccountsApi(
            accounts = listOf(
                AccountDto(
                    id = "acc_primary",
                    name = "Primary",
                    type = "SELF"
                )
            )
        )
        val viewModel = BudgetsViewModel(
            budgetsRepository = BudgetsRepository(budgetsApi),
            accountsRepository = AccountsRepository(accountsApi)
        )

        viewModel.load()
        advanceUntilIdle()

        assertEquals("acc_primary", budgetsApi.lastGetAccountId)
        assertEquals("acc_primary", viewModel.uiState.value.selectedAccountId)
        assertTrue(viewModel.uiState.value.items.isNotEmpty())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun load_prefersTypedAccountIdOverFallbackLookup() = runTest {
        val budgetsApi = FakeBudgetsApi()
        val accountsApi = FakeAccountsApi(
            accounts = listOf(
                AccountDto(
                    id = "acc_primary",
                    name = "Primary",
                    type = "SELF"
                )
            )
        )
        val viewModel = BudgetsViewModel(
            budgetsRepository = BudgetsRepository(budgetsApi),
            accountsRepository = AccountsRepository(accountsApi)
        )

        viewModel.onAccountIdChanged("acc_typed")
        viewModel.load()
        advanceUntilIdle()

        assertEquals("acc_typed", budgetsApi.lastGetAccountId)
        assertEquals(0, accountsApi.getAccountsCalls)
    }

    private class FakeBudgetsApi : BudgetsApi {
        var lastGetAccountId: String? = null

        override suspend fun getBudgets(
            accountId: String?,
            month: String?
        ): BudgetsResponse {
            lastGetAccountId = accountId
            return BudgetsResponse(
                budgets = listOf(
                    BudgetDto(
                        id = "budget_1",
                        accountId = accountId.orEmpty(),
                        categoryId = "cat_1",
                        monthKey = "2026-03-01",
                        amount = "250.00",
                        currencyCode = "USD"
                    )
                )
            )
        }

        override suspend fun createBudget(request: CreateBudgetRequest): BudgetDto {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun createQuickBudget(request: QuickBudgetRequest): QuickBudgetResponse {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun getIncomeGoalProgress(
            accountId: String,
            monthKey: String
        ): MonthlyIncomeGoalProgressResponse {
            return MonthlyIncomeGoalProgressResponse(
                incomeGoal = MonthlyIncomeGoalDto(
                    accountId = accountId,
                    month = "$monthKey-01T00:00:00.000Z",
                    amount = "1000.00",
                    currency = "USD"
                ),
                actualIncome = "0.00"
            )
        }

        override suspend fun upsertIncomeGoal(
            request: UpsertMonthlyIncomeGoalRequest
        ): MonthlyIncomeGoalDto {
            return MonthlyIncomeGoalDto(
                accountId = request.accountId,
                month = "${request.monthKey}-01T00:00:00.000Z",
                amount = request.amount.toString(),
                currency = request.currency,
                isDefault = request.setAsDefault
            )
        }

        override suspend fun deleteIncomeGoal(
            accountId: String,
            monthKey: String
        ): DeleteMonthlyIncomeGoalResponse {
            return DeleteMonthlyIncomeGoalResponse(deleted = true)
        }

        override suspend fun deleteBudget(
            accountId: String,
            categoryId: String,
            monthKey: String
        ): MessageResponse {
            throw UnsupportedOperationException("Not needed")
        }
    }

    private class FakeAccountsApi(
        private val accounts: List<AccountDto>
    ) : AccountsApi {
        var getAccountsCalls: Int = 0

        override suspend fun getAccounts(): AccountsResponse {
            getAccountsCalls += 1
            return AccountsResponse(accounts = accounts)
        }

        override suspend fun createAccount(request: CreateAccountRequest): AccountDto {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun updateAccount(
            id: String,
            request: UpdateAccountRequest
        ): AccountDto {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun deleteAccount(id: String): DeleteAccountResponse {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun activateAccount(
            id: String,
            body: Map<String, String>
        ): ActivateAccountResponse {
            throw UnsupportedOperationException("Not needed")
        }
    }
}

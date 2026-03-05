package app.balancebeacon.mobileandroid.feature.recurring.ui

import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsApi
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsRepository
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountsResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.ActivateAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.CreateAccountRequest
import app.balancebeacon.mobileandroid.feature.accounts.model.DeleteAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.UpdateAccountRequest
import app.balancebeacon.mobileandroid.feature.categories.data.CategoriesApi
import app.balancebeacon.mobileandroid.feature.categories.data.CategoriesRepository
import app.balancebeacon.mobileandroid.feature.categories.model.ArchiveCategoryRequest
import app.balancebeacon.mobileandroid.feature.categories.model.ArchiveCategoryResponse
import app.balancebeacon.mobileandroid.feature.categories.model.BulkCreateCategoriesRequest
import app.balancebeacon.mobileandroid.feature.categories.model.BulkCreateCategoriesResponse
import app.balancebeacon.mobileandroid.feature.categories.model.CategoriesResponse
import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.feature.categories.model.CreateCategoryRequest
import app.balancebeacon.mobileandroid.feature.categories.model.UpdateCategoryRequest
import app.balancebeacon.mobileandroid.feature.recurring.data.RecurringApi
import app.balancebeacon.mobileandroid.feature.recurring.data.RecurringRepository
import app.balancebeacon.mobileandroid.feature.recurring.model.ApplyRecurringRequest
import app.balancebeacon.mobileandroid.feature.recurring.model.ApplyRecurringResponse
import app.balancebeacon.mobileandroid.feature.recurring.model.DeleteRecurringResponse
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringCategoryDto
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringTemplateDto
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringTemplatesResponse
import app.balancebeacon.mobileandroid.feature.recurring.model.ToggleRecurringRequest
import app.balancebeacon.mobileandroid.feature.recurring.model.ToggleRecurringResponse
import app.balancebeacon.mobileandroid.feature.recurring.model.UpsertRecurringTemplateRequest
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
class RecurringViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialize_loadsAccountsCategoriesAndTemplates() = runTest {
        val api = FakeRecurringApi()
        val viewModel = createViewModel(api)

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals("acc_1", viewModel.uiState.value.accountId)
        assertEquals("cat_1", viewModel.uiState.value.formCategoryId)
        assertEquals(2, viewModel.uiState.value.accounts.size)
        assertEquals(3, viewModel.uiState.value.categories.size)
        assertEquals(2, viewModel.uiState.value.templates.size)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun deleteTemplate_removesTemplateAndShowsStatus() = runTest {
        val api = FakeRecurringApi()
        val viewModel = createViewModel(api)

        viewModel.initialize()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.templates.size)

        viewModel.deleteTemplate("tmpl_1")
        advanceUntilIdle()

        assertEquals(listOf("tmpl_1"), api.deletedTemplateIds)
        assertEquals(1, viewModel.uiState.value.templates.size)
        assertEquals("tmpl_2", viewModel.uiState.value.templates.first().id)
        assertEquals("Template deleted", viewModel.uiState.value.statusMessage)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun deleteTemplate_surfacesApiError() = runTest {
        val api = FakeRecurringApi(shouldFailDelete = true)
        val viewModel = createViewModel(api)

        viewModel.initialize()
        advanceUntilIdle()

        viewModel.deleteTemplate("tmpl_1")
        advanceUntilIdle()

        assertEquals("Delete failed", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.isMutating.not())
    }

    private fun createViewModel(api: FakeRecurringApi): RecurringViewModel {
        return RecurringViewModel(
            recurringRepository = RecurringRepository(api),
            accountsRepository = AccountsRepository(FakeAccountsApi()),
            categoriesRepository = CategoriesRepository(FakeCategoriesApi())
        )
    }

    private class FakeRecurringApi(
        private val shouldFailDelete: Boolean = false
    ) : RecurringApi {
        private val templates = mutableListOf(
            RecurringTemplateDto(
                id = "tmpl_1",
                accountId = "acc_1",
                categoryId = "cat_1",
                type = "EXPENSE",
                amount = "35.00",
                currency = "USD",
                dayOfMonth = 5,
                description = "Coffee",
                startMonth = "2026-01-01",
                endMonth = null,
                isActive = true,
                category = RecurringCategoryDto(id = "cat_1", name = "Coffee")
            ),
            RecurringTemplateDto(
                id = "tmpl_2",
                accountId = "acc_1",
                categoryId = "cat_2",
                type = "INCOME",
                amount = "2000.00",
                currency = "USD",
                dayOfMonth = 1,
                description = "Salary",
                startMonth = "2026-01-01",
                endMonth = null,
                isActive = true,
                category = RecurringCategoryDto(id = "cat_2", name = "Salary")
            )
        )

        val deletedTemplateIds = mutableListOf<String>()

        override suspend fun getRecurringTemplates(
            accountId: String,
            isActive: Boolean?
        ): RecurringTemplatesResponse {
            val filtered = templates.filter { template ->
                template.accountId == accountId && (isActive == null || template.isActive == isActive)
            }
            return RecurringTemplatesResponse(recurringTemplates = filtered)
        }

        override suspend fun upsertRecurringTemplate(
            request: UpsertRecurringTemplateRequest
        ): RecurringTemplateDto {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun toggleRecurringTemplate(
            id: String,
            request: ToggleRecurringRequest
        ): ToggleRecurringResponse {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun deleteRecurringTemplate(id: String): DeleteRecurringResponse {
            if (shouldFailDelete) {
                throw IllegalStateException("Delete failed")
            }
            deletedTemplateIds += id
            templates.removeAll { it.id == id }
            return DeleteRecurringResponse(id = id, deleted = true)
        }

        override suspend fun applyRecurringTemplates(
            request: ApplyRecurringRequest
        ): ApplyRecurringResponse {
            throw UnsupportedOperationException("Not needed")
        }
    }

    private class FakeAccountsApi : AccountsApi {
        override suspend fun getAccounts(): AccountsResponse {
            return AccountsResponse(
                accounts = listOf(
                    AccountDto(id = "acc_1", name = "Main account", type = "SELF"),
                    AccountDto(id = "acc_2", name = "Side account", type = "SELF")
                )
            )
        }

        override suspend fun createAccount(request: CreateAccountRequest): AccountDto {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun updateAccount(id: String, request: UpdateAccountRequest): AccountDto {
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

    private class FakeCategoriesApi : CategoriesApi {
        override suspend fun getCategories(type: String?, includeArchived: Boolean): CategoriesResponse {
            return CategoriesResponse(
                categories = listOf(
                    CategoryDto(id = "cat_1", name = "Coffee", type = "EXPENSE"),
                    CategoryDto(id = "cat_2", name = "Salary", type = "INCOME"),
                    CategoryDto(id = "cat_3", name = "Rent", type = "EXPENSE")
                )
            )
        }

        override suspend fun createCategory(request: CreateCategoryRequest): CategoryDto {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun bulkCreateCategories(request: BulkCreateCategoriesRequest): BulkCreateCategoriesResponse {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun updateCategory(id: String, request: UpdateCategoryRequest): CategoryDto {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun archiveCategory(id: String, request: ArchiveCategoryRequest): ArchiveCategoryResponse {
            throw UnsupportedOperationException("Not needed")
        }
    }
}

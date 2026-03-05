package app.balancebeacon.mobileandroid.feature.recurring.ui

import app.balancebeacon.mobileandroid.feature.recurring.data.RecurringApi
import app.balancebeacon.mobileandroid.feature.recurring.data.RecurringRepository
import app.balancebeacon.mobileandroid.feature.recurring.model.ApplyRecurringRequest
import app.balancebeacon.mobileandroid.feature.recurring.model.ApplyRecurringResponse
import app.balancebeacon.mobileandroid.feature.recurring.model.DeleteRecurringResponse
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
    fun deleteTemplate_removesTemplateAndShowsStatus() = runTest {
        val api = FakeRecurringApi()
        val viewModel = RecurringViewModel(RecurringRepository(api))

        viewModel.onAccountIdChanged("acc_1")
        viewModel.load()
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
        val viewModel = RecurringViewModel(RecurringRepository(api))

        viewModel.onAccountIdChanged("acc_1")
        viewModel.load()
        advanceUntilIdle()

        viewModel.deleteTemplate("tmpl_1")
        advanceUntilIdle()

        assertEquals("Delete failed", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.isMutating.not())
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
                startMonth = "2026-01-01T00:00:00.000Z",
                endMonth = null,
                isActive = true
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
                startMonth = "2026-01-01T00:00:00.000Z",
                endMonth = null,
                isActive = true
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
}

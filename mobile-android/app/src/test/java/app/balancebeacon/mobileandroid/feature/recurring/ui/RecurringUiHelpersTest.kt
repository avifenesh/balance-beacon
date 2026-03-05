package app.balancebeacon.mobileandroid.feature.recurring.ui

import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringTemplateDto
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringUiHelpersTest {
    @Test
    fun filterRecurringTemplates_hidesInactiveByDefault() {
        val filtered = filterRecurringTemplates(
            templates = sampleTemplates(),
            typeFilter = RecurringTypeFilter.ALL,
            includeInactive = false
        )

        assertEquals(listOf("rent", "salary"), filtered.map { it.id })
    }

    @Test
    fun filterRecurringTemplates_filtersByTypeWhenRequested() {
        val filtered = filterRecurringTemplates(
            templates = sampleTemplates(),
            typeFilter = RecurringTypeFilter.EXPENSE,
            includeInactive = true
        )

        assertEquals(listOf("rent", "gym"), filtered.map { it.id })
    }

    @Test
    fun filterRecurringCategories_returnsMatchingNonHoldingCategories() {
        val filtered = filterRecurringCategories(
            categories = listOf(
                CategoryDto(id = "expense_1", name = "Rent", type = "EXPENSE"),
                CategoryDto(id = "expense_2", name = "Brokerage", type = "EXPENSE", isHolding = true),
                CategoryDto(id = "income_1", name = "Salary", type = "INCOME")
            ),
            type = "expense"
        )

        assertEquals(listOf("expense_1"), filtered.map { it.id })
    }

    @Test
    fun buildRecurringSummary_countsOnlyActiveTemplates() {
        val summary = buildRecurringSummary(sampleTemplates())

        assertEquals(2, summary.activeCount)
        assertEquals(1200.0, summary.monthlyExpenseTotal, 0.001)
        assertEquals(4200.0, summary.monthlyIncomeTotal, 0.001)
    }

    private fun sampleTemplates(): List<RecurringTemplateDto> {
        return listOf(
            RecurringTemplateDto(
                id = "rent",
                accountId = "acc_1",
                categoryId = "cat_rent",
                type = "EXPENSE",
                amount = "1200.00",
                currency = "USD",
                dayOfMonth = 1,
                description = "Rent",
                startMonth = "2026-01-01",
                endMonth = null,
                isActive = true
            ),
            RecurringTemplateDto(
                id = "salary",
                accountId = "acc_1",
                categoryId = "cat_salary",
                type = "INCOME",
                amount = "4200.00",
                currency = "USD",
                dayOfMonth = 2,
                description = "Salary",
                startMonth = "2026-01-01",
                endMonth = null,
                isActive = true
            ),
            RecurringTemplateDto(
                id = "gym",
                accountId = "acc_1",
                categoryId = "cat_gym",
                type = "EXPENSE",
                amount = "55.00",
                currency = "USD",
                dayOfMonth = 12,
                description = "Gym",
                startMonth = "2026-01-01",
                endMonth = null,
                isActive = false
            )
        )
    }
}

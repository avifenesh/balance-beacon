package app.balancebeacon.mobileandroid.feature.recurring.ui

import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringTemplateDto
import java.util.Locale

enum class RecurringTypeFilter {
    ALL,
    EXPENSE,
    INCOME
}

data class RecurringSummary(
    val activeCount: Int,
    val monthlyExpenseTotal: Double,
    val monthlyIncomeTotal: Double
)

fun filterRecurringTemplates(
    templates: List<RecurringTemplateDto>,
    typeFilter: RecurringTypeFilter,
    includeInactive: Boolean
): List<RecurringTemplateDto> {
    return templates.filter { template ->
        val matchesType = when (typeFilter) {
            RecurringTypeFilter.ALL -> true
            RecurringTypeFilter.EXPENSE -> template.type.equals("EXPENSE", ignoreCase = true)
            RecurringTypeFilter.INCOME -> template.type.equals("INCOME", ignoreCase = true)
        }

        matchesType && (includeInactive || template.isActive)
    }
}

fun filterRecurringCategories(
    categories: List<CategoryDto>,
    type: String
): List<CategoryDto> {
    val normalizedType = type.trim().uppercase(Locale.ROOT)
    return categories
        .filter { category ->
            !category.isArchived &&
                !category.isHolding &&
                category.type.equals(normalizedType, ignoreCase = true)
        }
        .sortedBy { it.name.lowercase(Locale.ROOT) }
}

fun buildRecurringSummary(templates: List<RecurringTemplateDto>): RecurringSummary {
    val activeTemplates = templates.filter { it.isActive }

    return RecurringSummary(
        activeCount = activeTemplates.size,
        monthlyExpenseTotal = activeTemplates
            .filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .sumOf { it.amount.toDoubleOrNull() ?: 0.0 },
        monthlyIncomeTotal = activeTemplates
            .filter { it.type.equals("INCOME", ignoreCase = true) }
            .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    )
}

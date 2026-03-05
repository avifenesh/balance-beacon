package app.balancebeacon.mobileandroid.feature.recurring.model

import kotlinx.serialization.Serializable

@Serializable
data class RecurringCategoryDto(
    val id: String,
    val name: String,
    val type: String? = null,
    val color: String? = null
)

@Serializable
data class RecurringTemplateDto(
    val id: String,
    val accountId: String,
    val categoryId: String,
    val type: String,
    val amount: String,
    val currency: String,
    val dayOfMonth: Int,
    val description: String? = null,
    val startMonth: String,
    val endMonth: String? = null,
    val isActive: Boolean = true,
    val category: RecurringCategoryDto? = null
)

@Serializable
data class RecurringTemplatesResponse(
    val recurringTemplates: List<RecurringTemplateDto> = emptyList()
)

@Serializable
data class UpsertRecurringTemplateRequest(
    val id: String? = null,
    val accountId: String,
    val categoryId: String,
    val type: String,
    val amount: Double,
    val currency: String,
    val dayOfMonth: Int,
    val description: String? = null,
    val startMonthKey: String,
    val endMonthKey: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class ToggleRecurringRequest(
    val isActive: Boolean
)

@Serializable
data class ToggleRecurringResponse(
    val id: String,
    val isActive: Boolean
)

@Serializable
data class ApplyRecurringRequest(
    val accountId: String? = null,
    val monthKey: String,
    val templateIds: List<String>? = null
)

@Serializable
data class ApplyRecurringResponse(
    val created: Int = 0,
    val skipped: Int = 0,
    val errors: List<String> = emptyList()
)

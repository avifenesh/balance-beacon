package app.balancebeacon.mobileandroid.feature.categories.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val type: String,
    val color: String? = null,
    val isArchived: Boolean = false,
    val isHolding: Boolean = false
)

@Serializable
data class CategoriesResponse(
    val categories: List<CategoryDto> = emptyList()
)

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val type: String,
    val color: String? = null
)

@Serializable
data class BulkCreateCategoriesRequest(
    val categories: List<CreateCategoryRequest>
)

@Serializable
data class BulkCreateCategoriesResponse(
    val categoriesCreated: Int,
    val categories: List<CategoryDto> = emptyList()
)

@Serializable
data class UpdateCategoryRequest(
    val name: String,
    val color: String? = null
)

@Serializable
data class ArchiveCategoryRequest(
    val isArchived: Boolean
)

@Serializable
data class ArchiveCategoryResponse(
    val id: String,
    val isArchived: Boolean
)

package app.balancebeacon.mobileandroid.feature.categories.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.categories.model.ArchiveCategoryResponse
import app.balancebeacon.mobileandroid.feature.categories.model.BulkCreateCategoriesRequest
import app.balancebeacon.mobileandroid.feature.categories.model.BulkCreateCategoriesResponse
import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.feature.categories.model.CreateCategoryRequest
import app.balancebeacon.mobileandroid.feature.categories.model.ArchiveCategoryRequest
import app.balancebeacon.mobileandroid.feature.categories.model.UpdateCategoryRequest

class CategoriesRepository(
    private val categoriesApi: CategoriesApi
) {
    suspend fun getCategories(
        type: String? = null,
        includeArchived: Boolean = false
    ): AppResult<List<CategoryDto>> {
        return runAppResult {
            categoriesApi.getCategories(type = type, includeArchived = includeArchived).categories
        }
    }

    suspend fun createCategory(
        name: String,
        type: String,
        color: String? = null
    ): AppResult<CategoryDto> {
        return runAppResult {
            categoriesApi.createCategory(
                CreateCategoryRequest(
                    name = name.trim(),
                    type = type,
                    color = color
                )
            )
        }
    }

    suspend fun bulkCreateCategories(
        categories: List<CreateCategoryRequest>
    ): AppResult<BulkCreateCategoriesResponse> {
        return runAppResult {
            categoriesApi.bulkCreateCategories(
                request = BulkCreateCategoriesRequest(categories = categories)
            )
        }
    }

    suspend fun updateCategory(
        id: String,
        name: String,
        color: String? = null
    ): AppResult<CategoryDto> {
        return runAppResult {
            categoriesApi.updateCategory(
                id = id,
                request = UpdateCategoryRequest(
                    name = name.trim(),
                    color = color
                )
            )
        }
    }

    suspend fun archiveCategory(
        id: String,
        isArchived: Boolean
    ): AppResult<ArchiveCategoryResponse> {
        return runAppResult {
            categoriesApi.archiveCategory(
                id = id,
                request = ArchiveCategoryRequest(isArchived = isArchived)
            )
        }
    }
}

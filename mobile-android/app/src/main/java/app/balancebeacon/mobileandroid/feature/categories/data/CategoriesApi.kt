package app.balancebeacon.mobileandroid.feature.categories.data

import app.balancebeacon.mobileandroid.feature.categories.model.ArchiveCategoryRequest
import app.balancebeacon.mobileandroid.feature.categories.model.ArchiveCategoryResponse
import app.balancebeacon.mobileandroid.feature.categories.model.BulkCreateCategoriesRequest
import app.balancebeacon.mobileandroid.feature.categories.model.BulkCreateCategoriesResponse
import app.balancebeacon.mobileandroid.feature.categories.model.CategoriesResponse
import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import app.balancebeacon.mobileandroid.feature.categories.model.CreateCategoryRequest
import app.balancebeacon.mobileandroid.feature.categories.model.UpdateCategoryRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CategoriesApi {
    @GET("categories")
    suspend fun getCategories(
        @Query("type") type: String? = null,
        @Query("includeArchived") includeArchived: Boolean = false
    ): CategoriesResponse

    @POST("categories")
    suspend fun createCategory(
        @Body request: CreateCategoryRequest
    ): CategoryDto

    @POST("categories/bulk")
    suspend fun bulkCreateCategories(
        @Body request: BulkCreateCategoriesRequest
    ): BulkCreateCategoriesResponse

    @PUT("categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body request: UpdateCategoryRequest
    ): CategoryDto

    @PATCH("categories/{id}/archive")
    suspend fun archiveCategory(
        @Path("id") id: String,
        @Body request: ArchiveCategoryRequest
    ): ArchiveCategoryResponse
}

package app.balancebeacon.mobileandroid.feature.recurring.data

import app.balancebeacon.mobileandroid.feature.recurring.model.ApplyRecurringRequest
import app.balancebeacon.mobileandroid.feature.recurring.model.ApplyRecurringResponse
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringTemplateDto
import app.balancebeacon.mobileandroid.feature.recurring.model.RecurringTemplatesResponse
import app.balancebeacon.mobileandroid.feature.recurring.model.ToggleRecurringRequest
import app.balancebeacon.mobileandroid.feature.recurring.model.ToggleRecurringResponse
import app.balancebeacon.mobileandroid.feature.recurring.model.UpsertRecurringTemplateRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RecurringApi {
    @GET("recurring")
    suspend fun getRecurringTemplates(
        @Query("accountId") accountId: String,
        @Query("isActive") isActive: Boolean? = null
    ): RecurringTemplatesResponse

    @POST("recurring")
    suspend fun upsertRecurringTemplate(
        @Body request: UpsertRecurringTemplateRequest
    ): RecurringTemplateDto

    @PATCH("recurring/{id}/toggle")
    suspend fun toggleRecurringTemplate(
        @Path("id") id: String,
        @Body request: ToggleRecurringRequest
    ): ToggleRecurringResponse

    @POST("recurring/apply")
    suspend fun applyRecurringTemplates(
        @Body request: ApplyRecurringRequest
    ): ApplyRecurringResponse
}


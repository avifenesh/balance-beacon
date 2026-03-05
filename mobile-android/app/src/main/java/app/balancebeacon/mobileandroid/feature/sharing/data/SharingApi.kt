package app.balancebeacon.mobileandroid.feature.sharing.data

import app.balancebeacon.mobileandroid.feature.auth.model.MessageResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.CreateSharedExpenseRequest
import app.balancebeacon.mobileandroid.feature.sharing.model.DeclineShareResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.MarkPaidResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.SharedExpenseDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharingResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.UserLookupResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SharingApi {
    @GET("sharing")
    suspend fun getSharing(): SharingResponse

    @POST("expenses/share")
    suspend fun createSharedExpense(
        @Body request: CreateSharedExpenseRequest
    ): SharedExpenseDto

    @PATCH("expenses/shares/{participantId}/paid")
    suspend fun markParticipantPaid(
        @Path("participantId") participantId: String,
        @Body body: Map<String, String> = emptyMap()
    ): MarkPaidResponse

    @POST("expenses/shares/{participantId}/decline")
    suspend fun declineShare(
        @Path("participantId") participantId: String,
        @Body body: Map<String, String> = emptyMap()
    ): DeclineShareResponse

    @DELETE("expenses/shares/{id}")
    suspend fun deleteShare(
        @Path("id") id: String
    ): MessageResponse

    @POST("expenses/shares/{participantId}/remind")
    suspend fun sendReminder(
        @Path("participantId") participantId: String,
        @Body body: Map<String, String> = emptyMap()
    ): MessageResponse

    @GET("users/lookup")
    suspend fun lookupUser(
        @Query("email") email: String
    ): UserLookupResponse
}

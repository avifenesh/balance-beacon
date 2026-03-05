package app.balancebeacon.mobileandroid.feature.onboarding.data

import app.balancebeacon.mobileandroid.feature.auth.model.MessageResponse
import app.balancebeacon.mobileandroid.feature.onboarding.model.OnboardingCompleteRequest
import app.balancebeacon.mobileandroid.feature.onboarding.model.SeedDataRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface OnboardingApi {
    @POST("seed-data")
    suspend fun seedData(
        @Body request: SeedDataRequest = SeedDataRequest()
    ): MessageResponse

    @POST("onboarding/complete")
    suspend fun complete(
        @Body request: OnboardingCompleteRequest
    ): MessageResponse
}

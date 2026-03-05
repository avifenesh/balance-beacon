package app.balancebeacon.mobileandroid.feature.onboarding.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.auth.model.MessageResponse
import app.balancebeacon.mobileandroid.feature.onboarding.model.OnboardingCompleteRequest
import app.balancebeacon.mobileandroid.feature.onboarding.model.SeedDataRequest

class OnboardingRepository(
    private val onboardingApi: OnboardingApi
) {
    suspend fun seedSampleData(): AppResult<MessageResponse> {
        return runAppResult {
            onboardingApi.seedData(SeedDataRequest(includeSampleData = true))
        }
    }

    suspend fun completeOnboarding(currency: String, locale: String?): AppResult<MessageResponse> {
        return runAppResult {
            onboardingApi.complete(
                OnboardingCompleteRequest(
                    currency = currency,
                    locale = locale
                )
            )
        }
    }
}

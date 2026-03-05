package app.balancebeacon.mobileandroid.feature.onboarding.model

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingCompleteRequest(
    val currency: String,
    val locale: String? = null
)

@Serializable
data class OnboardingCompleteResponse(
    val message: String,
    val completed: Boolean = true
)

@Serializable
data class SeedDataRequest(
    val includeSampleData: Boolean = true
)

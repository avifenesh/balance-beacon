package app.balancebeacon.mobileandroid.feature.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String? = null,
    @SerialName("expiresIn") val expiresIn: Long? = null,
    val user: UserSummary? = null
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String
)

@Serializable
data class RegisterResponse(
    val message: String,
    @SerialName("emailVerified") val emailVerified: Boolean = false
)

@Serializable
data class RequestResetRequest(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val password: String
)

@Serializable
data class VerifyEmailRequest(
    val token: String
)

@Serializable
data class ResendVerificationRequest(
    val email: String
)

@Serializable
data class MessageResponse(
    val message: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class LogoutRequest(
    val refreshToken: String
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String
)

@Serializable
data class UpdateCurrencyRequest(
    val currency: String
)

@Serializable
data class UpdateCurrencyResponse(
    val currency: String
)

@Serializable
data class ExportUserDataResponse(
    val format: String? = null,
    val data: String? = null,
    val exportedAt: String? = null
)

@Serializable
data class DeleteAccountRequest(
    val confirmEmail: String
)

@Serializable
data class UserSummary(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val preferredCurrency: String? = null,
    val hasCompletedOnboarding: Boolean = false
)

@Serializable
data class MeResponse(
    val user: UserSummary
)

package app.balancebeacon.mobileandroid.feature.auth.data

import app.balancebeacon.mobileandroid.core.network.AuthInterceptor
import app.balancebeacon.mobileandroid.core.network.ApiEnvelope
import app.balancebeacon.mobileandroid.feature.auth.model.LoginRequest
import app.balancebeacon.mobileandroid.feature.auth.model.LoginResponse
import app.balancebeacon.mobileandroid.feature.auth.model.DeleteAccountRequest
import app.balancebeacon.mobileandroid.feature.auth.model.ExportUserDataResponse
import app.balancebeacon.mobileandroid.feature.auth.model.LogoutRequest
import app.balancebeacon.mobileandroid.feature.auth.model.MeResponse
import app.balancebeacon.mobileandroid.feature.auth.model.MessageResponse
import app.balancebeacon.mobileandroid.feature.auth.model.RefreshRequest
import app.balancebeacon.mobileandroid.feature.auth.model.RequestResetRequest
import app.balancebeacon.mobileandroid.feature.auth.model.ResendVerificationRequest
import app.balancebeacon.mobileandroid.feature.auth.model.RegisterRequest
import app.balancebeacon.mobileandroid.feature.auth.model.RegisterResponse
import app.balancebeacon.mobileandroid.feature.auth.model.ResetPasswordRequest
import app.balancebeacon.mobileandroid.feature.auth.model.UpdateCurrencyRequest
import app.balancebeacon.mobileandroid.feature.auth.model.UpdateCurrencyResponse
import app.balancebeacon.mobileandroid.feature.auth.model.UpdateProfileRequest
import app.balancebeacon.mobileandroid.feature.auth.model.UserSummary
import app.balancebeacon.mobileandroid.feature.auth.model.VerifyEmailRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    @POST("auth/login")
    suspend fun login(
        @Header(AuthInterceptor.NO_AUTH_HEADER) noAuth: String = "true",
        @Body request: LoginRequest
    ): ApiEnvelope<LoginResponse>

    @POST("auth/debug-login")
    suspend fun debugLogin(
        @Header(AuthInterceptor.NO_AUTH_HEADER) noAuth: String = "true"
    ): ApiEnvelope<LoginResponse>

    @POST("auth/register")
    suspend fun register(
        @Header(AuthInterceptor.NO_AUTH_HEADER) noAuth: String = "true",
        @Body request: RegisterRequest
    ): RegisterResponse

    @POST("auth/request-reset")
    suspend fun requestReset(
        @Header(AuthInterceptor.NO_AUTH_HEADER) noAuth: String = "true",
        @Body request: RequestResetRequest
    ): MessageResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Header(AuthInterceptor.NO_AUTH_HEADER) noAuth: String = "true",
        @Body request: ResetPasswordRequest
    ): MessageResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(
        @Header(AuthInterceptor.NO_AUTH_HEADER) noAuth: String = "true",
        @Body request: VerifyEmailRequest
    ): MessageResponse

    @POST("auth/resend-verification")
    suspend fun resendVerification(
        @Header(AuthInterceptor.NO_AUTH_HEADER) noAuth: String = "true",
        @Body request: ResendVerificationRequest
    ): MessageResponse

    @POST("auth/refresh")
    suspend fun refresh(
        @Header(AuthInterceptor.NO_AUTH_HEADER) noAuth: String = "true",
        @Body request: RefreshRequest
    ): ApiEnvelope<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest
    )

    @GET("users/me")
    suspend fun me(): MeResponse

    @PATCH("users/me")
    suspend fun updateMe(
        @Body request: UpdateProfileRequest
    ): UserSummary

    @PATCH("users/me/currency")
    suspend fun updateCurrency(
        @Body request: UpdateCurrencyRequest
    ): UpdateCurrencyResponse

    @GET("auth/export")
    suspend fun exportData(
        @Query("format") format: String = "json"
    ): ExportUserDataResponse

    @HTTP(method = "DELETE", path = "auth/account", hasBody = true)
    suspend fun deleteAccount(
        @Body request: DeleteAccountRequest
    ): MessageResponse
}

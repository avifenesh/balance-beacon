package app.balancebeacon.mobileandroid.feature.auth.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.core.session.SessionManager
import app.balancebeacon.mobileandroid.feature.auth.model.DeleteAccountRequest
import app.balancebeacon.mobileandroid.feature.auth.model.ExportUserDataResponse
import app.balancebeacon.mobileandroid.feature.auth.model.LoginRequest
import app.balancebeacon.mobileandroid.feature.auth.model.LoginResponse
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
import java.util.Locale

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) {
    suspend fun debugSignIn(): AppResult<LoginResponse> {
        val result = runAppResult { authApi.debugLogin().data }
        if (result is AppResult.Success) {
            persistTokens(result.value)
        }
        return result
    }

    suspend fun login(email: String, password: String): AppResult<LoginResponse> {
        val result = runAppResult {
            authApi.login(request = LoginRequest(email = email.trim(), password = password)).data
        }
        if (result is AppResult.Success) {
            persistTokens(result.value)
        }
        return result
    }

    suspend fun register(email: String, password: String, displayName: String): AppResult<RegisterResponse> {
        return runAppResult {
            authApi.register(
                request = RegisterRequest(
                    email = email.trim(),
                    password = password,
                    displayName = displayName.trim()
                )
            )
        }
    }

    suspend fun requestReset(email: String): AppResult<MessageResponse> {
        return runAppResult {
            authApi.requestReset(
                request = RequestResetRequest(email = email.trim())
            )
        }
    }

    suspend fun resetPassword(token: String, password: String): AppResult<MessageResponse> {
        return runAppResult {
            authApi.resetPassword(
                request = ResetPasswordRequest(
                    token = token.trim(),
                    password = password
                )
            )
        }
    }

    suspend fun verifyEmail(token: String): AppResult<MessageResponse> {
        return runAppResult {
            authApi.verifyEmail(request = VerifyEmailRequest(token = token.trim()))
        }
    }

    suspend fun resendVerification(email: String): AppResult<MessageResponse> {
        return runAppResult {
            authApi.resendVerification(
                request = ResendVerificationRequest(email = email.trim())
            )
        }
    }

    suspend fun me(): AppResult<MeResponse> {
        return runAppResult { authApi.me() }
    }

    suspend fun updateProfile(displayName: String): AppResult<UserSummary> {
        return runAppResult {
            authApi.updateMe(
                request = UpdateProfileRequest(displayName = displayName.trim())
            )
        }
    }

    suspend fun updateCurrency(currency: String): AppResult<UpdateCurrencyResponse> {
        return runAppResult {
            authApi.updateCurrency(
                request = UpdateCurrencyRequest(currency = currency.trim().uppercase(Locale.ROOT))
            )
        }
    }

    suspend fun exportUserData(format: String = "json"): AppResult<ExportUserDataResponse> {
        val normalizedFormat = format.trim().lowercase(Locale.ROOT).ifBlank { "json" }
        return runAppResult {
            authApi.exportData(format = normalizedFormat)
        }
    }

    suspend fun deleteAccount(confirmEmail: String): AppResult<MessageResponse> {
        val result = runAppResult {
            authApi.deleteAccount(
                request = DeleteAccountRequest(confirmEmail = confirmEmail.trim())
            )
        }
        if (result is AppResult.Success) {
            sessionManager.clearSession()
        }
        return result
    }

    suspend fun refreshSession(refreshToken: String): AppResult<LoginResponse> {
        val result = runAppResult {
            authApi.refresh(request = RefreshRequest(refreshToken = refreshToken)).data
        }
        if (result is AppResult.Success) {
            persistTokens(result.value)
        }
        return result
    }

    suspend fun logout(): AppResult<Unit> {
        val refreshToken = sessionManagerTokenRefresh()
        val result = if (refreshToken.isNullOrBlank()) {
            AppResult.Success(Unit)
        } else {
            runAppResult { authApi.logout(LogoutRequest(refreshToken = refreshToken)) }
        }
        sessionManager.clearSession()
        return result
    }

    private suspend fun persistTokens(response: LoginResponse) {
        val accessToken = response.accessToken
        if (accessToken.isBlank()) return

        sessionManager.persistTokens(
            app.balancebeacon.mobileandroid.core.storage.AuthTokens(
                accessToken = accessToken,
                refreshToken = response.refreshToken
            )
        )
    }

    private fun sessionManagerTokenRefresh(): String? {
        val refreshToken = sessionManager.currentRefreshToken
        return if (refreshToken.isNullOrBlank()) null else refreshToken
    }
}

package app.balancebeacon.mobileandroid.core.network

import app.balancebeacon.mobileandroid.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (original.header(NO_AUTH_HEADER) == "true") {
            return chain.proceed(
                original.newBuilder().removeHeader(NO_AUTH_HEADER).build()
            )
        }

        if (!original.header("Authorization").isNullOrBlank()) {
            return chain.proceed(original)
        }

        val accessToken = sessionManager.currentAccessToken
        val request = if (accessToken.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        }

        return chain.proceed(request)
    }

    companion object {
        const val NO_AUTH_HEADER = "X-No-Auth"
    }
}

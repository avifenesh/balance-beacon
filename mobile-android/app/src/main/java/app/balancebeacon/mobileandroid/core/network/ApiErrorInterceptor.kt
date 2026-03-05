package app.balancebeacon.mobileandroid.core.network

import okhttp3.Interceptor
import okhttp3.Response

class ApiErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) return response

        val responseBody = response.peekBody(MAX_ERROR_BODY_BYTES).string()
        val exception = ApiErrorMapper.toException(
            httpCode = response.code,
            body = responseBody
        )

        response.close()
        throw exception
    }

    private companion object {
        const val MAX_ERROR_BODY_BYTES = 1024L * 1024L
    }
}

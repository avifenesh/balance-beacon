package app.balancebeacon.mobileandroid.core.network

import app.balancebeacon.mobileandroid.core.result.AppError
import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ApiErrorMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun toException(httpCode: Int, body: String?): ApiHttpException {
        val payload = body?.takeIf { it.isNotBlank() }?.let(::parsePayload)
        val message = payload?.message?.takeIf { it.isNotBlank() }
            ?: payload?.error?.takeIf { it.isNotBlank() }
            ?: "Request failed with status $httpCode"
        return ApiHttpException(
            httpCode = httpCode,
            message = message,
            fieldErrors = payload?.fields ?: emptyMap()
        )
    }

    fun toAppError(throwable: Throwable): AppError {
        return when (throwable) {
            is ApiHttpException -> AppError(
                httpCode = throwable.httpCode,
                message = throwable.message,
                fieldErrors = throwable.fieldErrors,
                cause = throwable
            )

            is IOException -> AppError(
                message = "Network request failed",
                cause = throwable
            )

            else -> AppError(
                message = throwable.message ?: "Unexpected error",
                cause = throwable
            )
        }
    }

    private fun parsePayload(raw: String): ApiErrorPayload? {
        return runCatching { json.decodeFromString<ApiErrorPayload>(raw) }.getOrNull()
    }

    @Serializable
    private data class ApiErrorPayload(
        val error: String? = null,
        val message: String? = null,
        val fields: Map<String, String>? = null
    )
}

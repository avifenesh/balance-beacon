package app.balancebeacon.mobileandroid.core.result

import app.balancebeacon.mobileandroid.core.network.ApiHttpException
import app.balancebeacon.mobileandroid.core.network.ApiErrorMapper

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

data class AppError(
    val httpCode: Int? = null,
    val message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
    val cause: Throwable? = null
)

suspend fun <T> runAppResult(block: suspend () -> T): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (error: Throwable) {
        val mappedError = when (error) {
            is ApiHttpException -> AppError(
                httpCode = error.httpCode,
                message = error.message,
                fieldErrors = error.fieldErrors,
                cause = error
            )

            else -> ApiErrorMapper.toAppError(error)
        }

        AppResult.Failure(mappedError)
    }
}

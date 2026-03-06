package app.balancebeacon.mobileandroid.ui.util

fun sanitizeError(error: String): String = when {
    error.contains("expired", ignoreCase = true) -> "Session expired"
    error.contains("serial", ignoreCase = true) || error.contains("path:") -> "Something went wrong"
    error.length > 80 -> "Something went wrong"
    else -> error
}

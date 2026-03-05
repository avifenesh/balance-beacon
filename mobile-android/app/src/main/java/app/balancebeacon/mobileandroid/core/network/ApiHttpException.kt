package app.balancebeacon.mobileandroid.core.network

import java.io.IOException

class ApiHttpException(
    val httpCode: Int,
    override val message: String,
    val fieldErrors: Map<String, String> = emptyMap()
) : IOException(message)

package app.balancebeacon.mobileandroid.core.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val data: T
)

package app.balancebeacon.mobileandroid.feature.subscription.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionItem(
    val id: String? = null,
    val status: String = "UNKNOWN",
    val plan: String? = null,
    @SerialName("currentPeriodEnd") val currentPeriodEnd: String? = null
)

@Serializable
data class SubscriptionResponse(
    val subscriptions: List<SubscriptionItem>? = null,
    val status: String? = null
)

data class SubscriptionSnapshot(
    val isActive: Boolean,
    val status: String,
    val plan: String?
)

package app.balancebeacon.mobileandroid.feature.subscription.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.subscription.model.SubscriptionSnapshot

class SubscriptionRepository(
    private val subscriptionApi: SubscriptionApi
) {
    suspend fun fetchSnapshot(): AppResult<SubscriptionSnapshot> {
        return runAppResult {
            val response = subscriptionApi.getSubscriptions()
            val item = response.subscriptions?.firstOrNull()
            val status = item?.status ?: response.status ?: "UNKNOWN"
            SubscriptionSnapshot(
                isActive = status.equals("ACTIVE", ignoreCase = true) ||
                    status.equals("TRIALING", ignoreCase = true),
                status = status,
                plan = item?.plan
            )
        }
    }
}

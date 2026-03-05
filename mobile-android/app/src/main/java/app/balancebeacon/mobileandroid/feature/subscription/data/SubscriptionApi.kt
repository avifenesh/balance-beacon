package app.balancebeacon.mobileandroid.feature.subscription.data

import app.balancebeacon.mobileandroid.feature.subscription.model.SubscriptionResponse
import retrofit2.http.GET

interface SubscriptionApi {
    @GET("subscriptions")
    suspend fun getSubscriptions(): SubscriptionResponse
}

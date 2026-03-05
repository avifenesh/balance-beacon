package app.balancebeacon.mobileandroid.core.network

import app.balancebeacon.mobileandroid.BuildConfig
import app.balancebeacon.mobileandroid.core.session.SessionManager
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsApi
import app.balancebeacon.mobileandroid.feature.auth.data.AuthApi
import app.balancebeacon.mobileandroid.feature.budgets.data.BudgetsApi
import app.balancebeacon.mobileandroid.feature.categories.data.CategoriesApi
import app.balancebeacon.mobileandroid.feature.onboarding.data.OnboardingApi
import app.balancebeacon.mobileandroid.feature.sharing.data.SharingApi
import app.balancebeacon.mobileandroid.feature.subscription.data.SubscriptionApi
import app.balancebeacon.mobileandroid.feature.transactions.data.TransactionsApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit

class ApiClient(sessionManager: SessionManager) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val authInterceptor = AuthInterceptor(sessionManager)
    private val apiErrorInterceptor = ApiErrorInterceptor()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(apiErrorInterceptor)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
            }
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.trimEnd('/') + "/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val onboardingApi: OnboardingApi = retrofit.create(OnboardingApi::class.java)
    val subscriptionApi: SubscriptionApi = retrofit.create(SubscriptionApi::class.java)
    val transactionsApi: TransactionsApi = retrofit.create(TransactionsApi::class.java)
    val budgetsApi: BudgetsApi = retrofit.create(BudgetsApi::class.java)
    val accountsApi: AccountsApi = retrofit.create(AccountsApi::class.java)
    val categoriesApi: CategoriesApi = retrofit.create(CategoriesApi::class.java)
    val sharingApi: SharingApi = retrofit.create(SharingApi::class.java)
}

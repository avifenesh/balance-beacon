package app.balancebeacon.mobileandroid.core

import android.content.Context
import app.balancebeacon.mobileandroid.core.network.ApiClient
import app.balancebeacon.mobileandroid.core.session.SessionManager
import app.balancebeacon.mobileandroid.core.storage.SessionStore
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsRepository
import app.balancebeacon.mobileandroid.feature.auth.data.AuthRepository
import app.balancebeacon.mobileandroid.feature.budgets.data.BudgetsRepository
import app.balancebeacon.mobileandroid.feature.categories.data.CategoriesRepository
import app.balancebeacon.mobileandroid.feature.onboarding.data.OnboardingRepository
import app.balancebeacon.mobileandroid.feature.sharing.data.SharingRepository
import app.balancebeacon.mobileandroid.feature.subscription.data.SubscriptionRepository
import app.balancebeacon.mobileandroid.feature.transactions.data.TransactionsRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val sessionStore: SessionStore by lazy {
        SessionStore(appContext)
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(sessionStore)
    }

    val apiClient: ApiClient by lazy {
        ApiClient(sessionManager)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            authApi = apiClient.authApi,
            sessionManager = sessionManager
        )
    }

    val onboardingRepository: OnboardingRepository by lazy {
        OnboardingRepository(onboardingApi = apiClient.onboardingApi)
    }

    val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepository(subscriptionApi = apiClient.subscriptionApi)
    }

    val transactionsRepository: TransactionsRepository by lazy {
        TransactionsRepository(transactionsApi = apiClient.transactionsApi)
    }

    val budgetsRepository: BudgetsRepository by lazy {
        BudgetsRepository(budgetsApi = apiClient.budgetsApi)
    }

    val accountsRepository: AccountsRepository by lazy {
        AccountsRepository(accountsApi = apiClient.accountsApi)
    }

    val categoriesRepository: CategoriesRepository by lazy {
        CategoriesRepository(categoriesApi = apiClient.categoriesApi)
    }

    val sharingRepository: SharingRepository by lazy {
        SharingRepository(sharingApi = apiClient.sharingApi)
    }
}

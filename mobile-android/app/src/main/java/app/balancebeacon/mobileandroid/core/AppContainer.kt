package app.balancebeacon.mobileandroid.core

import android.content.Context
import app.balancebeacon.mobileandroid.core.database.AppDatabase
import app.balancebeacon.mobileandroid.core.network.ApiClient
import app.balancebeacon.mobileandroid.core.session.SessionManager
import app.balancebeacon.mobileandroid.core.storage.SessionStore
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsRepository
import app.balancebeacon.mobileandroid.feature.assistant.data.AssistantRepository
import app.balancebeacon.mobileandroid.feature.auth.data.AuthRepository
import app.balancebeacon.mobileandroid.feature.budgets.data.BudgetsRepository
import app.balancebeacon.mobileandroid.feature.categories.data.CategoriesRepository
import app.balancebeacon.mobileandroid.feature.dashboard.data.DashboardRepository
import app.balancebeacon.mobileandroid.feature.holdings.data.HoldingsRepository
import app.balancebeacon.mobileandroid.feature.onboarding.data.OnboardingRepository
import app.balancebeacon.mobileandroid.feature.recurring.data.RecurringRepository
import app.balancebeacon.mobileandroid.feature.sharing.data.SharingRepository
import app.balancebeacon.mobileandroid.feature.subscription.data.SubscriptionRepository
import app.balancebeacon.mobileandroid.feature.transactions.data.WorkManagerPendingTransactionSyncScheduler
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

    val appDatabase: AppDatabase by lazy {
        AppDatabase.getInstance(appContext)
    }

    private val pendingTransactionSyncScheduler by lazy {
        WorkManagerPendingTransactionSyncScheduler(appContext)
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
        TransactionsRepository(
            transactionsApi = apiClient.transactionsApi,
            pendingTransactionDao = appDatabase.pendingTransactionDao(),
            pendingTransactionSyncScheduler = pendingTransactionSyncScheduler
        )
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

    val dashboardRepository: DashboardRepository by lazy {
        DashboardRepository(dashboardApi = apiClient.dashboardApi)
    }

    val assistantRepository: AssistantRepository by lazy {
        AssistantRepository(assistantApi = apiClient.assistantApi)
    }

    val recurringRepository: RecurringRepository by lazy {
        RecurringRepository(recurringApi = apiClient.recurringApi)
    }

    val holdingsRepository: HoldingsRepository by lazy {
        HoldingsRepository(holdingsApi = apiClient.holdingsApi)
    }
}

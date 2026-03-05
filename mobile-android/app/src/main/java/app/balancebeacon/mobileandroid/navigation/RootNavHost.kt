package app.balancebeacon.mobileandroid.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.balancebeacon.mobileandroid.core.AppContainer
import app.balancebeacon.mobileandroid.core.session.SessionState
import app.balancebeacon.mobileandroid.feature.accounts.ui.AccountsScreen
import app.balancebeacon.mobileandroid.feature.accounts.ui.AccountsViewModel
import app.balancebeacon.mobileandroid.feature.assistant.ui.AssistantScreen
import app.balancebeacon.mobileandroid.feature.assistant.ui.AssistantViewModel
import app.balancebeacon.mobileandroid.feature.auth.ui.LoginScreen
import app.balancebeacon.mobileandroid.feature.auth.ui.LoginViewModel
import app.balancebeacon.mobileandroid.feature.auth.ui.LoginViewModelFactory
import app.balancebeacon.mobileandroid.feature.auth.ui.RegisterScreen
import app.balancebeacon.mobileandroid.feature.auth.ui.RegisterViewModel
import app.balancebeacon.mobileandroid.feature.auth.ui.RegisterViewModelFactory
import app.balancebeacon.mobileandroid.feature.auth.ui.ResetPasswordScreen
import app.balancebeacon.mobileandroid.feature.auth.ui.ResetPasswordViewModel
import app.balancebeacon.mobileandroid.feature.auth.ui.ResetPasswordViewModelFactory
import app.balancebeacon.mobileandroid.feature.auth.ui.VerifyEmailScreen
import app.balancebeacon.mobileandroid.feature.auth.ui.VerifyEmailViewModel
import app.balancebeacon.mobileandroid.feature.auth.ui.VerifyEmailViewModelFactory
import app.balancebeacon.mobileandroid.feature.budgets.ui.BudgetsScreen
import app.balancebeacon.mobileandroid.feature.budgets.ui.BudgetsViewModel
import app.balancebeacon.mobileandroid.feature.categories.ui.CategoriesScreen
import app.balancebeacon.mobileandroid.feature.categories.ui.CategoriesViewModel
import app.balancebeacon.mobileandroid.feature.dashboard.ui.DashboardTrendCard
import app.balancebeacon.mobileandroid.feature.dashboard.ui.DashboardOverviewScreen
import app.balancebeacon.mobileandroid.feature.dashboard.ui.DashboardViewModel
import app.balancebeacon.mobileandroid.feature.holdings.ui.HoldingsScreen
import app.balancebeacon.mobileandroid.feature.holdings.ui.HoldingsViewModel
import app.balancebeacon.mobileandroid.feature.onboarding.ui.OnboardingScreen
import app.balancebeacon.mobileandroid.feature.onboarding.ui.OnboardingViewModel
import app.balancebeacon.mobileandroid.feature.paywall.ui.PaywallScreen
import app.balancebeacon.mobileandroid.feature.profile.ui.ProfileScreen
import app.balancebeacon.mobileandroid.feature.profile.ui.ProfileViewModel
import app.balancebeacon.mobileandroid.feature.recurring.ui.RecurringScreen
import app.balancebeacon.mobileandroid.feature.recurring.ui.RecurringViewModel
import app.balancebeacon.mobileandroid.feature.sharing.ui.SharingScreen
import app.balancebeacon.mobileandroid.feature.sharing.ui.SharingViewModel
import app.balancebeacon.mobileandroid.feature.settings.ui.SettingsScreen
import app.balancebeacon.mobileandroid.feature.settings.ui.SettingsViewModel
import app.balancebeacon.mobileandroid.feature.subscription.ui.SubscriptionScreen
import app.balancebeacon.mobileandroid.feature.subscription.ui.SubscriptionViewModel
import app.balancebeacon.mobileandroid.feature.transactions.ui.TransactionsScreen
import app.balancebeacon.mobileandroid.feature.transactions.ui.TransactionsViewModel
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import kotlinx.coroutines.launch

@Composable
fun RootNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val sessionState by appContainer.sessionManager.sessionState.collectAsState()

    if (sessionState == SessionState.Loading) {
        LoadingScreen(modifier = modifier)
        return
    }

    key(sessionState) {
        val navController = rememberNavController()
        val coroutineScope = rememberCoroutineScope()
        val startDestination = when (sessionState) {
            SessionState.Authenticated -> AppDestination.Dashboard.route
            SessionState.Unauthenticated, SessionState.Loading -> AppDestination.Login.route
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier
        ) {
            composable(AppDestination.Login.route) {
                val loginViewModel: LoginViewModel = viewModel(
                    factory = remember { LoginViewModelFactory(appContainer.authRepository) }
                )
                LoginScreen(
                    viewModel = loginViewModel,
                    onRegisterClick = { navController.navigate(AppDestination.Register.route) },
                    onResetPasswordClick = { navController.navigate(AppDestination.ResetPassword.route) },
                    onVerifyEmailClick = { navController.navigate(AppDestination.VerifyEmail.route) }
                )
            }

            composable(AppDestination.Register.route) {
                val registerViewModel: RegisterViewModel = viewModel(
                    factory = remember { RegisterViewModelFactory(appContainer.authRepository) }
                )
                FeatureShell(
                    title = "Register",
                    onBack = { navController.popBackStack() }
                ) {
                    RegisterScreen(
                        viewModel = registerViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            composable(AppDestination.ResetPassword.route) {
                val resetPasswordViewModel: ResetPasswordViewModel = viewModel(
                    factory = remember { ResetPasswordViewModelFactory(appContainer.authRepository) }
                )
                FeatureShell(
                    title = "Reset Password",
                    onBack = { navController.popBackStack() }
                ) {
                    ResetPasswordScreen(
                        viewModel = resetPasswordViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            composable(AppDestination.VerifyEmail.route) {
                val verifyEmailViewModel: VerifyEmailViewModel = viewModel(
                    factory = remember { VerifyEmailViewModelFactory(appContainer.authRepository) }
                )
                FeatureShell(
                    title = "Verify Email",
                    onBack = { navController.popBackStack() }
                ) {
                    VerifyEmailScreen(
                        viewModel = verifyEmailViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            composable(AppDestination.Dashboard.route) {
                val dashboardVm: DashboardViewModel = viewModel(
                    factory = remember {
                        simpleFactory {
                            DashboardViewModel(
                                dashboardRepository = appContainer.dashboardRepository,
                                transactionsRepository = appContainer.transactionsRepository
                            )
                        }
                    }
                )
                DashboardScreen(
                    dashboardViewModel = dashboardVm,
                    onOpenOverview = { navController.navigate(AppDestination.Overview.route) },
                    onOpenAssistant = { navController.navigate(AppDestination.Assistant.route) },
                    onOpenRecurring = { navController.navigate(AppDestination.Recurring.route) },
                    onOpenHoldings = { navController.navigate(AppDestination.Holdings.route) },
                    onOpenOnboarding = { navController.navigate(AppDestination.Onboarding.route) },
                    onOpenSubscription = { navController.navigate(AppDestination.Subscription.route) },
                    onOpenPaywall = { navController.navigate(AppDestination.Paywall.route) },
                    onOpenTransactions = { navController.navigate(AppDestination.Transactions.route) },
                    onOpenBudgets = { navController.navigate(AppDestination.Budgets.route) },
                    onOpenAccounts = { navController.navigate(AppDestination.Accounts.route) },
                    onOpenCategories = { navController.navigate(AppDestination.Categories.route) },
                    onOpenSharing = { navController.navigate(AppDestination.Sharing.createRoute()) },
                    onOpenProfile = { navController.navigate(AppDestination.Profile.route) },
                    onOpenSettings = { navController.navigate(AppDestination.Settings.route) },
                    onSignOut = {
                        coroutineScope.launch {
                            appContainer.authRepository.logout()
                        }
                    }
                )
            }

            composable(AppDestination.Overview.route) {
                val vm: DashboardViewModel = viewModel(
                    factory = remember {
                        simpleFactory {
                            DashboardViewModel(
                                dashboardRepository = appContainer.dashboardRepository,
                                transactionsRepository = appContainer.transactionsRepository
                            )
                        }
                    }
                )
                FeatureShell(
                    title = "Overview",
                    onBack = { navController.popBackStack() }
                ) {
                    DashboardOverviewScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
            }

            composable(AppDestination.Assistant.route) {
                val vm: AssistantViewModel = viewModel(
                    factory = remember {
                        simpleFactory {
                            AssistantViewModel(
                                assistantRepository = appContainer.assistantRepository,
                                accountsRepository = appContainer.accountsRepository,
                                assistantSessionStore = appContainer.assistantSessionStore
                            )
                        }
                    }
                )
                FeatureShell(
                    title = "Assistant",
                    onBack = { navController.popBackStack() }
                ) {
                    AssistantScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
            }

            composable(AppDestination.Recurring.route) {
                val vm: RecurringViewModel = viewModel(
                    factory = remember {
                        simpleFactory {
                            RecurringViewModel(
                                recurringRepository = appContainer.recurringRepository,
                                accountsRepository = appContainer.accountsRepository,
                                categoriesRepository = appContainer.categoriesRepository
                            )
                        }
                    }
                )
                FeatureShell(
                    title = "Recurring",
                    onBack = { navController.popBackStack() }
                ) {
                    RecurringScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
            }

            composable(AppDestination.Holdings.route) {
                val vm: HoldingsViewModel = viewModel(
                    factory = remember {
                        simpleFactory { HoldingsViewModel(appContainer.holdingsRepository) }
                    }
                )
                FeatureShell(
                    title = "Holdings",
                    onBack = { navController.popBackStack() }
                ) {
                    HoldingsScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
            }

            composable(AppDestination.Onboarding.route) {
                val vm: OnboardingViewModel = viewModel(
                    factory = remember {
                        simpleFactory { OnboardingViewModel(appContainer.onboardingRepository) }
                    }
                )
                FeatureShell(
                    title = "Onboarding",
                    onBack = { navController.popBackStack() }
                ) {
                    OnboardingScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
            }

            composable(AppDestination.Subscription.route) {
                val vm: SubscriptionViewModel = viewModel(
                    factory = remember {
                        simpleFactory { SubscriptionViewModel(appContainer.subscriptionRepository) }
                    }
                )
                FeatureShell(
                    title = "Subscription",
                    onBack = { navController.popBackStack() }
                ) {
                    SubscriptionScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
            }

            composable(AppDestination.Paywall.route) {
                FeatureShell(
                    title = "Paywall",
                    onBack = { navController.popBackStack() }
                ) {
                    PaywallScreen(
                        onUpgradeClick = { navController.navigate(AppDestination.Subscription.route) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            composable(AppDestination.Transactions.route) {
                val vm: TransactionsViewModel = viewModel(
                    factory = remember {
                        simpleFactory {
                            TransactionsViewModel(
                                transactionsRepository = appContainer.transactionsRepository,
                                dashboardRepository = appContainer.dashboardRepository
                            )
                        }
                    }
                )
                FeatureShell(
                    title = "Transactions",
                    onBack = { navController.popBackStack() }
                ) {
                    TransactionsScreen(
                        viewModel = vm,
                        onShareTransaction = { transactionId ->
                            navController.navigate(AppDestination.Sharing.createRoute(transactionId))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            composable(AppDestination.Budgets.route) {
                val vm: BudgetsViewModel = viewModel(
                    factory = remember {
                        simpleFactory {
                            BudgetsViewModel(
                                budgetsRepository = appContainer.budgetsRepository,
                                accountsRepository = appContainer.accountsRepository
                            )
                        }
                    }
                )
                FeatureShell(
                    title = "Budgets",
                    onBack = { navController.popBackStack() }
                ) {
                    BudgetsScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
            }

            composable(AppDestination.Accounts.route) {
                val vm: AccountsViewModel = viewModel(
                    factory = remember {
                        simpleFactory { AccountsViewModel(appContainer.accountsRepository) }
                    }
                )
                FeatureShell(
                    title = "Accounts",
                    onBack = { navController.popBackStack() }
                ) {
                    AccountsScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
            }

            composable(AppDestination.Categories.route) {
                val vm: CategoriesViewModel = viewModel(
                    factory = remember {
                        simpleFactory { CategoriesViewModel(appContainer.categoriesRepository) }
                    }
                )
                FeatureShell(
                    title = "Categories",
                    onBack = { navController.popBackStack() }
                ) {
                    CategoriesScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
            }

            composable(
                route = AppDestination.Sharing.routeWithArgs,
                arguments = listOf(
                    navArgument(AppDestination.Sharing.transactionIdArg) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val initialTransactionId = backStackEntry.arguments
                    ?.getString(AppDestination.Sharing.transactionIdArg)
                    .orEmpty()
                val vm: SharingViewModel = viewModel(
                    factory = remember {
                        simpleFactory { SharingViewModel(appContainer.sharingRepository) }
                    }
                )
                FeatureShell(
                    title = "Sharing",
                    onBack = { navController.popBackStack() }
                ) {
                    SharingScreen(
                        viewModel = vm,
                        initialTransactionId = initialTransactionId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            composable(AppDestination.Profile.route) {
                val vm: ProfileViewModel = viewModel(
                    factory = remember {
                        simpleFactory { ProfileViewModel(appContainer.authRepository) }
                    }
                )
                FeatureShell(
                    title = "Profile",
                    onBack = { navController.popBackStack() }
                ) {
                    ProfileScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
            }

            composable(AppDestination.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = remember {
                        simpleFactory { SettingsViewModel(appContainer.authRepository) }
                    }
                )
                FeatureShell(
                    title = "Settings",
                    onBack = { navController.popBackStack() }
                ) {
                    SettingsScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text("Loading workspace...", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    onOpenOverview: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenHoldings: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSharing: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboard()
    }

    val navigationActions = listOf(
        "Overview" to onOpenOverview,
        "Assistant" to onOpenAssistant,
        "Recurring" to onOpenRecurring,
        "Holdings" to onOpenHoldings,
        "Transactions" to onOpenTransactions,
        "Budgets" to onOpenBudgets,
        "Accounts" to onOpenAccounts,
        "Categories" to onOpenCategories,
        "Sharing" to onOpenSharing,
        "Profile" to onOpenProfile,
        "Settings" to onOpenSettings,
        "Onboarding" to onOpenOnboarding,
        "Subscription" to onOpenSubscription,
        "Paywall" to onOpenPaywall
    )
    val featuredActions = navigationActions.take(6)
    val secondaryActions = navigationActions.drop(6)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Use the same web features in Android with the same dark glass styling.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                dashboardState.data?.summary?.let { summary ->
                    Text(
                        text = "Cashflow Snapshot: ${summary.netResult}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (dashboardState.isLoading && dashboardState.data == null) {
                    Text(
                        text = "Loading trend...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                dashboardState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = onOpenOverview,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    )
                ) {
                    Text("Open Cashflow Details")
                }
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick access", style = MaterialTheme.typography.titleMedium)
                featuredActions.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { (label, action) ->
                            Button(
                                onClick = action,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label)
                            }
                        }
                        repeat(2 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        dashboardState.data?.let { dashboard ->
            DashboardTrendCard(
                history = dashboard.history,
                comparison = dashboard.comparison,
                currencyCode = dashboard.preferredCurrency ?: "USD",
                modifier = Modifier.fillMaxWidth()
            )
        }

        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "More destinations",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(secondaryActions) { (label, action) ->
                    Button(
                        onClick = action,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label)
                    }
                }
            }
        }

        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Sign Out")
        }
    }
}

@Composable
private fun FeatureShell(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            GlassPanel(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

private fun <VM : ViewModel> simpleFactory(
    creator: () -> VM
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return creator() as T
        }
    }
}

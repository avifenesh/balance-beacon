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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.view.HapticFeedbackConstants
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import app.balancebeacon.mobileandroid.ui.theme.GlassSurfaceStrong
import app.balancebeacon.mobileandroid.ui.theme.SkyBlue
import app.balancebeacon.mobileandroid.ui.theme.Slate300
import kotlinx.coroutines.launch

private enum class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home(AppDestination.Dashboard.route, "Home", Icons.Default.Home),
    Transactions(AppDestination.Transactions.route, "Transactions", Icons.Default.Receipt),
    Budgets(AppDestination.Budgets.route, "Budgets", Icons.Default.AccountBalance),
    Sharing(AppDestination.Sharing.route, "Sharing", Icons.Default.People),
    Assistant(AppDestination.Assistant.route, "AI", Icons.Default.AutoAwesome)
}

private val bottomNavRoutes = BottomNavTab.entries.map { it.route }.toSet()

private fun isBottomNavRoute(route: String?): Boolean {
    if (route == null) return false
    val baseRoute = route.substringBefore("?")
    return baseRoute in bottomNavRoutes
}

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

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomNav = isBottomNavRoute(currentRoute)

        val view = LocalView.current

        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomNav) {
                    NavigationBar(
                        containerColor = GlassSurfaceStrong,
                        contentColor = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        BottomNavTab.entries.forEach { tab ->
                            val selected = currentRoute?.substringBefore("?") == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        tab.label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = SkyBlue,
                                    indicatorColor = SkyBlue.copy(alpha = 0.2f),
                                    unselectedIconColor = Slate300,
                                    unselectedTextColor = Slate300
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
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
                                    transactionsRepository = appContainer.transactionsRepository,
                                    accountsRepository = appContainer.accountsRepository
                                )
                            }
                        }
                    )
                    DashboardScreen(
                        dashboardViewModel = dashboardVm,
                        onOpenOverview = { navController.navigate(AppDestination.Overview.route) },
                        onOpenRecurring = { navController.navigate(AppDestination.Recurring.route) },
                        onOpenHoldings = { navController.navigate(AppDestination.Holdings.route) },
                        onOpenOnboarding = { navController.navigate(AppDestination.Onboarding.route) },
                        onOpenSubscription = { navController.navigate(AppDestination.Subscription.route) },
                        onOpenPaywall = { navController.navigate(AppDestination.Paywall.route) },
                        onOpenAccounts = { navController.navigate(AppDestination.Accounts.route) },
                        onOpenCategories = { navController.navigate(AppDestination.Categories.route) },
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
                                    transactionsRepository = appContainer.transactionsRepository,
                                    accountsRepository = appContainer.accountsRepository
                                )
                            }
                        }
                    )
                    FeatureShell(
                        title = "Overview",
                        onBack = { navController.popBackStack() }
                    ) {
                        DashboardOverviewScreen(
                            viewModel = vm,
                            navToAccounts = { navController.navigate(AppDestination.Accounts.route) },
                            navToAssistant = { navController.navigate(AppDestination.Assistant.route) },
                            modifier = Modifier.fillMaxSize()
                        )
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
                    AssistantScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
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
                            simpleFactory {
                                HoldingsViewModel(
                                    holdingsRepository = appContainer.holdingsRepository,
                                    accountsRepository = appContainer.accountsRepository,
                                    categoriesRepository = appContainer.categoriesRepository
                                )
                            }
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
                            simpleFactory {
                                OnboardingViewModel(
                                    onboardingRepository = appContainer.onboardingRepository,
                                    authRepository = appContainer.authRepository,
                                    categoriesRepository = appContainer.categoriesRepository
                                )
                            }
                        }
                    )
                    FeatureShell(
                        title = "Setup Wizard",
                        onBack = { navController.popBackStack() }
                    ) {
                        OnboardingScreen(
                            viewModel = vm,
                            onComplete = { navController.popBackStack() },
                            modifier = Modifier.fillMaxSize()
                        )
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
                                    dashboardRepository = appContainer.dashboardRepository,
                                    accountsRepository = appContainer.accountsRepository,
                                    categoriesRepository = appContainer.categoriesRepository
                                )
                            }
                        }
                    )
                    TransactionsScreen(
                        viewModel = vm,
                        onShareTransaction = { transactionId ->
                            navController.navigate(AppDestination.Sharing.createRoute(transactionId))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                composable(AppDestination.Budgets.route) {
                    val vm: BudgetsViewModel = viewModel(
                        factory = remember {
                            simpleFactory {
                                BudgetsViewModel(
                                    budgetsRepository = appContainer.budgetsRepository,
                                    accountsRepository = appContainer.accountsRepository,
                                    categoriesRepository = appContainer.categoriesRepository
                                )
                            }
                        }
                    )
                    BudgetsScreen(viewModel = vm, modifier = Modifier.fillMaxSize())
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
                    SharingScreen(
                        viewModel = vm,
                        initialTransactionId = initialTransactionId,
                        modifier = Modifier.fillMaxSize()
                    )
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
    onOpenRecurring: () -> Unit,
    onOpenHoldings: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboard()
    }

    val secondaryActions = listOf(
        "Overview" to onOpenOverview,
        "Settings" to onOpenSettings,
        "Recurring" to onOpenRecurring,
        "Holdings" to onOpenHoldings,
        "Accounts" to onOpenAccounts,
        "Categories" to onOpenCategories,
        "Profile" to onOpenProfile,
        "Onboarding" to onOpenOnboarding,
        "Subscription" to onOpenSubscription,
        "Paywall" to onOpenPaywall
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Balance Beacon", style = MaterialTheme.typography.headlineSmall)
                    dashboardState.data?.summary?.let { summary ->
                        Text(
                            text = "Cashflow: ${summary.netResult}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (dashboardState.isLoading && dashboardState.data == null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    dashboardState.error?.let { error ->
                        val displayError = if (error.contains("expired", ignoreCase = true)) {
                            "Session expired"
                        } else if (error.length > 60) {
                            "Could not load data"
                        } else {
                            error
                        }
                        Text(
                            text = displayError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (dashboardState.data != null) {
            val dashboard = dashboardState.data!!
            item {
                DashboardTrendCard(
                    history = dashboard.history,
                    comparison = dashboard.comparison,
                    currencyCode = dashboard.preferredCurrency ?: "USD",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("More", style = MaterialTheme.typography.titleMedium)
                    secondaryActions.chunked(2).forEach { rowItems ->
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
        }

        item {
            TextButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Sign Out",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        modifier = Modifier.fillMaxSize()
    ) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = Color.White
                    )
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content()
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

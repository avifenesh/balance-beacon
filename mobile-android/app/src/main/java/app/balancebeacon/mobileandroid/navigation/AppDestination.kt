package app.balancebeacon.mobileandroid.navigation

sealed interface AppDestination {
    val route: String

    data object Login : AppDestination {
        override val route: String = "login"
    }

    data object Register : AppDestination {
        override val route: String = "register"
    }

    data object ResetPassword : AppDestination {
        override val route: String = "reset-password"
    }

    data object VerifyEmail : AppDestination {
        override val route: String = "verify-email"
    }

    data object Dashboard : AppDestination {
        override val route: String = "dashboard"
    }

    data object Overview : AppDestination {
        override val route: String = "overview"
    }

    data object Assistant : AppDestination {
        override val route: String = "assistant"
    }

    data object Recurring : AppDestination {
        override val route: String = "recurring"
    }

    data object Holdings : AppDestination {
        override val route: String = "holdings"
    }

    data object Onboarding : AppDestination {
        override val route: String = "onboarding"
    }

    data object Subscription : AppDestination {
        override val route: String = "subscription"
    }

    data object Paywall : AppDestination {
        override val route: String = "paywall"
    }

    data object Transactions : AppDestination {
        override val route: String = "transactions"
    }

    data object Budgets : AppDestination {
        override val route: String = "budgets"
    }

    data object Accounts : AppDestination {
        override val route: String = "accounts"
    }

    data object Categories : AppDestination {
        override val route: String = "categories"
    }

    data object Sharing : AppDestination {
        override val route: String = "sharing"
    }

    data object Profile : AppDestination {
        override val route: String = "profile"
    }

    data object Settings : AppDestination {
        override val route: String = "settings"
    }
}

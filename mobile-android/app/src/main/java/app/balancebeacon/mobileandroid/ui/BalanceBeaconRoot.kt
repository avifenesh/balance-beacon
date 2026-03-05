package app.balancebeacon.mobileandroid.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import app.balancebeacon.mobileandroid.core.AppContainer
import app.balancebeacon.mobileandroid.navigation.RootNavHost
import app.balancebeacon.mobileandroid.ui.theme.AppGradientBackground
import app.balancebeacon.mobileandroid.ui.theme.BalanceBeaconTheme

@Composable
fun BalanceBeaconRoot(
    appContainer: AppContainer
) {
    BalanceBeaconTheme {
        AppGradientBackground(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = androidx.compose.ui.graphics.Color.Transparent
            ) {
                RootNavHost(
                    appContainer = appContainer,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

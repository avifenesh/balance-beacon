package app.balancebeacon.mobileandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.balancebeacon.mobileandroid.ui.BalanceBeaconRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as BalanceBeaconApplication).appContainer

        setContent {
            BalanceBeaconRoot(appContainer = appContainer)
        }
    }
}

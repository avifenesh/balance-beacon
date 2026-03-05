package app.balancebeacon.mobileandroid

import android.app.Application
import app.balancebeacon.mobileandroid.core.AppContainer

class BalanceBeaconApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        appContainer.transactionsRepository.schedulePendingSync()
    }
}

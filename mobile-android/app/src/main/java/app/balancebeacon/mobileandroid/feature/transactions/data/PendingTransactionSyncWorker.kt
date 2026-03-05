package app.balancebeacon.mobileandroid.feature.transactions.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.balancebeacon.mobileandroid.BalanceBeaconApplication
import app.balancebeacon.mobileandroid.core.result.AppResult

class PendingTransactionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? BalanceBeaconApplication
            ?: return Result.failure()

        return when (val result = application.appContainer.transactionsRepository.syncPendingTransactionsReport()) {
            is AppResult.Success -> {
                if (result.value.failedCount > 0) {
                    Result.retry()
                } else {
                    Result.success()
                }
            }

            is AppResult.Failure -> Result.retry()
        }
    }
}

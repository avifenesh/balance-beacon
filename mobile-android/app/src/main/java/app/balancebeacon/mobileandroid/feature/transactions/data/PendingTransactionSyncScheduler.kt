package app.balancebeacon.mobileandroid.feature.transactions.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun interface PendingTransactionSyncScheduler {
    fun scheduleSync()
}

class WorkManagerPendingTransactionSyncScheduler(
    context: Context
) : PendingTransactionSyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<PendingTransactionSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "pending-transaction-sync"
    }
}

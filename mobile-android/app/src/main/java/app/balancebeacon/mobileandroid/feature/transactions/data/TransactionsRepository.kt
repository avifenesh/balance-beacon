package app.balancebeacon.mobileandroid.feature.transactions.data

import app.balancebeacon.mobileandroid.core.database.PendingTransactionDao
import app.balancebeacon.mobileandroid.core.database.PendingTransactionEntity
import app.balancebeacon.mobileandroid.core.result.AppError
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
import app.balancebeacon.mobileandroid.feature.transactions.model.CreateTransactionRequest
import app.balancebeacon.mobileandroid.feature.transactions.model.DeleteTransactionResponse
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionDto
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionRequestActionResponse
import app.balancebeacon.mobileandroid.feature.transactions.model.UpdateTransactionRequest

class TransactionsRepository(
    private val transactionsApi: TransactionsApi,
    private val pendingTransactionDao: PendingTransactionDao? = null
) {
    companion object {
        private const val MAX_SYNC_ATTEMPTS = 3
    }

    suspend fun getTransactions(
        accountId: String? = null,
        month: String? = null
    ): AppResult<List<TransactionDto>> {
        return runAppResult {
            transactionsApi.getTransactions(
                accountId = accountId,
                month = month
            ).transactions
        }
    }

    suspend fun createTransaction(request: CreateTransactionRequest): AppResult<TransactionDto> {
        return runAppResult { transactionsApi.createTransaction(request) }
    }

    suspend fun getTransactionById(id: String): AppResult<TransactionDto> {
        return runAppResult { transactionsApi.getTransactionById(id = id) }
    }

    suspend fun updateTransaction(
        id: String,
        request: UpdateTransactionRequest
    ): AppResult<TransactionDto> {
        return runAppResult { transactionsApi.updateTransaction(id = id, request = request) }
    }

    suspend fun deleteTransaction(id: String): AppResult<DeleteTransactionResponse> {
        return runAppResult { transactionsApi.deleteTransaction(id = id) }
    }

    suspend fun approveTransactionRequest(id: String): AppResult<TransactionRequestActionResponse> {
        return runAppResult { transactionsApi.approveTransactionRequest(id = id) }
    }

    suspend fun rejectTransactionRequest(id: String): AppResult<TransactionRequestActionResponse> {
        return runAppResult { transactionsApi.rejectTransactionRequest(id = id) }
    }

    suspend fun enqueueTransaction(request: CreateTransactionRequest): AppResult<Long> {
        val dao = pendingTransactionDao
            ?: return AppResult.Failure(
                AppError(message = "Pending transaction queue is not configured")
            )

        return runAppResult {
            dao.insert(
                PendingTransactionEntity(
                    amount = request.amount,
                    type = request.type,
                    description = request.description,
                    categoryId = request.categoryId,
                    accountId = request.accountId,
                    date = request.date
                )
            )
        }
    }

    suspend fun syncPendingTransactions(maxItems: Int = 50): AppResult<Int> {
        val dao = pendingTransactionDao ?: return AppResult.Success(0)

        return runAppResult {
            val pendingItems = dao.listPending(limit = maxItems, maxAttempts = MAX_SYNC_ATTEMPTS)
            var syncedCount = 0

            pendingItems.forEach { item ->
                val request = CreateTransactionRequest(
                    amount = item.amount,
                    type = item.type,
                    description = item.description,
                    categoryId = item.categoryId,
                    accountId = item.accountId,
                    date = item.date
                )

                runCatching { transactionsApi.createTransaction(request) }
                    .onSuccess {
                        dao.deleteById(item.id)
                        syncedCount += 1
                    }
                    .onFailure { error ->
                        dao.markFailed(id = item.id, error = error.message)
                    }
            }

            syncedCount
        }
    }
}

package app.balancebeacon.mobileandroid.feature.transactions.ui

import app.balancebeacon.mobileandroid.core.database.PendingTransactionDao
import app.balancebeacon.mobileandroid.core.database.PendingTransactionEntity
import app.balancebeacon.mobileandroid.feature.transactions.data.PendingTransactionSyncScheduler
import app.balancebeacon.mobileandroid.feature.transactions.data.TransactionsApi
import app.balancebeacon.mobileandroid.feature.transactions.data.TransactionsRepository
import app.balancebeacon.mobileandroid.feature.transactions.model.CreateTransactionRequest
import app.balancebeacon.mobileandroid.feature.transactions.model.DeleteTransactionResponse
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionDto
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionRequestActionResponse
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionsResponse
import app.balancebeacon.mobileandroid.feature.transactions.model.UpdateTransactionRequest
import app.balancebeacon.mobileandroid.testutil.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun createTransaction_queuesOfflineWhenNetworkFails() = runTest {
        val pendingDao = FakePendingTransactionDao()
        val scheduler = FakePendingTransactionSyncScheduler()
        val api = FakeTransactionsApi(
            transactionsResponse = TransactionsResponse(),
            shouldThrowOnCreate = true
        )
        val repository = TransactionsRepository(
            transactionsApi = api,
            pendingTransactionDao = pendingDao,
            pendingTransactionSyncScheduler = scheduler
        )
        val viewModel = TransactionsViewModel(repository)

        viewModel.createTransaction(
            request = CreateTransactionRequest(
                amount = "15.00",
                type = "EXPENSE",
                description = "Offline coffee",
                categoryId = "cat_1",
                accountId = "acc_1",
                date = "2026-03-05"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Offline mode: transaction queued for sync", state.statusMessage)
        assertNull(state.error)
        assertTrue(state.items.any { it.id.startsWith("pending-") })
        assertEquals(1, scheduler.scheduleCalls)
    }

    @Test
    fun load_showsQueuedSyncMessageWhenPendingItemsSync() = runTest {
        val pendingDao = FakePendingTransactionDao(
            seedItems = listOf(
                PendingTransactionEntity(
                    id = 1L,
                    amount = "20.00",
                    type = "EXPENSE",
                    description = "Queued taxi",
                    categoryId = null,
                    accountId = "acc_1",
                    date = "2026-03-05",
                    attempts = 0,
                    createdAtEpochMs = 1L
                )
            )
        )
        val scheduler = FakePendingTransactionSyncScheduler()
        val api = FakeTransactionsApi(
            transactionsResponse = TransactionsResponse(
                transactions = listOf(
                    TransactionDto(
                        id = "tx_live",
                        amount = "99.00",
                        type = "EXPENSE",
                        description = "Live item",
                        accountId = "acc_1",
                        date = "2026-03-05"
                    )
                )
            ),
            shouldThrowOnCreate = false
        )
        val repository = TransactionsRepository(
            transactionsApi = api,
            pendingTransactionDao = pendingDao,
            pendingTransactionSyncScheduler = scheduler
        )
        val viewModel = TransactionsViewModel(repository)

        viewModel.load(accountId = "acc_1", month = "2026-03")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Synced 1 queued transaction(s)", state.statusMessage)
        assertEquals(1, state.items.size)
        assertEquals("tx_live", state.items.first().id)
        assertTrue(pendingDao.deletedIds.contains(1L))
    }

    private class FakeTransactionsApi(
        private val transactionsResponse: TransactionsResponse,
        private val shouldThrowOnCreate: Boolean
    ) : TransactionsApi {
        override suspend fun getTransactions(
            accountId: String?,
            month: String?,
            categoryId: String?,
            type: String?,
            limit: Int,
            offset: Int
        ): TransactionsResponse {
            return transactionsResponse
        }

        override suspend fun createTransaction(request: CreateTransactionRequest): TransactionDto {
            if (shouldThrowOnCreate) {
                throw IOException("offline")
            }
            return TransactionDto(
                id = "created-${request.accountId}",
                amount = request.amount,
                type = request.type,
                description = request.description,
                categoryId = request.categoryId,
                accountId = request.accountId,
                date = request.date
            )
        }

        override suspend fun getTransactionById(id: String): TransactionDto {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun updateTransaction(
            id: String,
            request: UpdateTransactionRequest
        ): TransactionDto {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun deleteTransaction(id: String): DeleteTransactionResponse {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun approveTransactionRequest(id: String): TransactionRequestActionResponse {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun rejectTransactionRequest(id: String): TransactionRequestActionResponse {
            throw UnsupportedOperationException("Not needed")
        }
    }

    private class FakePendingTransactionDao(
        seedItems: List<PendingTransactionEntity> = emptyList()
    ) : PendingTransactionDao {
        private val items = seedItems.toMutableList()
        private var nextId: Long = (seedItems.maxOfOrNull { it.id } ?: 0L) + 1L
        val deletedIds: MutableList<Long> = mutableListOf()

        override suspend fun insert(item: PendingTransactionEntity): Long {
            val assignedId = if (item.id == 0L) nextId++ else item.id
            items += item.copy(id = assignedId)
            return assignedId
        }

        override suspend fun listPending(limit: Int, maxAttempts: Int): List<PendingTransactionEntity> {
            return items
                .filter { it.attempts < maxAttempts }
                .sortedBy { it.createdAtEpochMs }
                .take(limit)
        }

        override suspend fun deleteById(id: Long) {
            deletedIds += id
            items.removeAll { it.id == id }
        }

        override suspend fun markFailed(id: Long, error: String?) {
            val index = items.indexOfFirst { it.id == id }
            if (index >= 0) {
                val existing = items[index]
                items[index] = existing.copy(
                    attempts = existing.attempts + 1,
                    lastError = error
                )
            }
        }
    }

    private class FakePendingTransactionSyncScheduler : PendingTransactionSyncScheduler {
        var scheduleCalls: Int = 0

        override fun scheduleSync() {
            scheduleCalls += 1
        }
    }
}

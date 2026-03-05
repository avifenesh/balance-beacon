package app.balancebeacon.mobileandroid.feature.transactions.data

import app.balancebeacon.mobileandroid.core.database.PendingTransactionDao
import app.balancebeacon.mobileandroid.core.database.PendingTransactionEntity
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.transactions.model.CreateTransactionRequest
import app.balancebeacon.mobileandroid.feature.transactions.model.DeleteTransactionResponse
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionDto
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionRequestActionResponse
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionsResponse
import app.balancebeacon.mobileandroid.feature.transactions.model.UpdateTransactionRequest
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionsRepositoryTest {
    @Test
    fun getTransactions_returnsSuccessWhenApiSucceeds() = runBlocking {
        val expected = listOf(
            TransactionDto(
                id = "tx_1",
                amount = "10.00",
                type = "expense",
                description = "Coffee",
                categoryId = "cat_1",
                accountId = "acc_1",
                date = "2026-03-01"
            )
        )
        val api = FakeTransactionsApi(
            response = TransactionsResponse(transactions = expected, total = expected.size),
            shouldThrow = false
        )
        val repository = TransactionsRepository(api)

        val result = repository.getTransactions(month = "2026-03")

        assertTrue(result is AppResult.Success)
        val success = result as AppResult.Success
        assertEquals(expected, success.value)
    }

    @Test
    fun getTransactions_returnsFailureWhenApiThrows() = runBlocking {
        val api = FakeTransactionsApi(
            response = TransactionsResponse(),
            shouldThrow = true
        )
        val repository = TransactionsRepository(api)

        val result = repository.getTransactions(month = "2026-03")

        assertTrue(result is AppResult.Failure)
        val failure = result as AppResult.Failure
        assertEquals("Network request failed", failure.error.message)
    }

    @Test
    fun enqueueTransaction_returnsFailureWhenQueueIsNotConfigured() = runBlocking {
        val repository = TransactionsRepository(
            transactionsApi = FakeTransactionsApi(
                response = TransactionsResponse(),
                shouldThrow = false
            )
        )

        val result = repository.enqueueTransaction(request = buildCreateRequest())

        assertTrue(result is AppResult.Failure)
        val failure = result as AppResult.Failure
        assertEquals("Pending transaction queue is not configured", failure.error.message)
    }

    @Test
    fun syncPendingTransactions_syncsSuccessesAndMarksFailures() = runBlocking {
        val pendingDao = FakePendingTransactionDao(
            seedItems = listOf(
                PendingTransactionEntity(
                    id = 1L,
                    amount = "12.00",
                    type = "EXPENSE",
                    description = "Coffee",
                    categoryId = "cat_1",
                    accountId = "acc_ok",
                    date = "2026-03-05",
                    attempts = 0,
                    createdAtEpochMs = 1L
                ),
                PendingTransactionEntity(
                    id = 2L,
                    amount = "18.00",
                    type = "EXPENSE",
                    description = "Taxi",
                    categoryId = "cat_2",
                    accountId = "acc_fail",
                    date = "2026-03-05",
                    attempts = 1,
                    createdAtEpochMs = 2L
                )
            )
        )
        val api = FakeTransactionsApi(
            response = TransactionsResponse(),
            shouldThrow = false,
            failingAccountIds = setOf("acc_fail")
        )
        val repository = TransactionsRepository(
            transactionsApi = api,
            pendingTransactionDao = pendingDao
        )

        val result = repository.syncPendingTransactions(maxItems = 10)

        assertTrue(result is AppResult.Success)
        val success = result as AppResult.Success
        assertEquals(1, success.value)
        assertEquals(10, pendingDao.lastLimit)
        assertEquals(3, pendingDao.lastMaxAttempts)
        assertEquals(listOf(1L), pendingDao.deletedIds)
        assertEquals(1, pendingDao.failedMarks.size)
        assertEquals(2L, pendingDao.failedMarks.first().first)
        assertTrue((pendingDao.failedMarks.first().second ?: "").contains("forced failure"))
    }

    @Test
    fun syncPendingTransactions_skipsItemsThatReachedRetryLimit() = runBlocking {
        val pendingDao = FakePendingTransactionDao(
            seedItems = listOf(
                PendingTransactionEntity(
                    id = 99L,
                    amount = "30.00",
                    type = "EXPENSE",
                    description = "Skipped",
                    categoryId = null,
                    accountId = "acc_ignored",
                    date = "2026-03-05",
                    attempts = 3,
                    createdAtEpochMs = 10L
                )
            )
        )
        val api = FakeTransactionsApi(
            response = TransactionsResponse(),
            shouldThrow = false
        )
        val repository = TransactionsRepository(
            transactionsApi = api,
            pendingTransactionDao = pendingDao
        )

        val result = repository.syncPendingTransactions()

        assertTrue(result is AppResult.Success)
        val success = result as AppResult.Success
        assertEquals(0, success.value)
        assertTrue(api.createdRequests.isEmpty())
        assertTrue(pendingDao.deletedIds.isEmpty())
        assertTrue(pendingDao.failedMarks.isEmpty())
    }

    private class FakeTransactionsApi(
        private val response: TransactionsResponse,
        private val shouldThrow: Boolean,
        private val failingAccountIds: Set<String> = emptySet()
    ) : TransactionsApi {
        val createdRequests: MutableList<CreateTransactionRequest> = mutableListOf()

        override suspend fun getTransactions(
            accountId: String?,
            month: String?,
            categoryId: String?,
            type: String?,
            limit: Int,
            offset: Int
        ): TransactionsResponse {
            if (shouldThrow) throw IOException("network down")
            return response
        }

        override suspend fun createTransaction(request: CreateTransactionRequest): TransactionDto {
            createdRequests += request
            if (request.accountId in failingAccountIds) {
                throw IOException("forced failure for ${request.accountId}")
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
            throw UnsupportedOperationException("Not used by this test")
        }

        override suspend fun updateTransaction(
            id: String,
            request: UpdateTransactionRequest
        ): TransactionDto {
            throw UnsupportedOperationException("Not used by this test")
        }

        override suspend fun deleteTransaction(id: String): DeleteTransactionResponse {
            throw UnsupportedOperationException("Not used by this test")
        }

        override suspend fun approveTransactionRequest(id: String): TransactionRequestActionResponse {
            throw UnsupportedOperationException("Not used by this test")
        }

        override suspend fun rejectTransactionRequest(id: String): TransactionRequestActionResponse {
            throw UnsupportedOperationException("Not used by this test")
        }
    }

    private class FakePendingTransactionDao(
        seedItems: List<PendingTransactionEntity> = emptyList()
    ) : PendingTransactionDao {
        private val items = seedItems.toMutableList()
        private var nextId: Long = (seedItems.maxOfOrNull { it.id } ?: 0L) + 1L

        var lastLimit: Int = 50
        var lastMaxAttempts: Int = 3
        val deletedIds: MutableList<Long> = mutableListOf()
        val failedMarks: MutableList<Pair<Long, String?>> = mutableListOf()

        override suspend fun insert(item: PendingTransactionEntity): Long {
            val assignedId = if (item.id == 0L) nextId++ else item.id
            items += item.copy(id = assignedId)
            return assignedId
        }

        override suspend fun listPending(limit: Int, maxAttempts: Int): List<PendingTransactionEntity> {
            lastLimit = limit
            lastMaxAttempts = maxAttempts
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
            failedMarks += id to error
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

    private fun buildCreateRequest(): CreateTransactionRequest {
        return CreateTransactionRequest(
            amount = "10.00",
            type = "EXPENSE",
            description = "Coffee",
            categoryId = "cat_1",
            accountId = "acc_1",
            date = "2026-03-05"
        )
    }
}

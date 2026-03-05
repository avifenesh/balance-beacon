package app.balancebeacon.mobileandroid.feature.transactions.data

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

    private class FakeTransactionsApi(
        private val response: TransactionsResponse,
        private val shouldThrow: Boolean
    ) : TransactionsApi {
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
            throw UnsupportedOperationException("Not used by this test")
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
}

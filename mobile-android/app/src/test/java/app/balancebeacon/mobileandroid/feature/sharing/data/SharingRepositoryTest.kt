package app.balancebeacon.mobileandroid.feature.sharing.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.auth.model.MessageResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.CreateSharedExpenseParticipantRequest
import app.balancebeacon.mobileandroid.feature.sharing.model.CreateSharedExpenseRequest
import app.balancebeacon.mobileandroid.feature.sharing.model.DeclineShareResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.MarkPaidResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.SettleAllRequest
import app.balancebeacon.mobileandroid.feature.sharing.model.SettleAllResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.ShareUserDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharedExpenseDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharingResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.UserLookupResponse
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharingRepositoryTest {
    @Test
    fun createSharedExpense_trimsDescriptionBeforeRequest() = runBlocking {
        val api = FakeSharingApi()
        val repository = SharingRepository(api)

        val result = repository.createSharedExpense(
            transactionId = "tx_1",
            splitType = "EQUAL",
            participants = listOf(
                CreateSharedExpenseParticipantRequest(
                    email = "friend@example.com",
                    shareAmount = 50.0
                )
            ),
            description = "  Dinner split  "
        )

        assertTrue(result is AppResult.Success)
        assertEquals("Dinner split", api.lastCreateRequest?.description)
    }

    @Test
    fun lookupUser_trimsEmailBeforeLookup() = runBlocking {
        val api = FakeSharingApi()
        val repository = SharingRepository(api)

        val result = repository.lookupUser("  teammate@example.com  ")

        assertTrue(result is AppResult.Success)
        assertEquals("teammate@example.com", api.lastLookupEmail)
    }

    @Test
    fun settleAllWithUser_normalizesInputs() = runBlocking {
        val api = FakeSharingApi()
        val repository = SharingRepository(api)

        val result = repository.settleAllWithUser(
            targetUserId = "  user_42  ",
            currency = "  ils  "
        )

        assertTrue(result is AppResult.Success)
        assertEquals("user_42", api.lastSettleRequest?.targetUserId)
        assertEquals("ILS", api.lastSettleRequest?.currency)
    }

    @Test
    fun createSharedExpense_returnsFailureWhenApiThrows() = runBlocking {
        val api = FakeSharingApi(shouldThrowOnCreate = true)
        val repository = SharingRepository(api)

        val result = repository.createSharedExpense(
            transactionId = "tx_1",
            splitType = "EQUAL",
            participants = emptyList(),
            description = "test"
        )

        assertTrue(result is AppResult.Failure)
        val failure = result as AppResult.Failure
        assertEquals("Network request failed", failure.error.message)
    }

    private class FakeSharingApi(
        private val shouldThrowOnCreate: Boolean = false
    ) : SharingApi {
        var lastCreateRequest: CreateSharedExpenseRequest? = null
        var lastLookupEmail: String? = null
        var lastSettleRequest: SettleAllRequest? = null

        override suspend fun getSharing(): SharingResponse = SharingResponse()

        override suspend fun createSharedExpense(request: CreateSharedExpenseRequest): SharedExpenseDto {
            if (shouldThrowOnCreate) {
                throw IOException("network down")
            }
            lastCreateRequest = request
            return SharedExpenseDto(
                id = "share_1",
                transactionId = request.transactionId,
                splitType = request.splitType,
                description = request.description,
                totalAmount = "100.00",
                currency = "USD",
                createdAt = "2026-03-05T00:00:00Z"
            )
        }

        override suspend fun markParticipantPaid(
            participantId: String,
            body: Map<String, String>
        ): MarkPaidResponse {
            return MarkPaidResponse(id = participantId, status = "PAID")
        }

        override suspend fun declineShare(
            participantId: String,
            body: Map<String, String>
        ): DeclineShareResponse {
            return DeclineShareResponse(id = participantId, status = "DECLINED")
        }

        override suspend fun deleteShare(id: String): MessageResponse {
            return MessageResponse(message = "Deleted")
        }

        override suspend fun sendReminder(
            participantId: String,
            body: Map<String, String>
        ): MessageResponse {
            return MessageResponse(message = "Reminder sent")
        }

        override suspend fun lookupUser(email: String): UserLookupResponse {
            lastLookupEmail = email
            return UserLookupResponse(user = ShareUserDto(id = "user_1", email = email))
        }

        override suspend fun settleAllWithUser(request: SettleAllRequest): SettleAllResponse {
            lastSettleRequest = request
            return SettleAllResponse(settledCount = 3)
        }
    }
}

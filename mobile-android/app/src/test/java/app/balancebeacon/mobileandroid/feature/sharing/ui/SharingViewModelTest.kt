package app.balancebeacon.mobileandroid.feature.sharing.ui

import app.balancebeacon.mobileandroid.feature.auth.model.MessageResponse
import app.balancebeacon.mobileandroid.feature.sharing.data.SharingApi
import app.balancebeacon.mobileandroid.feature.sharing.data.SharingRepository
import app.balancebeacon.mobileandroid.feature.sharing.model.CreateSharedExpenseRequest
import app.balancebeacon.mobileandroid.feature.sharing.model.DeclineShareResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.MarkPaidResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.SettleAllRequest
import app.balancebeacon.mobileandroid.feature.sharing.model.SettleAllResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.ShareUserDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharedExpenseDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharingResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.UserLookupResponse
import app.balancebeacon.mobileandroid.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun createSharedExpense_parsesPercentageParticipants() = runTest {
        val api = FakeSharingApi()
        val viewModel = SharingViewModel(SharingRepository(api))

        viewModel.createSharedExpense(
            transactionId = "tx_1",
            splitType = "PERCENTAGE",
            participantsInput = "alice@example.com:60, bob@example.com:40",
            description = "Trip split"
        )
        advanceUntilIdle()

        val request = api.lastCreateRequest
        assertEquals("PERCENTAGE", request?.splitType)
        assertEquals(2, request?.participants?.size)
        assertEquals(60.0, request?.participants?.get(0)?.sharePercentage ?: 0.0, 0.001)
        assertNull(request?.participants?.get(0)?.shareAmount)
        assertEquals(40.0, request?.participants?.get(1)?.sharePercentage ?: 0.0, 0.001)
        assertEquals("Shared expense created", viewModel.uiState.value.actionMessage)
        assertTrue(!viewModel.uiState.value.actionMessageIsError)
    }

    @Test
    fun createSharedExpense_rejectsDuplicateParticipantEmails() = runTest {
        val api = FakeSharingApi()
        val viewModel = SharingViewModel(SharingRepository(api))

        viewModel.createSharedExpense(
            transactionId = "tx_1",
            splitType = "FIXED",
            participantsInput = "dup@example.com:25, dup@example.com:10",
            description = "Duplicate test"
        )

        assertNull(api.lastCreateRequest)
        assertEquals("Duplicate participant emails are not allowed", viewModel.uiState.value.actionMessage)
        assertTrue(viewModel.uiState.value.actionMessageIsError)
    }

    private class FakeSharingApi : SharingApi {
        var lastCreateRequest: CreateSharedExpenseRequest? = null

        override suspend fun getSharing(): SharingResponse = SharingResponse()

        override suspend fun createSharedExpense(request: CreateSharedExpenseRequest): SharedExpenseDto {
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
            return UserLookupResponse(user = ShareUserDto(id = "user_1", email = email))
        }

        override suspend fun settleAllWithUser(request: SettleAllRequest): SettleAllResponse {
            return SettleAllResponse(settledCount = 1)
        }
    }
}

package app.balancebeacon.mobileandroid.feature.sharing.ui

import app.balancebeacon.mobileandroid.feature.auth.model.MessageResponse
import app.balancebeacon.mobileandroid.feature.sharing.data.SharingApi
import app.balancebeacon.mobileandroid.feature.sharing.data.SharingRepository
import app.balancebeacon.mobileandroid.feature.sharing.model.CreateSharedExpenseRequest
import app.balancebeacon.mobileandroid.feature.sharing.model.DeclineShareResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.MarkPaidResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.SettleAllRequest
import app.balancebeacon.mobileandroid.feature.sharing.model.SettleAllResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.ShareParticipantDto
import app.balancebeacon.mobileandroid.feature.sharing.model.ShareUserDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharedExpenseDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharedWithMeParticipationDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharingResponse
import app.balancebeacon.mobileandroid.feature.sharing.model.UserLookupResponse
import app.balancebeacon.mobileandroid.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ---- legacy: createSharedExpense via participantsInput string ----

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
        assertFalse(viewModel.uiState.value.actionMessageIsError)
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

    // ---- structured participant state: addParticipant / removeParticipant ----

    @Test
    fun addParticipant_lookupsUserAndAppendsToParticipantList() = runTest {
        val api = FakeSharingApi()
        val viewModel = SharingViewModel(SharingRepository(api))

        viewModel.onNewParticipantEmailChanged("alice@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.participants.size)
        assertEquals("alice@example.com", viewModel.uiState.value.participants[0].email)
        assertEquals("", viewModel.uiState.value.newParticipantEmail)
        assertFalse(viewModel.uiState.value.isLookingUpParticipant)
    }

    @Test
    fun addParticipant_rejectsInvalidEmail() = runTest {
        val viewModel = SharingViewModel(SharingRepository(FakeSharingApi()))

        viewModel.onNewParticipantEmailChanged("not-an-email")
        viewModel.addParticipant()

        assertEquals(0, viewModel.uiState.value.participants.size)
        assertEquals("Enter a valid email", viewModel.uiState.value.actionMessage)
        assertTrue(viewModel.uiState.value.actionMessageIsError)
    }

    @Test
    fun addParticipant_rejectsDuplicateEmail() = runTest {
        val viewModel = SharingViewModel(SharingRepository(FakeSharingApi()))

        viewModel.onNewParticipantEmailChanged("alice@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()

        viewModel.onNewParticipantEmailChanged("alice@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.participants.size)
        assertEquals("Participant already added", viewModel.uiState.value.actionMessage)
        assertTrue(viewModel.uiState.value.actionMessageIsError)
    }

    @Test
    fun removeParticipant_removesCorrectEntryByIndex() = runTest {
        val viewModel = SharingViewModel(SharingRepository(FakeSharingApi()))

        viewModel.onNewParticipantEmailChanged("alice@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()
        viewModel.onNewParticipantEmailChanged("bob@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()

        viewModel.removeParticipant(0)

        assertEquals(1, viewModel.uiState.value.participants.size)
        assertEquals("bob@example.com", viewModel.uiState.value.participants[0].email)
    }

    @Test
    fun updateParticipantValue_updatesShareValueForTargetIndex() = runTest {
        val viewModel = SharingViewModel(SharingRepository(FakeSharingApi()))

        viewModel.onNewParticipantEmailChanged("alice@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()

        viewModel.updateParticipantValue(0, 75.0)

        assertEquals(75.0, viewModel.uiState.value.participants[0].shareValue ?: 0.0, 0.001)
    }

    // ---- createStructuredSharedExpense ----

    @Test
    fun createStructuredSharedExpense_requiresTransactionId() = runTest {
        val viewModel = SharingViewModel(SharingRepository(FakeSharingApi()))

        viewModel.onNewParticipantEmailChanged("alice@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()

        viewModel.createStructuredSharedExpense()

        assertEquals("Transaction ID is required", viewModel.uiState.value.actionMessage)
        assertTrue(viewModel.uiState.value.actionMessageIsError)
    }

    @Test
    fun createStructuredSharedExpense_requiresAtLeastOneParticipant() = runTest {
        val viewModel = SharingViewModel(SharingRepository(FakeSharingApi()))

        viewModel.onCreateTransactionIdChanged("tx_1")
        viewModel.createStructuredSharedExpense()

        assertEquals("Add at least one participant", viewModel.uiState.value.actionMessage)
        assertTrue(viewModel.uiState.value.actionMessageIsError)
    }

    @Test
    fun createStructuredSharedExpense_withEqualSplit_sendsCorrectRequestAndResetsState() = runTest {
        val api = FakeSharingApi()
        val viewModel = SharingViewModel(SharingRepository(api))

        viewModel.onCreateTransactionIdChanged("tx_equal")
        viewModel.onSplitTypeChanged("EQUAL")
        viewModel.onNewParticipantEmailChanged("alice@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()
        viewModel.onNewParticipantEmailChanged("bob@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()

        viewModel.createStructuredSharedExpense()
        advanceUntilIdle()

        val request = api.lastCreateRequest
        assertEquals("tx_equal", request?.transactionId)
        assertEquals("EQUAL", request?.splitType)
        assertEquals(2, request?.participants?.size)
        // State resets on success
        assertEquals(0, viewModel.uiState.value.participants.size)
        assertEquals("", viewModel.uiState.value.createTransactionId)
        assertEquals("Shared expense created", viewModel.uiState.value.actionMessage)
        assertFalse(viewModel.uiState.value.actionMessageIsError)
    }

    @Test
    fun createStructuredSharedExpense_withFixedSplit_rejectsParticipantsWithoutAmount() = runTest {
        val viewModel = SharingViewModel(SharingRepository(FakeSharingApi()))

        viewModel.onCreateTransactionIdChanged("tx_fixed")
        viewModel.onSplitTypeChanged("FIXED")
        viewModel.onNewParticipantEmailChanged("alice@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()
        // No shareValue set -> amount defaults to null (treated as 0)

        viewModel.createStructuredSharedExpense()

        assertEquals("Each participant needs a fixed amount", viewModel.uiState.value.actionMessage)
        assertTrue(viewModel.uiState.value.actionMessageIsError)
    }

    @Test
    fun createStructuredSharedExpense_withPercentageSplit_rejectsTotalExceeding100() = runTest {
        val viewModel = SharingViewModel(SharingRepository(FakeSharingApi()))

        viewModel.onCreateTransactionIdChanged("tx_pct")
        viewModel.onSplitTypeChanged("PERCENTAGE")
        viewModel.onNewParticipantEmailChanged("alice@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()
        viewModel.updateParticipantValue(0, 60.0)
        viewModel.onNewParticipantEmailChanged("bob@example.com")
        viewModel.addParticipant()
        advanceUntilIdle()
        viewModel.updateParticipantValue(1, 60.0) // total = 120, exceeds 100

        viewModel.createStructuredSharedExpense()

        assertEquals("Total percentage cannot exceed 100", viewModel.uiState.value.actionMessage)
        assertTrue(viewModel.uiState.value.actionMessageIsError)
    }

    // ---- onSplitTypeChanged ----

    @Test
    fun onSplitTypeChanged_updatesSplitTypeAndClearsActionMessage() {
        val viewModel = SharingViewModel(SharingRepository(FakeSharingApi()))

        viewModel.onSplitTypeChanged("PERCENTAGE")

        assertEquals("PERCENTAGE", viewModel.uiState.value.createSplitType)
        assertNull(viewModel.uiState.value.actionMessage)
    }

    // ---- load ----

    @Test
    fun load_populatesSharedByMeAndSharedWithMeOnSuccess() = runTest {
        val expense = SharedExpenseDto(
            id = "share_1",
            transactionId = "tx_1",
            splitType = "EQUAL",
            totalAmount = "100.00",
            currency = "USD",
            createdAt = "2026-03-05T00:00:00Z"
        )
        val participation = SharedWithMeParticipationDto(
            id = "part_1",
            shareAmount = "50.00",
            status = "PENDING"
        )
        val api = FakeSharingApi(
            sharingResponse = SharingResponse(
                sharedExpenses = listOf(expense),
                expensesSharedWithMe = listOf(participation)
            )
        )
        val viewModel = SharingViewModel(SharingRepository(api))

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.sharedByMe.size)
        assertEquals("share_1", state.sharedByMe[0].id)
        assertEquals(1, state.sharedWithMe.size)
        assertEquals("part_1", state.sharedWithMe[0].id)
        assertNull(state.error)
    }

    // ---- markParticipantPaid ----

    @Test
    fun markParticipantPaid_updatesParticipantStatusInSharedByMe() = runTest {
        val participant = ShareParticipantDto(id = "part_a", shareAmount = "50.00", status = "PENDING")
        val expense = SharedExpenseDto(
            id = "share_a",
            transactionId = "tx_a",
            splitType = "EQUAL",
            totalAmount = "100.00",
            currency = "USD",
            createdAt = "2026-03-05T00:00:00Z",
            participants = listOf(participant)
        )
        val api = FakeSharingApi(
            sharingResponse = SharingResponse(sharedExpenses = listOf(expense))
        )
        val viewModel = SharingViewModel(SharingRepository(api))
        viewModel.load()
        advanceUntilIdle()

        viewModel.markParticipantPaid("part_a")
        advanceUntilIdle()

        val updatedParticipant = viewModel.uiState.value.sharedByMe[0].participants[0]
        assertEquals("PAID", updatedParticipant.status)
        assertEquals("Participant marked as paid", viewModel.uiState.value.actionMessage)
        assertFalse(viewModel.uiState.value.isActionInProgress)
    }

    // ---- declineShare ----

    @Test
    fun declineShare_removesParticipationFromSharedWithMe() = runTest {
        val participation = SharedWithMeParticipationDto(
            id = "part_b",
            shareAmount = "25.00",
            status = "PENDING"
        )
        val api = FakeSharingApi(
            sharingResponse = SharingResponse(expensesSharedWithMe = listOf(participation))
        )
        val viewModel = SharingViewModel(SharingRepository(api))
        viewModel.load()
        advanceUntilIdle()

        viewModel.declineShare("part_b")
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.sharedWithMe.size)
        assertEquals("Share declined", viewModel.uiState.value.actionMessage)
    }

    // ---- lookupUser ----

    @Test
    fun lookupUser_populatesLookedUpUserOnSuccess() = runTest {
        val api = FakeSharingApi()
        val viewModel = SharingViewModel(SharingRepository(api))

        viewModel.lookupUser("alice@example.com")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("alice@example.com", state.lookedUpUser?.email)
        assertFalse(state.actionMessageIsError)
        assertTrue(state.actionMessage?.contains("User found") == true)
    }

    @Test
    fun lookupUser_setsErrorForInvalidEmail() {
        val viewModel = SharingViewModel(SharingRepository(FakeSharingApi()))

        viewModel.lookupUser("not-an-email")

        assertEquals("Email must be valid", viewModel.uiState.value.actionMessage)
        assertTrue(viewModel.uiState.value.actionMessageIsError)
    }

    // ---- fakes ----

    private class FakeSharingApi(
        private val sharingResponse: SharingResponse = SharingResponse()
    ) : SharingApi {
        var lastCreateRequest: CreateSharedExpenseRequest? = null

        override suspend fun getSharing(): SharingResponse = sharingResponse

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
        ): MarkPaidResponse = MarkPaidResponse(
            id = participantId,
            status = "PAID",
            paidAt = "2026-03-06T10:00:00Z"
        )

        override suspend fun declineShare(
            participantId: String,
            body: Map<String, String>
        ): DeclineShareResponse = DeclineShareResponse(id = participantId, status = "DECLINED")

        override suspend fun deleteShare(id: String): MessageResponse =
            MessageResponse(message = "Deleted")

        override suspend fun sendReminder(
            participantId: String,
            body: Map<String, String>
        ): MessageResponse = MessageResponse(message = "Reminder sent")

        override suspend fun lookupUser(email: String): UserLookupResponse =
            UserLookupResponse(user = ShareUserDto(id = "user_1", email = email, displayName = "Alice"))

        override suspend fun settleAllWithUser(request: SettleAllRequest): SettleAllResponse =
            SettleAllResponse(settledCount = 1)
    }
}

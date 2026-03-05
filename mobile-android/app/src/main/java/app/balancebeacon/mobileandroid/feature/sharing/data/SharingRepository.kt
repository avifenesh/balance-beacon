package app.balancebeacon.mobileandroid.feature.sharing.data

import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.core.result.runAppResult
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

class SharingRepository(
    private val sharingApi: SharingApi
) {
    suspend fun getSharing(): AppResult<SharingResponse> {
        return runAppResult { sharingApi.getSharing() }
    }

    suspend fun createSharedExpense(
        transactionId: String,
        splitType: String,
        participants: List<CreateSharedExpenseParticipantRequest>,
        description: String? = null
    ): AppResult<SharedExpenseDto> {
        return runAppResult {
            sharingApi.createSharedExpense(
                request = CreateSharedExpenseRequest(
                    transactionId = transactionId,
                    splitType = splitType,
                    description = description?.trim(),
                    participants = participants
                )
            )
        }
    }

    suspend fun markParticipantPaid(participantId: String): AppResult<MarkPaidResponse> {
        return runAppResult { sharingApi.markParticipantPaid(participantId = participantId) }
    }

    suspend fun declineShare(participantId: String): AppResult<DeclineShareResponse> {
        return runAppResult { sharingApi.declineShare(participantId = participantId) }
    }

    suspend fun deleteShare(sharedExpenseId: String): AppResult<MessageResponse> {
        return runAppResult { sharingApi.deleteShare(id = sharedExpenseId) }
    }

    suspend fun sendReminder(participantId: String): AppResult<MessageResponse> {
        return runAppResult { sharingApi.sendReminder(participantId = participantId) }
    }

    suspend fun lookupUser(email: String): AppResult<ShareUserDto> {
        return runAppResult { sharingApi.lookupUser(email = email.trim()).user }
    }

    suspend fun settleAllWithUser(targetUserId: String, currency: String): AppResult<SettleAllResponse> {
        return runAppResult {
            sharingApi.settleAllWithUser(
                request = SettleAllRequest(
                    targetUserId = targetUserId.trim(),
                    currency = currency.trim().uppercase()
                )
            )
        }
    }
}

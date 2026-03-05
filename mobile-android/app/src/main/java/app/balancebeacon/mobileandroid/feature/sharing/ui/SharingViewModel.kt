package app.balancebeacon.mobileandroid.feature.sharing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.sharing.data.SharingRepository
import app.balancebeacon.mobileandroid.feature.sharing.model.CreateSharedExpenseParticipantRequest
import app.balancebeacon.mobileandroid.feature.sharing.model.PaymentHistoryItemDto
import app.balancebeacon.mobileandroid.feature.sharing.model.ShareUserDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SettlementBalanceDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharedExpenseDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharedWithMeParticipationDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SharingUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val sharedByMe: List<SharedExpenseDto> = emptyList(),
    val sharedWithMe: List<SharedWithMeParticipationDto> = emptyList(),
    val settlementBalances: List<SettlementBalanceDto> = emptyList(),
    val paymentHistory: List<PaymentHistoryItemDto> = emptyList(),
    val lookedUpUser: ShareUserDto? = null,
    val actionMessage: String? = null,
    val actionMessageIsError: Boolean = false,
    val error: String? = null
)

class SharingViewModel(
    private val sharingRepository: SharingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SharingUiState())
    val uiState: StateFlow<SharingUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = sharingRepository.getSharing()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        sharedByMe = result.value.sharedExpenses,
                        sharedWithMe = result.value.expensesSharedWithMe,
                        settlementBalances = result.value.settlementBalances,
                        paymentHistory = result.value.paymentHistory,
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun createSharedExpense(
        transactionId: String,
        participantEmail: String,
        shareAmount: String,
        description: String
    ) {
        if (_uiState.value.isActionInProgress) return

        val normalizedTransactionId = transactionId.trim()
        if (normalizedTransactionId.isBlank()) {
            updateActionError("Transaction ID is required")
            return
        }

        val normalizedEmail = participantEmail.trim().lowercase()
        if (normalizedEmail.isBlank()) {
            updateActionError("Participant email is required")
            return
        }
        if (!normalizedEmail.contains("@")) {
            updateActionError("Participant email must be valid")
            return
        }

        val parsedShareAmount = shareAmount.trim().toDoubleOrNull()
        if (parsedShareAmount == null || parsedShareAmount <= 0.0) {
            updateActionError("Share amount must be greater than 0")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    actionMessage = null,
                    actionMessageIsError = false
                )
            }

            when (
                val result = sharingRepository.createSharedExpense(
                    transactionId = normalizedTransactionId,
                    splitType = "EQUAL",
                    participants = listOf(
                        CreateSharedExpenseParticipantRequest(
                            email = normalizedEmail,
                            shareAmount = parsedShareAmount
                        )
                    ),
                    description = description.trim().takeIf { it.isNotBlank() }
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        sharedByMe = listOf(result.value) + it.sharedByMe,
                        actionMessage = "Shared expense created",
                        actionMessageIsError = false
                    )
                }

                is AppResult.Failure -> updateActionError(result.error.message)
            }
        }
    }

    fun markParticipantPaid(participantId: String) {
        if (_uiState.value.isActionInProgress) return

        val normalizedParticipantId = participantId.trim()
        if (normalizedParticipantId.isBlank()) {
            updateActionError("Participant ID is required")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    actionMessage = null,
                    actionMessageIsError = false
                )
            }

            when (val result = sharingRepository.markParticipantPaid(normalizedParticipantId)) {
                is AppResult.Success -> _uiState.update { state ->
                    state.copy(
                        isActionInProgress = false,
                        sharedByMe = state.sharedByMe.map { expense ->
                            expense.copy(
                                participants = expense.participants.map { participant ->
                                    if (participant.id == result.value.id) {
                                        participant.copy(
                                            status = result.value.status,
                                            paidAt = result.value.paidAt
                                        )
                                    } else {
                                        participant
                                    }
                                }
                            )
                        },
                        sharedWithMe = state.sharedWithMe.map { participation ->
                            if (participation.id == result.value.id) {
                                participation.copy(
                                    status = result.value.status,
                                    paidAt = result.value.paidAt
                                )
                            } else {
                                participation
                            }
                        },
                        actionMessage = "Participant marked as paid",
                        actionMessageIsError = false
                    )
                }

                is AppResult.Failure -> updateActionError(result.error.message)
            }
        }
    }

    fun declineShare(participantId: String) {
        if (_uiState.value.isActionInProgress) return

        val normalizedParticipantId = participantId.trim()
        if (normalizedParticipantId.isBlank()) {
            updateActionError("Participant ID is required")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    actionMessage = null,
                    actionMessageIsError = false
                )
            }

            when (val result = sharingRepository.declineShare(normalizedParticipantId)) {
                is AppResult.Success -> _uiState.update { state ->
                    state.copy(
                        isActionInProgress = false,
                        sharedWithMe = state.sharedWithMe.filterNot { it.id == result.value.id },
                        actionMessage = "Share declined",
                        actionMessageIsError = false
                    )
                }

                is AppResult.Failure -> updateActionError(result.error.message)
            }
        }
    }

    fun deleteShare(sharedExpenseId: String) {
        if (_uiState.value.isActionInProgress) return

        val normalizedSharedExpenseId = sharedExpenseId.trim()
        if (normalizedSharedExpenseId.isBlank()) {
            updateActionError("Shared expense ID is required")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    actionMessage = null,
                    actionMessageIsError = false
                )
            }

            when (val result = sharingRepository.deleteShare(normalizedSharedExpenseId)) {
                is AppResult.Success -> _uiState.update { state ->
                    state.copy(
                        isActionInProgress = false,
                        sharedByMe = state.sharedByMe.filterNot { it.id == normalizedSharedExpenseId },
                        actionMessage = result.value.message.ifBlank { "Shared expense deleted" },
                        actionMessageIsError = false
                    )
                }

                is AppResult.Failure -> updateActionError(result.error.message)
            }
        }
    }

    fun sendReminder(participantId: String) {
        if (_uiState.value.isActionInProgress) return

        val normalizedParticipantId = participantId.trim()
        if (normalizedParticipantId.isBlank()) {
            updateActionError("Participant ID is required")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    actionMessage = null,
                    actionMessageIsError = false
                )
            }

            when (val result = sharingRepository.sendReminder(normalizedParticipantId)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        actionMessage = result.value.message.ifBlank { "Reminder sent" },
                        actionMessageIsError = false
                    )
                }

                is AppResult.Failure -> updateActionError(result.error.message)
            }
        }
    }

    fun lookupUser(email: String) {
        if (_uiState.value.isActionInProgress) return

        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank()) {
            updateActionError("Email is required")
            return
        }
        if (!normalizedEmail.contains("@")) {
            updateActionError("Email must be valid")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    actionMessage = null,
                    actionMessageIsError = false
                )
            }

            when (val result = sharingRepository.lookupUser(normalizedEmail)) {
                is AppResult.Success -> {
                    val user = result.value
                    val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            lookedUpUser = user,
                            actionMessage = "User found: $displayName",
                            actionMessageIsError = false
                        )
                    }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        lookedUpUser = null,
                        actionMessage = result.error.message,
                        actionMessageIsError = true
                    )
                }
            }
        }
    }

    fun settleAllWithUser(targetUserId: String, currency: String) {
        if (_uiState.value.isActionInProgress) return

        val normalizedTargetUserId = targetUserId.trim()
        if (normalizedTargetUserId.isBlank()) {
            updateActionError("Target user is required")
            return
        }

        val normalizedCurrency = currency.trim().uppercase()
        if (normalizedCurrency.isBlank()) {
            updateActionError("Currency is required")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    actionMessage = null,
                    actionMessageIsError = false
                )
            }

            when (
                val result = sharingRepository.settleAllWithUser(
                    targetUserId = normalizedTargetUserId,
                    currency = normalizedCurrency
                )
            ) {
                is AppResult.Success -> {
                    val settledCount = result.value.settledCount
                    val successMessage = if (settledCount == 1) {
                        "Settled 1 payment"
                    } else {
                        "Settled $settledCount payments"
                    }
                    reloadSharingDataAfterAction(successMessage)
                }

                is AppResult.Failure -> updateActionError(result.error.message)
            }
        }
    }

    private fun updateActionError(message: String?) {
        _uiState.update {
            it.copy(
                isActionInProgress = false,
                actionMessage = message ?: "Unexpected error",
                actionMessageIsError = true
            )
        }
    }

    private suspend fun reloadSharingDataAfterAction(successMessage: String) {
        when (val sharingResult = sharingRepository.getSharing()) {
            is AppResult.Success -> _uiState.update {
                it.copy(
                    isActionInProgress = false,
                    sharedByMe = sharingResult.value.sharedExpenses,
                    sharedWithMe = sharingResult.value.expensesSharedWithMe,
                    settlementBalances = sharingResult.value.settlementBalances,
                    paymentHistory = sharingResult.value.paymentHistory,
                    actionMessage = successMessage,
                    actionMessageIsError = false,
                    error = null
                )
            }

            is AppResult.Failure -> _uiState.update {
                it.copy(
                    isActionInProgress = false,
                    actionMessage = successMessage,
                    actionMessageIsError = false,
                    error = sharingResult.error.message
                )
            }
        }
    }
}

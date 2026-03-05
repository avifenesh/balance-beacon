package app.balancebeacon.mobileandroid.feature.transactions.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.dashboard.data.DashboardRepository
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardTransactionRequestDto
import app.balancebeacon.mobileandroid.feature.transactions.data.TransactionsRepository
import app.balancebeacon.mobileandroid.feature.transactions.model.CreateTransactionRequest
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionDto
import app.balancebeacon.mobileandroid.feature.transactions.model.UpdateTransactionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class TransactionsUiState(
    val isLoading: Boolean = false,
    val items: List<TransactionDto> = emptyList(),
    val requestItems: List<DashboardTransactionRequestDto> = emptyList(),
    val activeAccountId: String? = null,
    val activeMonth: String? = null,
    val requestActionInProgressId: String? = null,
    val requestActionMessage: String? = null,
    val requestActionError: String? = null,
    val statusMessage: String? = null,
    val error: String? = null
)

class TransactionsViewModel(
    private val transactionsRepository: TransactionsRepository,
    private val dashboardRepository: DashboardRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    fun load(
        accountId: String? = null,
        month: String? = null
    ) {
        val normalizedAccountId = accountId?.trim()?.ifBlank { null }
        val normalizedMonth = month?.trim()?.ifBlank { null }
        if (normalizedMonth != null && !MONTH_KEY_REGEX.matches(normalizedMonth)) {
            _uiState.update { it.copy(error = "Month key must use YYYY-MM format") }
            return
        }

        if (normalizedAccountId == null) {
            _uiState.update { it.copy(error = "Account ID is required to load transactions") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    statusMessage = null,
                    activeAccountId = normalizedAccountId,
                    activeMonth = normalizedMonth,
                    requestActionError = null
                )
            }

            val syncMessage = when (val syncResult = transactionsRepository.syncPendingTransactions()) {
                is AppResult.Success -> {
                    if (syncResult.value > 0) {
                        "Synced ${syncResult.value} queued transaction(s)"
                    } else {
                        null
                    }
                }

                is AppResult.Failure -> null
            }

            when (
                val result = transactionsRepository.getTransactions(
                    accountId = normalizedAccountId,
                    month = normalizedMonth
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = result.value,
                        statusMessage = syncMessage,
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = syncMessage,
                        error = result.error.message
                    )
                }
            }

            when (
                val dashboardResult = dashboardRepository?.getDashboard(
                    accountId = normalizedAccountId,
                    month = normalizedMonth
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(requestItems = dashboardResult.value.transactionRequests)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(requestItems = emptyList())
                }

                null -> Unit
            }
        }
    }

    fun approveTransactionRequest(id: String) {
        handleTransactionRequestAction(id = id, approve = true)
    }

    fun rejectTransactionRequest(id: String) {
        handleTransactionRequestAction(id = id, approve = false)
    }

    fun loadById(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = transactionsRepository.getTransactionById(id = id)) {
                is AppResult.Success -> _uiState.update { state ->
                    val updatedItems = replaceOrPrepend(state.items, result.value)
                    state.copy(isLoading = false, items = updatedItems, error = null)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun createTransaction(request: CreateTransactionRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null, error = null) }
            when (val result = transactionsRepository.createTransaction(request = request)) {
                is AppResult.Success -> _uiState.update { state ->
                    val updatedItems = replaceOrPrepend(state.items, result.value)
                    state.copy(
                        isLoading = false,
                        items = updatedItems,
                        statusMessage = "Transaction created",
                        error = null
                    )
                }

                is AppResult.Failure -> handleCreateFailure(
                    request = request,
                    errorMessage = result.error.message,
                    networkFailure = isNetworkFailure(result)
                )
            }
        }
    }

    fun updateTransaction(
        id: String,
        request: UpdateTransactionRequest
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null, error = null) }
            when (val result = transactionsRepository.updateTransaction(id = id, request = request)) {
                is AppResult.Success -> _uiState.update { state ->
                    val updatedItems = replaceOrPrepend(state.items, result.value)
                    state.copy(
                        isLoading = false,
                        items = updatedItems,
                        statusMessage = "Transaction updated",
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null, error = null) }
            when (val result = transactionsRepository.deleteTransaction(id = id)) {
                is AppResult.Success -> _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        items = state.items.filterNot { it.id == id },
                        statusMessage = "Transaction deleted",
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    private suspend fun handleCreateFailure(
        request: CreateTransactionRequest,
        errorMessage: String,
        networkFailure: Boolean
    ) {
        if (!networkFailure) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = null,
                    error = errorMessage
                )
            }
            return
        }

        when (val queueResult = transactionsRepository.enqueueTransaction(request)) {
            is AppResult.Success -> _uiState.update { state ->
                val queuedItem = TransactionDto(
                    id = "pending-${queueResult.value}",
                    amount = request.amount,
                    type = request.type,
                    description = request.description?.ifBlank { "Queued offline" } ?: "Queued offline",
                    categoryId = request.categoryId,
                    accountId = request.accountId,
                    date = request.date
                )
                state.copy(
                    isLoading = false,
                    items = replaceOrPrepend(state.items, queuedItem),
                    statusMessage = "Offline mode: transaction queued for sync",
                    error = null
                )
            }

            is AppResult.Failure -> _uiState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = null,
                    error = queueResult.error.message
                )
            }
        }
    }

    private fun isNetworkFailure(result: AppResult.Failure): Boolean {
        return result.error.cause is IOException || result.error.message == "Network request failed"
    }

    private fun replaceOrPrepend(
        items: List<TransactionDto>,
        item: TransactionDto
    ): List<TransactionDto> {
        val existingIndex = items.indexOfFirst { it.id == item.id }
        if (existingIndex == -1) {
            return listOf(item) + items
        }

        val mutableItems = items.toMutableList()
        mutableItems[existingIndex] = item
        return mutableItems
    }

    private fun handleTransactionRequestAction(
        id: String,
        approve: Boolean
    ) {
        val requestId = id.trim()
        if (requestId.isBlank()) {
            _uiState.update { it.copy(requestActionError = "Request ID is required") }
            return
        }

        val state = _uiState.value
        val accountId = state.activeAccountId?.takeIf { it.isNotBlank() }
        if (accountId == null) {
            _uiState.update {
                it.copy(requestActionError = "Load transactions for an account before handling requests")
            }
            return
        }
        if (state.requestActionInProgressId != null) {
            return
        }

        val month = state.activeMonth
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    requestActionInProgressId = requestId,
                    requestActionMessage = null,
                    requestActionError = null
                )
            }

            val result = if (approve) {
                transactionsRepository.approveTransactionRequest(requestId)
            } else {
                transactionsRepository.rejectTransactionRequest(requestId)
            }

            when (result) {
                is AppResult.Success -> {
                    val statusLabel = if (result.value.status.equals("APPROVED", ignoreCase = true)) {
                        "approved"
                    } else {
                        "rejected"
                    }
                    _uiState.update {
                        it.copy(
                            requestActionInProgressId = null,
                            requestActionMessage = "Request ${result.value.id} $statusLabel",
                            requestActionError = null
                        )
                    }
                    load(accountId = accountId, month = month)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(
                        requestActionInProgressId = null,
                        requestActionError = result.error.message
                    )
                }
            }
        }
    }

    private companion object {
        val MONTH_KEY_REGEX = Regex("^\\d{4}-\\d{2}$")
    }
}

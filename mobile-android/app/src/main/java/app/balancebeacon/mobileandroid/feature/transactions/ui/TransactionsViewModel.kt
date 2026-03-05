package app.balancebeacon.mobileandroid.feature.transactions.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.transactions.data.TransactionsRepository
import app.balancebeacon.mobileandroid.feature.transactions.model.CreateTransactionRequest
import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionDto
import app.balancebeacon.mobileandroid.feature.transactions.model.UpdateTransactionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val isLoading: Boolean = false,
    val items: List<TransactionDto> = emptyList(),
    val error: String? = null
)

class TransactionsViewModel(
    private val transactionsRepository: TransactionsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    fun load(month: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = transactionsRepository.getTransactions(month = month)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoading = false, items = result.value, error = null)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = transactionsRepository.createTransaction(request = request)) {
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

    fun updateTransaction(
        id: String,
        request: UpdateTransactionRequest
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = transactionsRepository.updateTransaction(id = id, request = request)) {
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

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = transactionsRepository.deleteTransaction(id = id)) {
                is AppResult.Success -> _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        items = state.items.filterNot { it.id == id },
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
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
}

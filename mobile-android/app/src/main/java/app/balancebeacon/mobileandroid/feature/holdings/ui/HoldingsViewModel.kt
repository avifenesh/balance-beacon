package app.balancebeacon.mobileandroid.feature.holdings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.holdings.data.HoldingsRepository
import app.balancebeacon.mobileandroid.feature.holdings.model.CreateHoldingRequest
import app.balancebeacon.mobileandroid.feature.holdings.model.HoldingDto
import app.balancebeacon.mobileandroid.feature.holdings.model.UpdateHoldingRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class HoldingsUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val accountId: String = "",
    val selectedHoldingId: String? = null,
    val holdings: List<HoldingDto> = emptyList(),
    val formCategoryId: String = "",
    val formSymbol: String = "",
    val formQuantity: String = "",
    val formAverageCost: String = "",
    val formCurrency: String = "USD",
    val formNotes: String = "",
    val statusMessage: String? = null,
    val error: String? = null
)

class HoldingsViewModel(
    private val holdingsRepository: HoldingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HoldingsUiState())
    val uiState: StateFlow<HoldingsUiState> = _uiState.asStateFlow()

    fun onAccountIdChanged(value: String) {
        _uiState.update { it.copy(accountId = value, statusMessage = null, error = null) }
    }

    fun onFormCategoryIdChanged(value: String) {
        _uiState.update { it.copy(formCategoryId = value, statusMessage = null, error = null) }
    }

    fun onFormSymbolChanged(value: String) {
        _uiState.update { it.copy(formSymbol = value, statusMessage = null, error = null) }
    }

    fun onFormQuantityChanged(value: String) {
        _uiState.update { it.copy(formQuantity = value, statusMessage = null, error = null) }
    }

    fun onFormAverageCostChanged(value: String) {
        _uiState.update { it.copy(formAverageCost = value, statusMessage = null, error = null) }
    }

    fun onFormCurrencyChanged(value: String) {
        _uiState.update { it.copy(formCurrency = value, statusMessage = null, error = null) }
    }

    fun onFormNotesChanged(value: String) {
        _uiState.update { it.copy(formNotes = value, statusMessage = null, error = null) }
    }

    fun load() {
        val accountId = _uiState.value.accountId.trim()
        if (accountId.isBlank()) {
            _uiState.update { it.copy(error = "Account ID is required to load holdings") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, statusMessage = null) }
            when (val result = holdingsRepository.getHoldings(accountId = accountId)) {
                is AppResult.Success -> _uiState.update { state ->
                    val selectedId = state.selectedHoldingId?.takeIf { id ->
                        result.value.any { holding -> holding.id == id }
                    }
                    state.copy(
                        isLoading = false,
                        holdings = result.value,
                        selectedHoldingId = selectedId,
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun createHolding() {
        val state = _uiState.value
        val accountId = state.accountId.trim()
        val categoryId = state.formCategoryId.trim()
        val symbol = state.formSymbol.trim().uppercase(Locale.ROOT)
        val quantity = state.formQuantity.toDoubleOrNull()
        val averageCost = state.formAverageCost.toDoubleOrNull()
        val currency = normalizeCurrency(state.formCurrency)

        if (accountId.isBlank()) {
            _uiState.update { it.copy(error = "Account ID is required") }
            return
        }
        if (categoryId.isBlank()) {
            _uiState.update { it.copy(error = "Category ID is required") }
            return
        }
        if (symbol.isBlank()) {
            _uiState.update { it.copy(error = "Symbol is required") }
            return
        }
        if (quantity == null || quantity <= 0) {
            _uiState.update { it.copy(error = "Quantity must be greater than 0") }
            return
        }
        if (averageCost == null || averageCost < 0) {
            _uiState.update { it.copy(error = "Average cost must be 0 or greater") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }

            val request = CreateHoldingRequest(
                accountId = accountId,
                categoryId = categoryId,
                symbol = symbol,
                quantity = quantity,
                averageCost = averageCost,
                currencyCode = currency,
                notes = state.formNotes.trim().ifBlank { null }
            )

            when (val result = holdingsRepository.createHolding(request = request)) {
                is AppResult.Success -> _uiState.update { current ->
                    val updatedHoldings = replaceOrPrepend(current.holdings, result.value)
                    current.copy(
                        isMutating = false,
                        holdings = updatedHoldings,
                        selectedHoldingId = result.value.id,
                        formCategoryId = result.value.categoryId,
                        formSymbol = result.value.symbol,
                        formQuantity = result.value.quantity,
                        formAverageCost = result.value.averageCost,
                        formCurrency = normalizeCurrency(result.value.currencyCode),
                        formNotes = result.value.notes.orEmpty(),
                        statusMessage = "Holding created",
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun selectHolding(holding: HoldingDto) {
        _uiState.update {
            it.copy(
                selectedHoldingId = holding.id,
                formCategoryId = holding.categoryId,
                formSymbol = holding.symbol,
                formQuantity = holding.quantity,
                formAverageCost = holding.averageCost,
                formCurrency = normalizeCurrency(holding.currencyCode),
                formNotes = holding.notes.orEmpty(),
                statusMessage = null,
                error = null
            )
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedHoldingId = null,
                formCategoryId = "",
                formSymbol = "",
                formQuantity = "",
                formAverageCost = "",
                formCurrency = "USD",
                formNotes = "",
                statusMessage = null,
                error = null
            )
        }
    }

    fun updateSelectedHolding() {
        val state = _uiState.value
        val selectedId = state.selectedHoldingId
        val quantity = state.formQuantity.toDoubleOrNull()
        val averageCost = state.formAverageCost.toDoubleOrNull()

        if (selectedId.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Select a holding to update") }
            return
        }
        if (quantity == null || quantity <= 0) {
            _uiState.update { it.copy(error = "Quantity must be greater than 0") }
            return
        }
        if (averageCost == null || averageCost < 0) {
            _uiState.update { it.copy(error = "Average cost must be 0 or greater") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }

            when (
                val result = holdingsRepository.updateHolding(
                    id = selectedId,
                    request = UpdateHoldingRequest(
                        quantity = quantity,
                        averageCost = averageCost,
                        notes = state.formNotes.trim().ifBlank { null }
                    )
                )
            ) {
                is AppResult.Success -> _uiState.update { current ->
                    current.copy(
                        isMutating = false,
                        holdings = replaceOrPrepend(current.holdings, result.value),
                        selectedHoldingId = result.value.id,
                        formCategoryId = result.value.categoryId,
                        formSymbol = result.value.symbol,
                        formQuantity = result.value.quantity,
                        formAverageCost = result.value.averageCost,
                        formCurrency = normalizeCurrency(result.value.currencyCode),
                        formNotes = result.value.notes.orEmpty(),
                        statusMessage = "Holding updated",
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun deleteHolding(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (val result = holdingsRepository.deleteHolding(id = id)) {
                is AppResult.Success -> _uiState.update { state ->
                    val selectedWasDeleted = state.selectedHoldingId == id
                    state.copy(
                        isMutating = false,
                        holdings = state.holdings.filterNot { holding -> holding.id == id },
                        selectedHoldingId = if (selectedWasDeleted) null else state.selectedHoldingId,
                        formCategoryId = if (selectedWasDeleted) "" else state.formCategoryId,
                        formSymbol = if (selectedWasDeleted) "" else state.formSymbol,
                        formQuantity = if (selectedWasDeleted) "" else state.formQuantity,
                        formAverageCost = if (selectedWasDeleted) "" else state.formAverageCost,
                        formCurrency = if (selectedWasDeleted) "USD" else state.formCurrency,
                        formNotes = if (selectedWasDeleted) "" else state.formNotes,
                        statusMessage = "Holding deleted",
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun refreshPrices() {
        val accountId = _uiState.value.accountId.trim()
        if (accountId.isBlank()) {
            _uiState.update { it.copy(error = "Account ID is required to refresh prices") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (val refreshResult = holdingsRepository.refreshHoldingPrices(accountId = accountId)) {
                is AppResult.Success -> {
                    val status = buildString {
                        append("Prices refreshed: ${refreshResult.value.updated} updated")
                        if (refreshResult.value.skipped > 0) {
                            append(", ${refreshResult.value.skipped} skipped")
                        }
                        if (refreshResult.value.errors.isNotEmpty()) {
                            append(", ${refreshResult.value.errors.size} errors")
                        }
                    }
                    reloadHoldingsAfterMutation(accountId = accountId, statusMessage = status)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = refreshResult.error.message)
                }
            }
        }
    }

    private suspend fun reloadHoldingsAfterMutation(accountId: String, statusMessage: String) {
        when (val loadResult = holdingsRepository.getHoldings(accountId = accountId)) {
            is AppResult.Success -> _uiState.update { state ->
                val selectedId = state.selectedHoldingId?.takeIf { id ->
                    loadResult.value.any { holding -> holding.id == id }
                }
                state.copy(
                    isMutating = false,
                    holdings = loadResult.value,
                    selectedHoldingId = selectedId,
                    statusMessage = statusMessage,
                    error = null
                )
            }

            is AppResult.Failure -> _uiState.update {
                it.copy(isMutating = false, error = loadResult.error.message)
            }
        }
    }

    private fun replaceOrPrepend(items: List<HoldingDto>, item: HoldingDto): List<HoldingDto> {
        val existingIndex = items.indexOfFirst { it.id == item.id }
        if (existingIndex == -1) {
            return listOf(item) + items
        }

        val mutableItems = items.toMutableList()
        mutableItems[existingIndex] = item
        return mutableItems
    }

    private fun normalizeCurrency(value: String?): String {
        val normalized = value.orEmpty().trim().uppercase(Locale.ROOT)
        return if (normalized.isBlank()) "USD" else normalized
    }
}

package app.balancebeacon.mobileandroid.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val currency: String = "USD",
    val message: String? = null,
    val error: String? = null
)

class SettingsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.me()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        currency = result.value.user.preferredCurrency ?: "USD",
                        message = null,
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun onCurrencyChanged(value: String) {
        _uiState.update { it.copy(currency = value, message = null, error = null) }
    }

    fun saveCurrency() {
        val state = _uiState.value
        if (state.currency.isBlank()) {
            _uiState.update { it.copy(error = "Currency is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null, error = null) }
            when (val result = authRepository.updateCurrency(state.currency)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        currency = result.value.currency,
                        message = "Currency updated to ${result.value.currency}",
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, error = result.error.message)
                }
            }
        }
    }
}

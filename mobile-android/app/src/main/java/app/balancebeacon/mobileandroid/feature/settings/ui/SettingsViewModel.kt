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
import java.util.Locale

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isSavingCurrency: Boolean = false,
    val isExportingData: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val currency: String = "USD",
    val accountEmail: String = "",
    val deleteConfirmEmail: String = "",
    val selectedExportFormat: String = "JSON",
    val exportFormat: String? = null,
    val exportGeneratedAt: String? = null,
    val exportData: String? = null,
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
                        accountEmail = result.value.user.email,
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

    fun onDeleteConfirmEmailChanged(value: String) {
        _uiState.update { it.copy(deleteConfirmEmail = value, message = null, error = null) }
    }

    fun onExportFormatChanged(value: String) {
        _uiState.update {
            it.copy(
                selectedExportFormat = value.trim().uppercase(Locale.ROOT).ifBlank { "JSON" },
                message = null,
                error = null
            )
        }
    }

    fun saveCurrency() {
        val state = _uiState.value
        if (state.currency.isBlank()) {
            _uiState.update { it.copy(error = "Currency is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingCurrency = true, message = null, error = null) }
            when (val result = authRepository.updateCurrency(state.currency)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isSavingCurrency = false,
                        currency = result.value.currency,
                        message = "Currency updated to ${result.value.currency}",
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isSavingCurrency = false, error = result.error.message)
                }
            }
        }
    }

    fun exportMyData() {
        val format = _uiState.value.selectedExportFormat.trim().lowercase(Locale.ROOT).ifBlank { "json" }
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingData = true, message = null, error = null) }
            when (val result = authRepository.exportUserData(format = format)) {
                is AppResult.Success -> {
                    val responseFormat = result.value.format
                        ?.trim()
                        ?.ifBlank { format }
                        ?.uppercase(Locale.ROOT)
                        ?: format.uppercase(Locale.ROOT)
                    val exportedAt = result.value.exportedAt?.trim().takeUnless { it.isNullOrBlank() }
                    val message = if (exportedAt != null) {
                        "Export ready ($responseFormat) at $exportedAt"
                    } else {
                        "Export ready ($responseFormat)"
                    }
                    _uiState.update {
                        it.copy(
                            isExportingData = false,
                            exportFormat = responseFormat,
                            exportGeneratedAt = exportedAt,
                            exportData = result.value.data,
                            message = message,
                            error = null
                        )
                    }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isExportingData = false, error = result.error.message)
                }
            }
        }
    }

    fun deleteAccount() {
        val state = _uiState.value
        val email = state.deleteConfirmEmail.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Type your email to confirm account deletion") }
            return
        }
        if (
            state.accountEmail.isNotBlank() &&
            !email.equals(state.accountEmail, ignoreCase = true)
        ) {
            _uiState.update { it.copy(error = "Confirmation email must match ${state.accountEmail}") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true, message = null, error = null) }
            when (val result = authRepository.deleteAccount(confirmEmail = email)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        deleteConfirmEmail = "",
                        message = result.value.message.ifBlank { "Account deleted successfully" },
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isDeletingAccount = false, error = result.error.message)
                }
            }
        }
    }
}

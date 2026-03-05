package app.balancebeacon.mobileandroid.feature.profile.ui

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

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isExporting: Boolean = false,
    val isDeleting: Boolean = false,
    val email: String = "",
    val displayName: String = "",
    val preferredCurrency: String = "USD",
    val exportFormat: String = "json",
    val confirmEmail: String = "",
    val exportStatus: String? = null,
    val accountStatus: String? = null,
    val error: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.me()) {
                is AppResult.Success -> {
                    val user = result.value.user
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            email = user.email,
                            displayName = user.displayName.orEmpty(),
                            preferredCurrency = user.preferredCurrency ?: "USD",
                            confirmEmail = user.email,
                            error = null
                        )
                    }
                }

                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun onDisplayNameChanged(value: String) {
        _uiState.update { it.copy(displayName = value, error = null) }
    }

    fun onExportFormatChanged(value: String) {
        _uiState.update {
            it.copy(
                exportFormat = value.trim().lowercase(Locale.ROOT),
                error = null
            )
        }
    }

    fun onConfirmEmailChanged(value: String) {
        _uiState.update { it.copy(confirmEmail = value, error = null) }
    }

    fun saveProfile() {
        val state = _uiState.value
        if (state.displayName.isBlank()) {
            _uiState.update { it.copy(error = "Display name is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, accountStatus = null) }
            when (val result = authRepository.updateProfile(state.displayName)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        displayName = result.value.displayName.orEmpty(),
                        preferredCurrency = result.value.preferredCurrency ?: it.preferredCurrency,
                        accountStatus = "Profile updated",
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, error = result.error.message)
                }
            }
        }
    }

    fun exportData() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null, exportStatus = null) }
            when (val result = authRepository.exportUserData(state.exportFormat)) {
                is AppResult.Success -> {
                    val response = result.value
                    val status = when {
                        !response.data.isNullOrBlank() -> "Export ready (${response.format ?: state.exportFormat})"
                        !response.exportedAt.isNullOrBlank() -> "Export generated at ${response.exportedAt}"
                        else -> "Export completed"
                    }
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportStatus = status,
                            error = null
                        )
                    }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isExporting = false, error = result.error.message)
                }
            }
        }
    }

    fun deleteAccount() {
        val state = _uiState.value
        if (state.confirmEmail.isBlank()) {
            _uiState.update { it.copy(error = "Confirm email is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null, accountStatus = null) }
            when (val result = authRepository.deleteAccount(state.confirmEmail)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isDeleting = false,
                        accountStatus = result.value.message,
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isDeleting = false, error = result.error.message)
                }
            }
        }
    }
}

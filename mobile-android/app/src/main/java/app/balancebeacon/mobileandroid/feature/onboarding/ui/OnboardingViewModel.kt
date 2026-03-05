package app.balancebeacon.mobileandroid.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.onboarding.data.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currency: String = "USD",
    val locale: String = "en-US",
    val isSubmitting: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
)

class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onCurrencyChanged(value: String) {
        _uiState.update { it.copy(currency = value, error = null) }
    }

    fun complete() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            when (
                val result = onboardingRepository.completeOnboarding(
                    currency = state.currency,
                    locale = state.locale
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, isCompleted = true, error = null)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.error.message)
                }
            }
        }
    }
}

package app.balancebeacon.mobileandroid.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.auth.data.AuthRepository
import app.balancebeacon.mobileandroid.feature.categories.data.CategoriesRepository
import app.balancebeacon.mobileandroid.feature.categories.model.CreateCategoryRequest
import app.balancebeacon.mobileandroid.feature.onboarding.data.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, CURRENCY, CATEGORIES, COMPLETE }

data class PresetCategory(
    val name: String,
    val type: String,
    val color: String,
    val selected: Boolean = true
)

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val currency: String = "USD",
    val locale: String = "en-US",
    val presetCategories: List<PresetCategory> = defaultPresetCategories(),
    val categoriesCreatedCount: Int = 0,
    val isSubmitting: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
)

private fun defaultPresetCategories(): List<PresetCategory> = listOf(
    PresetCategory("Groceries", "EXPENSE", "#22c55e"),
    PresetCategory("Dining Out", "EXPENSE", "#f97316"),
    PresetCategory("Transportation", "EXPENSE", "#3b82f6"),
    PresetCategory("Utilities", "EXPENSE", "#8b5cf6"),
    PresetCategory("Entertainment", "EXPENSE", "#ec4899"),
    PresetCategory("Shopping", "EXPENSE", "#06b6d4"),
    PresetCategory("Health", "EXPENSE", "#ef4444"),
    PresetCategory("Housing", "EXPENSE", "#84cc16"),
    PresetCategory("Insurance", "EXPENSE", "#6366f1"),
    PresetCategory("Subscriptions", "EXPENSE", "#14b8a6"),
    PresetCategory("Salary", "INCOME", "#10b981"),
    PresetCategory("Freelance", "INCOME", "#06b6d4"),
    PresetCategory("Investments", "INCOME", "#8b5cf6"),
    PresetCategory("Other Income", "INCOME", "#6b7280")
)

class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val authRepository: AuthRepository? = null,
    private val categoriesRepository: CategoriesRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        val current = _uiState.value.currentStep
        val next = when (current) {
            OnboardingStep.WELCOME -> OnboardingStep.CURRENCY
            OnboardingStep.CURRENCY -> OnboardingStep.CATEGORIES
            OnboardingStep.CATEGORIES -> OnboardingStep.COMPLETE
            OnboardingStep.COMPLETE -> return
        }
        _uiState.update { it.copy(currentStep = next, error = null) }
    }

    fun previousStep() {
        val current = _uiState.value.currentStep
        val prev = when (current) {
            OnboardingStep.WELCOME -> return
            OnboardingStep.CURRENCY -> OnboardingStep.WELCOME
            OnboardingStep.CATEGORIES -> OnboardingStep.CURRENCY
            OnboardingStep.COMPLETE -> OnboardingStep.CATEGORIES
        }
        _uiState.update { it.copy(currentStep = prev, error = null) }
    }

    fun selectCurrency(currency: String) {
        _uiState.update { it.copy(currency = currency, error = null) }
    }

    fun confirmCurrency() {
        val auth = authRepository ?: run {
            nextStep()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            when (auth.updateCurrency(_uiState.value.currency)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    nextStep()
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false, error = "Failed to update currency") }
                }
            }
        }
    }

    fun toggleCategory(name: String) {
        _uiState.update { state ->
            state.copy(
                presetCategories = state.presetCategories.map { cat ->
                    if (cat.name == name) cat.copy(selected = !cat.selected) else cat
                },
                error = null
            )
        }
    }

    fun confirmCategories() {
        val repo = categoriesRepository ?: run {
            nextStep()
            return
        }
        val selected = _uiState.value.presetCategories.filter { it.selected }
        if (selected.isEmpty()) {
            nextStep()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val requests = selected.map { cat ->
                CreateCategoryRequest(name = cat.name, type = cat.type, color = cat.color)
            }
            when (val result = repo.bulkCreateCategories(requests)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            categoriesCreatedCount = result.value.categoriesCreated
                        )
                    }
                    nextStep()
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false, error = "Failed to create categories") }
                }
            }
        }
    }

    fun complete() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            when (
                onboardingRepository.completeOnboarding(
                    currency = state.currency,
                    locale = state.locale
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, isCompleted = true, error = null)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, error = "Failed to complete onboarding")
                }
            }
        }
    }
}

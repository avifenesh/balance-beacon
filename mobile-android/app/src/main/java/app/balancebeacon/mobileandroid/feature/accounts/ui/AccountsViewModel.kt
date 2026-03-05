package app.balancebeacon.mobileandroid.feature.accounts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsRepository
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class AccountsUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val items: List<AccountDto> = emptyList(),
    val createName: String = "",
    val createType: String = "SELF",
    val createColor: String = "",
    val createPreferredCurrency: String = "",
    val editingAccountId: String? = null,
    val editName: String = "",
    val editType: String = "",
    val editColor: String = "",
    val editPreferredCurrency: String = "",
    val statusMessage: String? = null,
    val error: String? = null
)

class AccountsViewModel(
    private val accountsRepository: AccountsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            fetchAccounts(showLoading = true)
        }
    }

    fun onCreateNameChanged(value: String) {
        _uiState.update { it.copy(createName = value, error = null, statusMessage = null) }
    }

    fun onCreateTypeChanged(value: String) {
        _uiState.update { it.copy(createType = value, error = null, statusMessage = null) }
    }

    fun onCreateColorChanged(value: String) {
        _uiState.update { it.copy(createColor = value, error = null, statusMessage = null) }
    }

    fun onCreatePreferredCurrencyChanged(value: String) {
        _uiState.update {
            it.copy(createPreferredCurrency = value, error = null, statusMessage = null)
        }
    }

    fun createAccount() {
        val state = _uiState.value
        if (state.createName.isBlank()) {
            _uiState.update { it.copy(error = "Account name is required") }
            return
        }

        val accountType = normalizeAccountType(state.createType)
        if (accountType == null) {
            _uiState.update {
                it.copy(error = "Account type must be SELF, PARTNER, or OTHER")
            }
            return
        }

        val currency = normalizeCurrency(state.createPreferredCurrency)
        if (state.createPreferredCurrency.isNotBlank() && currency == null) {
            _uiState.update { it.copy(error = "Currency must be USD, EUR, or ILS") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (
                val result = accountsRepository.createAccount(
                    name = state.createName,
                    type = accountType,
                    color = state.createColor.trim().ifBlank { null },
                    preferredCurrency = currency
                )
            ) {
                is AppResult.Success -> refreshAccountsAfterMutation(
                    statusMessage = "Account created",
                    resetCreateForm = true
                )

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun startEditing(account: AccountDto) {
        _uiState.update {
            it.copy(
                editingAccountId = account.id,
                editName = account.name,
                editType = account.type,
                editColor = account.color.orEmpty(),
                editPreferredCurrency = account.preferredCurrency.orEmpty(),
                error = null,
                statusMessage = null
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                editingAccountId = null,
                editName = "",
                editType = "",
                editColor = "",
                editPreferredCurrency = "",
                error = null,
                statusMessage = null
            )
        }
    }

    fun onEditNameChanged(value: String) {
        _uiState.update { it.copy(editName = value, error = null, statusMessage = null) }
    }

    fun onEditTypeChanged(value: String) {
        _uiState.update { it.copy(editType = value, error = null, statusMessage = null) }
    }

    fun onEditColorChanged(value: String) {
        _uiState.update { it.copy(editColor = value, error = null, statusMessage = null) }
    }

    fun onEditPreferredCurrencyChanged(value: String) {
        _uiState.update {
            it.copy(editPreferredCurrency = value, error = null, statusMessage = null)
        }
    }

    fun updateAccount() {
        val state = _uiState.value
        val accountId = state.editingAccountId
        if (accountId.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Select an account to edit") }
            return
        }

        if (state.editName.isBlank()) {
            _uiState.update { it.copy(error = "Account name is required") }
            return
        }

        val accountType = if (state.editType.isBlank()) {
            null
        } else {
            normalizeAccountType(state.editType)
        }
        if (state.editType.isNotBlank() && accountType == null) {
            _uiState.update {
                it.copy(error = "Account type must be SELF, PARTNER, or OTHER")
            }
            return
        }

        val currency = normalizeCurrency(state.editPreferredCurrency)
        if (state.editPreferredCurrency.isNotBlank() && currency == null) {
            _uiState.update { it.copy(error = "Currency must be USD, EUR, or ILS") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (
                val result = accountsRepository.updateAccount(
                    id = accountId,
                    name = state.editName,
                    type = accountType,
                    color = state.editColor.trim().ifBlank { null },
                    preferredCurrency = currency
                )
            ) {
                is AppResult.Success -> refreshAccountsAfterMutation(
                    statusMessage = "Account updated",
                    resetEditForm = true
                )

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (val result = accountsRepository.deleteAccount(id)) {
                is AppResult.Success -> {
                    val shouldResetEditForm = _uiState.value.editingAccountId == id
                    val status = if (result.value.deleted) {
                        "Account deleted"
                    } else {
                        "Account delete request completed"
                    }
                    refreshAccountsAfterMutation(
                        statusMessage = status,
                        resetEditForm = shouldResetEditForm
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    fun activateAccount(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, error = null, statusMessage = null) }
            when (val result = accountsRepository.activateAccount(id)) {
                is AppResult.Success -> refreshAccountsAfterMutation(
                    statusMessage = "Active account set to ${result.value.activeAccountId}"
                )

                is AppResult.Failure -> _uiState.update {
                    it.copy(isMutating = false, error = result.error.message)
                }
            }
        }
    }

    private suspend fun fetchAccounts(showLoading: Boolean) {
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }

        when (val result = accountsRepository.getAccounts()) {
            is AppResult.Success -> _uiState.update {
                it.copy(
                    isLoading = false,
                    items = result.value,
                    error = null
                )
            }

            is AppResult.Failure -> _uiState.update {
                it.copy(
                    isLoading = false,
                    error = result.error.message
                )
            }
        }
    }

    private suspend fun refreshAccountsAfterMutation(
        statusMessage: String,
        resetCreateForm: Boolean = false,
        resetEditForm: Boolean = false
    ) {
        when (val reloadResult = accountsRepository.getAccounts()) {
            is AppResult.Success -> _uiState.update { state ->
                var next = state.copy(
                    isLoading = false,
                    isMutating = false,
                    items = reloadResult.value,
                    statusMessage = statusMessage,
                    error = null
                )

                if (resetCreateForm) {
                    next = next.copy(
                        createName = "",
                        createType = DEFAULT_ACCOUNT_TYPE,
                        createColor = "",
                        createPreferredCurrency = ""
                    )
                }

                if (resetEditForm) {
                    next = next.copy(
                        editingAccountId = null,
                        editName = "",
                        editType = "",
                        editColor = "",
                        editPreferredCurrency = ""
                    )
                }

                next
            }

            is AppResult.Failure -> _uiState.update {
                it.copy(
                    isMutating = false,
                    error = reloadResult.error.message
                )
            }
        }
    }

    private fun normalizeAccountType(raw: String): String? {
        val normalized = raw.trim().uppercase(Locale.ROOT)
        return normalized.takeIf { it in VALID_ACCOUNT_TYPES }
    }

    private fun normalizeCurrency(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val normalized = trimmed.uppercase(Locale.ROOT)
        return normalized.takeIf { it in VALID_CURRENCIES }
    }

    private companion object {
        const val DEFAULT_ACCOUNT_TYPE = "SELF"

        val VALID_ACCOUNT_TYPES = setOf(
            "SELF",
            "PARTNER",
            "OTHER"
        )

        val VALID_CURRENCIES = setOf(
            "USD",
            "EUR",
            "ILS"
        )
    }
}

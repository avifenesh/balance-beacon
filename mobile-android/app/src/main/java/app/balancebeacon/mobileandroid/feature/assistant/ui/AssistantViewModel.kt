package app.balancebeacon.mobileandroid.feature.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.assistant.data.AssistantRepository
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatRequest
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantMessageDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantChatUiMessage(
    val role: String,
    val text: String
)

data class AssistantUiState(
    val isLoading: Boolean = false,
    val accountId: String = "",
    val monthKey: String = "",
    val messageInput: String = "",
    val conversation: List<AssistantChatUiMessage> = emptyList(),
    val error: String? = null
)

class AssistantViewModel(
    private val assistantRepository: AssistantRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun onAccountIdChanged(value: String) {
        _uiState.update { it.copy(accountId = value, error = null) }
    }

    fun onMonthKeyChanged(value: String) {
        _uiState.update { it.copy(monthKey = value, error = null) }
    }

    fun onMessageInputChanged(value: String) {
        _uiState.update { it.copy(messageInput = value, error = null) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val accountId = state.accountId.trim()
        val monthKey = state.monthKey.trim()
        val message = state.messageInput.trim()

        if (accountId.isBlank()) {
            _uiState.update { it.copy(error = "Account ID is required") }
            return
        }
        if (!MONTH_KEY_REGEX.matches(monthKey)) {
            _uiState.update { it.copy(error = "Month key must use YYYY-MM format") }
            return
        }
        if (message.isBlank()) {
            _uiState.update { it.copy(error = "Enter a message") }
            return
        }

        val nextConversation = state.conversation + AssistantChatUiMessage(
            role = "user",
            text = message
        )

        _uiState.update {
            it.copy(
                isLoading = true,
                messageInput = "",
                conversation = nextConversation,
                error = null
            )
        }

        viewModelScope.launch {
            val request = AssistantChatRequest(
                accountId = accountId,
                monthKey = monthKey,
                messages = nextConversation.map { chat ->
                    AssistantMessageDto(role = chat.role, content = chat.text)
                }
            )

            when (val result = assistantRepository.chat(request = request)) {
                is AppResult.Success -> _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        conversation = current.conversation + AssistantChatUiMessage(
                            role = "assistant",
                            text = result.value
                        ),
                        error = null
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    private companion object {
        val MONTH_KEY_REGEX = Regex("^\\d{4}-\\d{2}$")
    }
}

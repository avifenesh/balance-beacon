package app.balancebeacon.mobileandroid.feature.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.balancebeacon.mobileandroid.core.result.AppResult
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsRepository
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.feature.assistant.data.AssistantRepository
import app.balancebeacon.mobileandroid.feature.assistant.data.AssistantSessionStore
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatRequest
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatSession
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantMessageDto
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantSessionMessage
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantSessionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val accounts: List<AccountDto> = emptyList(),
    val accountId: String = "",
    val monthKey: String = currentAssistantMonthKey(),
    val messageInput: String = "",
    val sessions: List<AssistantChatSession> = emptyList(),
    val activeSessionId: String = "",
    val error: String? = null
)

class AssistantViewModel(
    private val assistantRepository: AssistantRepository,
    private val accountsRepository: AccountsRepository,
    private val assistantSessionStore: AssistantSessionStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = accountsRepository.getAccounts()) {
                is AppResult.Success -> {
                    val accounts = result.value
                    val accountId = accounts.firstOrNull()?.id.orEmpty()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            accounts = accounts,
                            accountId = accountId,
                            error = if (accountId.isBlank()) "Create an account first to use the assistant" else null
                        )
                    }
                    if (accountId.isNotBlank()) {
                        loadSessionScope(accountId = accountId, monthKey = _uiState.value.monthKey)
                    }
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun selectAccount(value: String) {
        val monthKey = _uiState.value.monthKey.trim()
        _uiState.update { it.copy(accountId = value, error = null) }
        if (value.isNotBlank() && MONTH_KEY_REGEX.matches(monthKey)) {
            viewModelScope.launch {
                loadSessionScope(accountId = value, monthKey = monthKey)
            }
        }
    }

    fun onMonthKeyChanged(value: String) {
        val normalized = value.trim()
        _uiState.update { it.copy(monthKey = value, error = null) }
        val accountId = _uiState.value.accountId.trim()
        if (accountId.isNotBlank() && MONTH_KEY_REGEX.matches(normalized)) {
            viewModelScope.launch {
                loadSessionScope(accountId = accountId, monthKey = normalized)
            }
        }
    }

    fun onMessageInputChanged(value: String) {
        _uiState.update { it.copy(messageInput = value, error = null) }
    }

    fun selectSession(id: String) {
        _uiState.update { it.copy(activeSessionId = id, error = null) }
        persistCurrentScope()
    }

    fun createNewSession() {
        val state = _uiState.value
        val nextSessions = state.sessions + createAssistantSession(title = "Conversation ${state.sessions.size + 1}")
        val nextActiveId = nextSessions.last().id
        _uiState.update {
            it.copy(
                sessions = nextSessions,
                activeSessionId = nextActiveId,
                error = null
            )
        }
        persistCurrentScope()
    }

    fun deleteSession(id: String) {
        val state = _uiState.value
        if (state.sessions.size <= 1) {
            return
        }

        val nextSessions = state.sessions.filterNot { it.id == id }
        val nextActiveId = if (state.activeSessionId == id) nextSessions.first().id else state.activeSessionId
        _uiState.update {
            it.copy(
                sessions = nextSessions,
                activeSessionId = nextActiveId,
                error = null
            )
        }
        persistCurrentScope()
    }

    fun sendMessage() {
        val state = _uiState.value
        val accountId = state.accountId.trim()
        val monthKey = state.monthKey.trim()
        val message = state.messageInput.trim()
        val activeSession = currentSession(state)

        if (accountId.isBlank()) {
            _uiState.update { it.copy(error = "Select an account first") }
            return
        }
        if (!MONTH_KEY_REGEX.matches(monthKey)) {
            _uiState.update { it.copy(error = "Month must use YYYY-MM format") }
            return
        }
        if (activeSession == null) {
            _uiState.update { it.copy(error = "Create a conversation first") }
            return
        }
        if (message.isBlank()) {
            _uiState.update { it.copy(error = "Enter a message") }
            return
        }

        val userMessage = createAssistantMessage(role = "user", text = message)
        val sessionsAfterUserMessage = updateSession(
            sessions = state.sessions,
            sessionId = activeSession.id
        ) { session ->
            val updatedMessages = session.messages + userMessage
            val derivedTitle = if (session.isCustomTitle) {
                null
            } else {
                createAssistantTitleFromMessage(updatedMessages.firstOrNull { it.role == "user" })
            }
            session.copy(
                title = derivedTitle ?: session.title,
                messages = updatedMessages,
                updatedAt = currentAssistantTimestamp(),
                isCustomTitle = session.isCustomTitle
            )
        }

        _uiState.update {
            it.copy(
                isSending = true,
                messageInput = "",
                sessions = sessionsAfterUserMessage,
                error = null
            )
        }
        persistCurrentScope()

        viewModelScope.launch {
            val request = AssistantChatRequest(
                accountId = accountId,
                monthKey = monthKey,
                messages = sessionsAfterUserMessage
                    .first { it.id == activeSession.id }
                    .messages
                    .map { AssistantMessageDto(role = it.role, content = it.text) }
            )

            when (val result = assistantRepository.chat(request = request)) {
                is AppResult.Success -> {
                    val assistantMessage = createAssistantMessage(role = "assistant", text = result.value)
                    val sessionsAfterAssistant = updateSession(
                        sessions = _uiState.value.sessions,
                        sessionId = activeSession.id
                    ) { session ->
                        session.copy(
                            messages = session.messages + assistantMessage,
                            updatedAt = currentAssistantTimestamp()
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isSending = false,
                            sessions = sessionsAfterAssistant,
                            error = null
                        )
                    }
                    persistCurrentScope()
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isSending = false, error = result.error.message)
                }
            }
        }
    }

    private suspend fun loadSessionScope(accountId: String, monthKey: String) {
        val snapshot = assistantSessionStore.load(scopeKey(accountId = accountId, monthKey = monthKey))
        val sessions = ensureAssistantSessions(snapshot.sessions)
        val activeSessionId = snapshot.activeSessionId?.takeIf { activeId ->
            sessions.any { it.id == activeId }
        } ?: sessions.first().id

        _uiState.update {
            it.copy(
                accountId = accountId,
                monthKey = monthKey,
                sessions = sessions,
                activeSessionId = activeSessionId,
                error = null
            )
        }

        assistantSessionStore.save(
            scopeKey = scopeKey(accountId = accountId, monthKey = monthKey),
            snapshot = AssistantSessionSnapshot(
                sessions = sessions,
                activeSessionId = activeSessionId
            )
        )
    }

    private fun persistCurrentScope() {
        val state = _uiState.value
        val accountId = state.accountId.trim()
        val monthKey = state.monthKey.trim()
        if (accountId.isBlank() || !MONTH_KEY_REGEX.matches(monthKey)) {
            return
        }

        val snapshot = AssistantSessionSnapshot(
            sessions = state.sessions,
            activeSessionId = state.activeSessionId
        )

        viewModelScope.launch {
            assistantSessionStore.save(scopeKey(accountId = accountId, monthKey = monthKey), snapshot)
        }
    }

    private fun currentSession(state: AssistantUiState): AssistantChatSession? {
        return state.sessions.firstOrNull { it.id == state.activeSessionId }
            ?: state.sessions.firstOrNull()
    }

    private fun updateSession(
        sessions: List<AssistantChatSession>,
        sessionId: String,
        updater: (AssistantChatSession) -> AssistantChatSession
    ): List<AssistantChatSession> {
        return sessions.map { session ->
            if (session.id == sessionId) {
                updater(session)
            } else {
                session
            }
        }
    }

    private fun scopeKey(accountId: String, monthKey: String): String {
        return "$accountId::$monthKey"
    }

    private companion object {
        val MONTH_KEY_REGEX = Regex("^\\d{4}-\\d{2}$")
    }
}

private fun currentAssistantTimestamp(): String {
    return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(java.util.Date())
}

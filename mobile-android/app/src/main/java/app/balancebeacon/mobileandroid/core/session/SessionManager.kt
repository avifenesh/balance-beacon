package app.balancebeacon.mobileandroid.core.session

import app.balancebeacon.mobileandroid.core.storage.AuthTokens
import app.balancebeacon.mobileandroid.core.storage.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionManager(
    private val sessionStore: SessionStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tokenWriteMutex = Mutex()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    @Volatile
    var currentAccessToken: String? = null
        private set

    @Volatile
    var currentRefreshToken: String? = null
        private set

    init {
        scope.launch {
            sessionStore.observeTokens().collect { tokens ->
                currentAccessToken = tokens?.accessToken
                currentRefreshToken = tokens?.refreshToken
                _sessionState.value = if (tokens?.accessToken.isNullOrBlank()) {
                    SessionState.Unauthenticated
                } else {
                    SessionState.Authenticated
                }
            }
        }
    }

    suspend fun persistTokens(tokens: AuthTokens) {
        tokenWriteMutex.withLock {
            currentAccessToken = tokens.accessToken
            currentRefreshToken = tokens.refreshToken
            _sessionState.value = SessionState.Authenticated
            sessionStore.saveTokens(tokens)
        }
    }

    suspend fun clearSession() {
        tokenWriteMutex.withLock {
            currentAccessToken = null
            currentRefreshToken = null
            _sessionState.value = SessionState.Unauthenticated
            sessionStore.clearTokens()
        }
    }
}

sealed interface SessionState {
    data object Loading : SessionState
    data object Unauthenticated : SessionState
    data object Authenticated : SessionState
}

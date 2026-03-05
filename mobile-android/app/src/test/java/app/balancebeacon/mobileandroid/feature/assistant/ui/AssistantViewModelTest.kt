package app.balancebeacon.mobileandroid.feature.assistant.ui

import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsApi
import app.balancebeacon.mobileandroid.feature.accounts.data.AccountsRepository
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountDto
import app.balancebeacon.mobileandroid.feature.accounts.model.AccountsResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.ActivateAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.CreateAccountRequest
import app.balancebeacon.mobileandroid.feature.accounts.model.DeleteAccountResponse
import app.balancebeacon.mobileandroid.feature.accounts.model.UpdateAccountRequest
import app.balancebeacon.mobileandroid.feature.assistant.data.AssistantApi
import app.balancebeacon.mobileandroid.feature.assistant.data.AssistantRepository
import app.balancebeacon.mobileandroid.feature.assistant.data.AssistantSessionStore
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatRequest
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatSession
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantSessionMessage
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantSessionSnapshot
import app.balancebeacon.mobileandroid.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialize_loadsAccountsAndDefaultSession() = runTest {
        val store = FakeAssistantSessionStore()
        val viewModel = createViewModel(store = store)

        viewModel.initialize()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("acc_primary", state.accountId)
        assertEquals(1, state.sessions.size)
        assertEquals("Conversation 1", state.sessions.first().title)
        assertEquals(state.sessions.first().id, state.activeSessionId)
        assertNotNull(store.savedSnapshots["acc_primary::${state.monthKey}"])
    }

    @Test
    fun sendMessage_appendsAssistantReplyAndPersistsSessionHistory() = runTest {
        val store = FakeAssistantSessionStore()
        val viewModel = createViewModel(store = store)

        viewModel.initialize()
        advanceUntilIdle()
        viewModel.onMessageInputChanged("How am I doing this month?")
        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val currentSession = state.sessions.first { it.id == state.activeSessionId }
        assertEquals(2, currentSession.messages.size)
        assertEquals(listOf("user", "assistant"), currentSession.messages.map { it.role })
        assertEquals("How am I doing this month?", currentSession.messages.first().text)
        assertEquals("You are on track.", currentSession.messages.last().text)
        assertEquals("How am I doing this month?", currentSession.title)

        val persisted = store.savedSnapshots["acc_primary::${state.monthKey}"]
        assertNotNull(persisted)
        assertEquals(2, persisted!!.sessions.first().messages.size)
    }

    @Test
    fun selectAccount_loadsPersistedSessionsForThatScope() = runTest {
        val store = FakeAssistantSessionStore(
            initialSnapshots = mutableMapOf(
                "acc_secondary::2026-03" to AssistantSessionSnapshot(
                    sessions = listOf(
                        AssistantChatSession(
                            id = "session_secondary",
                            title = "Secondary history",
                            messages = listOf(
                                AssistantSessionMessage(
                                    id = "msg_saved",
                                    role = "assistant",
                                    text = "Saved response"
                                )
                            ),
                            createdAt = "2026-03-01T00:00:00.000Z",
                            updatedAt = "2026-03-01T00:00:00.000Z"
                        )
                    ),
                    activeSessionId = "session_secondary"
                )
            )
        )
        val viewModel = createViewModel(store = store)

        viewModel.initialize()
        advanceUntilIdle()
        viewModel.onMonthKeyChanged("2026-03")
        advanceUntilIdle()
        viewModel.selectAccount("acc_secondary")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("acc_secondary", state.accountId)
        assertEquals(1, state.sessions.size)
        assertEquals("Secondary history", state.sessions.first().title)
        assertEquals("Saved response", state.sessions.first().messages.first().text)
    }

    private fun createViewModel(
        store: FakeAssistantSessionStore,
        assistantApi: FakeAssistantApi = FakeAssistantApi()
    ): AssistantViewModel {
        return AssistantViewModel(
            assistantRepository = AssistantRepository(assistantApi),
            accountsRepository = AccountsRepository(FakeAccountsApi()),
            assistantSessionStore = store
        )
    }

    private class FakeAssistantSessionStore(
        initialSnapshots: MutableMap<String, AssistantSessionSnapshot> = mutableMapOf()
    ) : AssistantSessionStore {
        val savedSnapshots = initialSnapshots

        override suspend fun load(scopeKey: String): AssistantSessionSnapshot {
            return savedSnapshots[scopeKey] ?: AssistantSessionSnapshot()
        }

        override suspend fun save(scopeKey: String, snapshot: AssistantSessionSnapshot) {
            savedSnapshots[scopeKey] = snapshot
        }
    }

    private class FakeAccountsApi : AccountsApi {
        override suspend fun getAccounts(): AccountsResponse {
            return AccountsResponse(
                accounts = listOf(
                    AccountDto(id = "acc_primary", name = "Primary", type = "SELF", preferredCurrency = "USD"),
                    AccountDto(id = "acc_secondary", name = "Secondary", type = "SELF", preferredCurrency = "EUR")
                )
            )
        }

        override suspend fun createAccount(request: CreateAccountRequest): AccountDto {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun updateAccount(id: String, request: UpdateAccountRequest): AccountDto {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun deleteAccount(id: String): DeleteAccountResponse {
            throw UnsupportedOperationException("Not needed")
        }

        override suspend fun activateAccount(id: String, body: Map<String, String>): ActivateAccountResponse {
            throw UnsupportedOperationException("Not needed")
        }
    }

    private class FakeAssistantApi : AssistantApi {
        val requests = mutableListOf<AssistantChatRequest>()

        override suspend fun chat(request: AssistantChatRequest): Response<okhttp3.ResponseBody> {
            requests += request
            val body = """{"message":"You are on track."}"""
                .toResponseBody("application/json".toMediaType())
            return Response.success(body)
        }
    }
}

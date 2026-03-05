package app.balancebeacon.mobileandroid.feature.assistant.model

import kotlinx.serialization.Serializable

@Serializable
data class AssistantMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class AssistantChatRequest(
    val messages: List<AssistantMessageDto>,
    val accountId: String,
    val monthKey: String,
    val preferredCurrency: String? = null
)

@Serializable
data class AssistantChatResponse(
    val message: String? = null,
    val text: String? = null,
    val response: String? = null
)

@Serializable
data class AssistantSessionMessage(
    val id: String,
    val role: String,
    val text: String
)

@Serializable
data class AssistantChatSession(
    val id: String,
    val title: String,
    val messages: List<AssistantSessionMessage> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
    val isCustomTitle: Boolean = false
)

@Serializable
data class AssistantSessionSnapshot(
    val sessions: List<AssistantChatSession> = emptyList(),
    val activeSessionId: String? = null
)

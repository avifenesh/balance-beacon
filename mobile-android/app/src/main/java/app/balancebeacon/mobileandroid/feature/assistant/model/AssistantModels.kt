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

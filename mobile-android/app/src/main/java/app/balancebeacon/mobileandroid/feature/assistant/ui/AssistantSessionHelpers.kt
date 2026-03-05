package app.balancebeacon.mobileandroid.feature.assistant.ui

import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatSession
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantSessionMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

fun currentAssistantMonthKey(): String {
    val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date())
}

fun createAssistantSession(
    title: String,
    isCustomTitle: Boolean = false,
    nowIso: String = currentAssistantTimestamp()
): AssistantChatSession {
    return AssistantChatSession(
        id = UUID.randomUUID().toString(),
        title = title,
        messages = emptyList(),
        createdAt = nowIso,
        updatedAt = nowIso,
        isCustomTitle = isCustomTitle
    )
}

fun createAssistantMessage(
    role: String,
    text: String
): AssistantSessionMessage {
    return AssistantSessionMessage(
        id = UUID.randomUUID().toString(),
        role = role,
        text = text
    )
}

fun ensureAssistantSessions(
    sessions: List<AssistantChatSession>
): List<AssistantChatSession> {
    return if (sessions.isEmpty()) {
        listOf(createAssistantSession(title = "Conversation 1"))
    } else {
        sessions
    }
}

fun createAssistantTitleFromMessage(message: AssistantSessionMessage?): String? {
    val trimmed = message?.text?.trim().orEmpty()
    if (trimmed.isBlank()) {
        return null
    }
    return buildString {
        append(trimmed.take(40))
        if (trimmed.length > 40) {
            append("...")
        }
    }
}

private fun currentAssistantTimestamp(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date())
}

package app.balancebeacon.mobileandroid.feature.assistant.ui

import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantSessionMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantSessionHelpersTest {
    @Test
    fun ensureAssistantSessions_createsDefaultConversationWhenEmpty() {
        val sessions = ensureAssistantSessions(emptyList())

        assertEquals(1, sessions.size)
        assertEquals("Conversation 1", sessions.first().title)
        assertTrue(sessions.first().messages.isEmpty())
    }

    @Test
    fun createAssistantTitleFromMessage_truncatesLongPrompt() {
        val title = createAssistantTitleFromMessage(
            AssistantSessionMessage(
                id = "msg_1",
                role = "user",
                text = "Show me a very detailed summary of all monthly transactions and trends"
            )
        )

        assertTrue(title!!.startsWith("Show me a very detailed"))
        assertTrue(title.endsWith("..."))
    }
}

package app.balancebeacon.mobileandroid.feature.assistant.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantChatSession
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantSessionMessage
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel

private val assistantQuickPrompts = listOf(
    "Spending summary" to "Show me a summary of my spending this month",
    "Budget status" to "How am I doing on my budgets?",
    "Top expenses" to "What are my biggest expenses?",
    "Income trends" to "How has my income changed over the past 3 months?"
)

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val currentSession = remember(state.sessions, state.activeSessionId) {
        state.sessions.firstOrNull { it.id == state.activeSessionId } ?: state.sessions.firstOrNull()
    }

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Assistant", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Ask Balance AI with saved conversation threads scoped to the selected account and month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.isLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text("Loading assistant workspace...")
                    }
                }

                Text("Account", style = MaterialTheme.typography.labelLarge)
                if (state.accounts.isEmpty()) {
                    Text(
                        text = "No accounts available yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.accounts.forEach { account ->
                            FilterChip(
                                selected = state.accountId == account.id,
                                onClick = { viewModel.selectAccount(account.id) },
                                label = { Text(account.name) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.monthKey,
                    onValueChange = viewModel::onMonthKeyChanged,
                    label = { Text("Month (YYYY-MM)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.sessions.forEach { session ->
                        FilterChip(
                            selected = currentSession?.id == session.id,
                            onClick = { viewModel.selectSession(session.id) },
                            label = { Text(session.title) }
                        )
                    }
                    OutlinedButton(onClick = viewModel::createNewSession) {
                        Text("New")
                    }
                    OutlinedButton(
                        onClick = {
                            currentSession?.id?.let(viewModel::deleteSession)
                        },
                        enabled = state.sessions.size > 1 && currentSession != null
                    ) {
                        Text("Delete")
                    }
                }

                OutlinedTextField(
                    value = state.messageInput,
                    onValueChange = viewModel::onMessageInputChanged,
                    label = { Text("Ask the assistant") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = viewModel::sendMessage,
                        enabled = !state.isSending
                    ) {
                        Text(if (state.isSending) "Sending..." else "Send")
                    }
                    if (state.isSending) {
                        CircularProgressIndicator()
                    }
                }

                Text("Quick prompts", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    assistantQuickPrompts.forEach { (label, prompt) ->
                        OutlinedButton(
                            onClick = { viewModel.onMessageInputChanged(prompt) }
                        ) {
                            Text(label)
                        }
                    }
                }

                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentSession == null || currentSession.messages.isEmpty()) {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Text("No messages in this conversation yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = currentSession.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Saved messages stay grouped by account and month.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(
                    items = currentSession.messages,
                    key = { it.id }
                ) { message ->
                    AssistantMessageCard(message = message)
                }
            }
        }
    }
}

@Composable
private fun AssistantMessageCard(message: AssistantSessionMessage) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = message.role.replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

package app.balancebeacon.mobileandroid.feature.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Assistant", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Ask questions about your account and month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = state.accountId,
                    onValueChange = viewModel::onAccountIdChanged,
                    label = { Text("Account ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.monthKey,
                    onValueChange = viewModel::onMonthKeyChanged,
                    label = { Text("Month (YYYY-MM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.messageInput,
                    onValueChange = viewModel::onMessageInputChanged,
                    label = { Text("Ask the assistant") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = viewModel::sendMessage,
                        enabled = !state.isLoading
                    ) {
                        Text(if (state.isLoading) "Sending..." else "Send")
                    }
                    if (state.isLoading) {
                        CircularProgressIndicator()
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
            if (state.conversation.isEmpty()) {
                item {
                    Text("No chat yet. Send a message to start.")
                }
            }
            items(
                items = state.conversation,
                key = { "${it.role}-${it.text.hashCode()}" }
            ) { message ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            message.role.replaceFirstChar { it.uppercaseChar() },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            message.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

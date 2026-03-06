package app.balancebeacon.mobileandroid.feature.assistant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.assistant.model.AssistantSessionMessage
import app.balancebeacon.mobileandroid.ui.util.sanitizeError
import app.balancebeacon.mobileandroid.ui.components.BouncingDotsIndicator
import app.balancebeacon.mobileandroid.ui.theme.GlassBorder
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import app.balancebeacon.mobileandroid.ui.theme.GlassSurface
import app.balancebeacon.mobileandroid.ui.theme.Sky300
import app.balancebeacon.mobileandroid.ui.theme.SkyBlue
import app.balancebeacon.mobileandroid.ui.theme.Slate200

private val assistantQuickPrompts = listOf(
    "Spending summary" to "Show me a summary of my spending this month",
    "Budget status" to "How am I doing on my budgets?",
    "Top expenses" to "What are my biggest expenses?",
    "Income trends" to "How has my income changed over the past 3 months?"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onMessageInputChanged(spokenText)
            }
        }
    }
    val currentSession = remember(state.sessions, state.activeSessionId) {
        state.sessions.firstOrNull { it.id == state.activeSessionId } ?: state.sessions.firstOrNull()
    }
    var showDeleteConfirmId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = SkyBlue
                    )
                    Text("Balance AI 3.1", style = MaterialTheme.typography.headlineSmall)
                }
            }

            // Loading indicator
            if (state.isLoading) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text("Loading assistant workspace...")
                    }
                }
            }

            // Account chips
            if (state.accounts.isEmpty() && state.error == null && !state.isLoading) {
                item {
                    Text(
                        text = "No accounts yet. Create one to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (state.accounts.isNotEmpty()) {
                item {
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
            }

            // Session tab row with inline rename
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.sessions.forEach { session ->
                        if (state.renamingSessionId == session.id) {
                            // Inline rename mode
                            OutlinedTextField(
                                value = state.sessionTitleInput,
                                onValueChange = viewModel::onSessionTitleChanged,
                                modifier = Modifier.widthIn(min = 120.dp, max = 200.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true
                            )
                            IconButton(
                                onClick = viewModel::confirmRenaming,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Confirm rename",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = viewModel::cancelRenaming,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel rename",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            FilterChip(
                                selected = currentSession?.id == session.id,
                                onClick = { viewModel.selectSession(session.id) },
                                label = { Text(session.title) }
                            )
                            if (currentSession?.id == session.id) {
                                IconButton(
                                    onClick = { viewModel.startRenamingSession(session.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename session",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (state.sessions.size > 1) {
                                    IconButton(
                                        onClick = { showDeleteConfirmId = session.id },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete session",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = viewModel::createNewSession) {
                        Text("New")
                    }
                }
            }

            // Messages or empty state
            if (currentSession == null || currentSession.messages.isEmpty()) {
                item {
                    AssistantEmptyState(onQuickPrompt = viewModel::onMessageInputChanged)
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
                items(items = currentSession.messages, key = { it.id }) { message ->
                    when (message.role) {
                        "user" -> UserMessageBubble(message = message)
                        else -> AssistantMessageBubble(message = message, isSending = state.isSending)
                    }
                }
            }
        }

        // Fixed bottom bar: message input + send button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.error?.let { error ->
                Text(
                    text = sanitizeError(error),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.messageInput,
                    onValueChange = viewModel::onMessageInputChanged,
                    placeholder = { Text("Ask Balance AI...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Balance AI...")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Speech recognition not available",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !state.isSending
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice input",
                        tint = SkyBlue
                    )
                }
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        viewModel.sendMessage()
                    },
                    enabled = !state.isSending
                ) {
                    if (state.isSending) {
                        BouncingDotsIndicator(dotSize = 6.dp)
                    } else {
                        Text("Send")
                    }
                }
                if (state.isSending) {
                    OutlinedButton(onClick = viewModel::stopSending) {
                        Text("Stop")
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirmId?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmId = null },
            title = { Text("Delete conversation") },
            text = { Text("Are you sure you want to delete this conversation? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.deleteSession(sessionId)
                    showDeleteConfirmId = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserMessageBubble(message: AssistantSessionMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    brush = Brush.linearGradient(listOf(SkyBlue, Color(0xFF6366F1))),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
                )
                .padding(12.dp)
        ) {
            Text(text = message.text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}

@Composable
private fun AssistantMessageBubble(message: AssistantSessionMessage, isSending: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = GlassSurface,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                if (message.text.isBlank() && isSending) {
                    BouncingDotsIndicator()
                } else {
                    Text(
                        text = message.text.ifBlank { "Thinking..." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate200
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantEmptyState(onQuickPrompt: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(shape = CircleShape, color = SkyBlue.copy(alpha = 0.15f), modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(32.dp), tint = SkyBlue)
            }
        }
        Text("Ask anything about your finances", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            "Balance AI can help with spending summaries, budget advice, and expense analysis.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            assistantQuickPrompts.forEach { (label, prompt) ->
                Surface(
                    onClick = { onQuickPrompt(prompt) },
                    shape = RoundedCornerShape(20.dp),
                    color = GlassSurface,
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Sky300
                    )
                }
            }
        }
    }
}

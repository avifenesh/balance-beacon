package app.balancebeacon.mobileandroid.feature.sharing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import app.balancebeacon.mobileandroid.feature.sharing.model.SharedExpenseDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharedWithMeParticipationDto
import androidx.compose.ui.unit.dp

@Composable
fun SharingScreen(
    viewModel: SharingViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var createTransactionId by rememberSaveable { mutableStateOf("") }
    var createParticipantEmail by rememberSaveable { mutableStateOf("") }
    var createShareAmount by rememberSaveable { mutableStateOf("") }
    var createDescription by rememberSaveable { mutableStateOf("") }
    var lookupEmail by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.isLoading) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text("Loading sharing data...")
                }
            }
        }
        state.error?.let { error ->
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = viewModel::load) {
                        Text("Retry load")
                    }
                }
            }
        }

        item {
            Text("Create Shared Expense", style = MaterialTheme.typography.titleMedium)
        }
        item {
            OutlinedTextField(
                value = createTransactionId,
                onValueChange = { createTransactionId = it },
                label = { Text("Transaction ID") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = createParticipantEmail,
                onValueChange = { createParticipantEmail = it },
                label = { Text("Participant email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = createShareAmount,
                onValueChange = { createShareAmount = it },
                label = { Text("Share amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = createDescription,
                onValueChange = { createDescription = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = {
                    viewModel.createSharedExpense(
                        transactionId = createTransactionId,
                        participantEmail = createParticipantEmail,
                        shareAmount = createShareAmount,
                        description = createDescription
                    )
                },
                enabled = !state.isActionInProgress
            ) {
                Text(if (state.isActionInProgress) "Working..." else "Create Shared Expense")
            }
        }
        item {
            Text(
                "Lookup User",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        item {
            OutlinedTextField(
                value = lookupEmail,
                onValueChange = { lookupEmail = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = { viewModel.lookupUser(lookupEmail) },
                enabled = !state.isActionInProgress
            ) {
                Text(if (state.isActionInProgress) "Working..." else "Lookup User")
            }
        }
        state.lookedUpUser?.let { user ->
            item {
                val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "N/A"
                Text(
                    text = "Lookup result: ${user.email} ($displayName)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        state.actionMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = if (state.actionMessageIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            Text(
                "Settlement Balances",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        if (state.settlementBalances.isEmpty()) {
            item {
                Text("No settlement balances")
            }
        } else {
            items(
                items = state.settlementBalances,
                key = { "${it.userId}-${it.currency}" }
            ) { balance ->
                val displayName = balance.userDisplayName?.takeIf { it.isNotBlank() } ?: balance.userEmail
                Text(
                    text = "$displayName: ${balance.netBalance} ${balance.currency}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            Text(
                "Shared By Me",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        if (state.sharedByMe.isEmpty()) {
            item {
                Text("No shared expenses")
            }
        }
        items(state.sharedByMe) { sharedExpense ->
            SharedByMeItem(
                sharedExpense = sharedExpense,
                isActionInProgress = state.isActionInProgress,
                onDeleteShare = { viewModel.deleteShare(sharedExpense.id) },
                onMarkPaid = viewModel::markParticipantPaid,
                onSendReminder = viewModel::sendReminder
            )
        }
        item {
            Text(
                "Shared With Me",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        if (state.sharedWithMe.isEmpty()) {
            item {
                Text("No shared expenses shared with you")
            }
        }
        items(state.sharedWithMe) { participation ->
            SharedWithMeItem(
                participation = participation,
                isActionInProgress = state.isActionInProgress,
                onDecline = viewModel::declineShare
            )
        }
    }
}

@Composable
private fun SharedByMeItem(
    sharedExpense: SharedExpenseDto,
    isActionInProgress: Boolean,
    onDeleteShare: () -> Unit,
    onMarkPaid: (String) -> Unit,
    onSendReminder: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val title = sharedExpense.description
            ?: sharedExpense.transaction?.description
            ?: "Shared expense"
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = "${sharedExpense.totalAmount} ${sharedExpense.currency} (${sharedExpense.splitType})",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = onDeleteShare, enabled = !isActionInProgress) {
            Text("Delete Share")
        }

        if (sharedExpense.participants.isEmpty()) {
            Text("No participants", style = MaterialTheme.typography.bodySmall)
        } else {
            sharedExpense.participants.forEach { participant ->
                val participantName = participant.participant?.displayName?.takeIf { it.isNotBlank() }
                    ?: participant.participant?.email
                    ?: "Participant"
                Text(
                    text = "$participantName • ${participant.shareAmount} • ${participant.status}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (participant.status.equals("PENDING", ignoreCase = true)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onMarkPaid(participant.id) },
                            enabled = !isActionInProgress
                        ) {
                            Text("Mark Paid")
                        }
                        OutlinedButton(
                            onClick = { onSendReminder(participant.id) },
                            enabled = !isActionInProgress
                        ) {
                            Text("Send Reminder")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedWithMeItem(
    participation: SharedWithMeParticipationDto,
    isActionInProgress: Boolean,
    onDecline: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val sharedExpense = participation.sharedExpense
        val title = sharedExpense?.description
            ?: sharedExpense?.transaction?.description
            ?: "Shared expense"
        val currency = sharedExpense?.currency ?: ""
        val owner = sharedExpense?.participants?.firstOrNull()?.participant?.displayName
            ?: sharedExpense?.participants?.firstOrNull()?.participant?.email
            ?: "Unknown"

        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Owner: $owner",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Your share: ${participation.shareAmount} $currency (${participation.status})",
            style = MaterialTheme.typography.bodyMedium
        )
        if (participation.status.equals("PENDING", ignoreCase = true)) {
            Button(
                onClick = { onDecline(participation.id) },
                enabled = !isActionInProgress
            ) {
                Text("Decline")
            }
        }
    }
}

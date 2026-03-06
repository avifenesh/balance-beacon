package app.balancebeacon.mobileandroid.feature.sharing.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.sharing.model.PaymentHistoryItemDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SettlementBalanceDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharedExpenseDto
import app.balancebeacon.mobileandroid.feature.sharing.model.SharedWithMeParticipationDto
import app.balancebeacon.mobileandroid.ui.components.SkeletonCard
import app.balancebeacon.mobileandroid.ui.components.StatusBadge
import app.balancebeacon.mobileandroid.ui.theme.Emerald400
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import app.balancebeacon.mobileandroid.ui.util.sanitizeError
import app.balancebeacon.mobileandroid.ui.theme.Rose400
import app.balancebeacon.mobileandroid.ui.theme.Slate700

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharingScreen(
    viewModel: SharingViewModel,
    initialTransactionId: String = "",
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current
    var showDeleteShareId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeclineShareId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettleAllKey by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }
    LaunchedEffect(initialTransactionId) {
        if (initialTransactionId.isNotBlank()) {
            viewModel.onCreateTransactionIdChanged(initialTransactionId)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            viewModel.load()
        },
        modifier = modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.isLoading) {
            item { SkeletonCard() }
            item { SkeletonCard() }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Create Shared Expense", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.createTransactionId,
                        onValueChange = viewModel::onCreateTransactionIdChanged,
                        label = { Text("Transaction ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (initialTransactionId.isNotBlank()) {
                        Text(
                            text = "Transaction ID prefilled from Transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.createSplitType == "EQUAL",
                            onClick = { viewModel.onSplitTypeChanged("EQUAL") },
                            label = { Text("Equal") }
                        )
                        FilterChip(
                            selected = state.createSplitType == "PERCENTAGE",
                            onClick = { viewModel.onSplitTypeChanged("PERCENTAGE") },
                            label = { Text("Percentage") }
                        )
                        FilterChip(
                            selected = state.createSplitType == "FIXED",
                            onClick = { viewModel.onSplitTypeChanged("FIXED") },
                            label = { Text("Fixed") }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.newParticipantEmail,
                            onValueChange = viewModel::onNewParticipantEmailChanged,
                            label = { Text("Participant email") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = viewModel::addParticipant,
                            enabled = !state.isLookingUpParticipant
                        ) {
                            if (state.isLookingUpParticipant) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Add participant")
                            }
                        }
                    }
                }
            }
        }
        if (state.participants.isNotEmpty()) {
            itemsIndexed(state.participants) { index, participant ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = participant.displayName ?: participant.email,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (participant.displayName != null) {
                                Text(
                                    text = participant.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (state.createSplitType == "PERCENTAGE") {
                            OutlinedTextField(
                                value = participant.shareValue?.toString() ?: "",
                                onValueChange = { viewModel.updateParticipantValue(index, it.toDoubleOrNull()) },
                                label = { Text("%") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(90.dp)
                            )
                        } else if (state.createSplitType == "FIXED") {
                            OutlinedTextField(
                                value = participant.shareValue?.toString() ?: "",
                                onValueChange = { viewModel.updateParticipantValue(index, it.toDoubleOrNull()) },
                                label = { Text("Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(100.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.removeParticipant(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Rose400)
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.createDescription,
                onValueChange = viewModel::onCreateDescriptionChanged,
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = viewModel::createStructuredSharedExpense,
                enabled = !state.isActionInProgress
            ) {
                Text(if (state.isActionInProgress) "Working..." else "Create Shared Expense")
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
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Settlement Balances",
                        style = MaterialTheme.typography.titleMedium
                    )
                    state.error?.let { error ->
                        Text(
                            text = sanitizeError(error),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(onClick = viewModel::load) {
                            Text("Retry load")
                        }
                    }
                    if (state.error == null) {
                        if (state.settlementBalances.isEmpty()) {
                            Text("No settlement balances")
                        } else {
                            SettlementSummaryCard(state.settlementBalances)
                        }
                    }
                }
            }
        }
        if (state.error == null && state.settlementBalances.isNotEmpty()) {
            items(
                items = state.settlementBalances,
                key = { "${it.userId}-${it.currency}" }
            ) { balance ->
                SettlementBalanceItem(
                    balance = balance,
                    isActionInProgress = state.isActionInProgress,
                    onSettle = { showSettleAllKey = balance.userId to balance.currency }
                )
            }
        }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Payment History",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (state.paymentHistory.isEmpty()) {
                        Text("No payment history")
                    }
                }
            }
        }
        if (state.paymentHistory.isNotEmpty()) {
            items(
                items = state.paymentHistory,
                key = { "${it.participantId}:${it.paidAt}" }
            ) { payment ->
                PaymentHistoryItem(payment = payment)
            }
        }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Shared By Me",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (state.sharedByMe.isEmpty()) {
                        Text("No shared expenses")
                    }
                }
            }
        }
        items(state.sharedByMe) { sharedExpense ->
            SharedByMeItem(
                sharedExpense = sharedExpense,
                isActionInProgress = state.isActionInProgress,
                onDeleteShare = { showDeleteShareId = sharedExpense.id },
                onMarkPaid = viewModel::markParticipantPaid,
                onSendReminder = viewModel::sendReminder
            )
        }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Shared With Me",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (state.sharedWithMe.isEmpty()) {
                        Text("No shared expenses shared with you")
                    }
                }
            }
        }
        items(state.sharedWithMe) { participation ->
            SharedWithMeItem(
                participation = participation,
                isActionInProgress = state.isActionInProgress,
                onDecline = { showDeclineShareId = it }
            )
        }
    }
    } // PullToRefreshBox

    showDeleteShareId?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteShareId = null },
            title = { Text("Delete shared expense") },
            text = { Text("Are you sure you want to delete this shared expense? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.deleteShare(id)
                    showDeleteShareId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteShareId = null }) { Text("Cancel") }
            }
        )
    }

    showDeclineShareId?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeclineShareId = null },
            title = { Text("Decline shared expense") },
            text = { Text("Are you sure you want to decline this shared expense?") },
            confirmButton = {
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.declineShare(id)
                    showDeclineShareId = null
                }) { Text("Decline") }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineShareId = null }) { Text("Cancel") }
            }
        )
    }

    showSettleAllKey?.let { (userId, currency) ->
        AlertDialog(
            onDismissRequest = { showSettleAllKey = null },
            title = { Text("Settle all balances") },
            text = { Text("Are you sure you want to settle all balances with this user?") },
            confirmButton = {
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.settleAllWithUser(userId, currency)
                    showSettleAllKey = null
                }) { Text("Settle") }
            },
            dismissButton = {
                TextButton(onClick = { showSettleAllKey = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettlementBalanceItem(
    balance: SettlementBalanceDto,
    isActionInProgress: Boolean,
    onSettle: () -> Unit
) {
    val displayName = balance.userDisplayName?.takeIf { it.isNotBlank() } ?: balance.userEmail
    val netBalance = balance.netBalance.toDoubleOrNull() ?: 0.0
    val canSettle = netBalance > 0.0

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$displayName: ${balance.netBalance} ${balance.currency}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (canSettle) {
                Button(
                    onClick = onSettle,
                    enabled = !isActionInProgress
                ) {
                    Text("Settle All")
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryItem(
    payment: PaymentHistoryItemDto
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        val directionText = if (payment.direction.equals("received", ignoreCase = true)) {
            "${payment.userDisplayName} paid you"
        } else {
            "You paid ${payment.userDisplayName}"
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(directionText, style = MaterialTheme.typography.titleSmall)
            Text(
                "${payment.amount} ${payment.currency}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(payment.paidAt, style = MaterialTheme.typography.bodySmall)
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
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$participantName • ${participant.shareAmount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        StatusBadge(status = participant.status)
                    }

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
}

@Composable
private fun SharedWithMeItem(
    participation: SharedWithMeParticipationDto,
    isActionInProgress: Boolean,
    onDecline: (String) -> Unit
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Your share: ${participation.shareAmount} $currency",
                    style = MaterialTheme.typography.bodyMedium
                )
                StatusBadge(status = participation.status)
            }
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
}

@Composable
private fun SettlementSummaryCard(balances: List<SettlementBalanceDto>) {
    val currencies = balances.map { it.currency }.distinct()
    currencies.forEach { currency ->
        val currencyBalances = balances.filter { it.currency == currency }
        val totalYouOwe = currencyBalances.sumOf { it.youOwe.toDoubleOrNull() ?: 0.0 }
        val totalTheyOwe = currencyBalances.sumOf { it.theyOwe.toDoubleOrNull() ?: 0.0 }
        val netBalance = totalTheyOwe - totalYouOwe

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currencies.size > 1) {
                    Text(currency, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("You owe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatSettlementAmount(totalYouOwe, currency), style = MaterialTheme.typography.titleMedium, color = Rose400)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate700)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("They owe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatSettlementAmount(totalTheyOwe, currency), style = MaterialTheme.typography.titleMedium, color = Emerald400)
                    }
                }
                Surface(shape = RoundedCornerShape(12.dp), color = if (netBalance >= 0) Emerald400.copy(alpha = 0.1f) else Rose400.copy(alpha = 0.1f)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Net balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = (if (netBalance >= 0) "+" else "") + formatSettlementAmount(netBalance, currency),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (netBalance >= 0) Emerald400 else Rose400
                        )
                    }
                }
            }
        }
    }
}

private fun formatSettlementAmount(amount: Double, currency: String): String {
    return runCatching {
        val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US)
        formatter.currency = java.util.Currency.getInstance(currency)
        formatter.maximumFractionDigits = 2
        formatter.format(amount)
    }.getOrElse { String.format(java.util.Locale.US, "%.2f %s", amount, currency) }
}

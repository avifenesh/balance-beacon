package app.balancebeacon.mobileandroid.feature.settings.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.ui.components.SkeletonFormScreen
import app.balancebeacon.mobileandroid.ui.theme.Emerald400
import app.balancebeacon.mobileandroid.ui.theme.GlassBorder
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import app.balancebeacon.mobileandroid.ui.theme.GlassSurface
import app.balancebeacon.mobileandroid.ui.util.sanitizeError

private val CurrencyOptions = listOf("USD", "EUR", "ILS")
private val ExportFormatOptions = listOf("JSON", "CSV")

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    var showDeleteAccountConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    if (state.isLoading) {
        SkeletonFormScreen(modifier = modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Preferred currency", style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyOptions.forEach { option ->
                        FilterChip(
                            selected = (state.currency == option),
                            onClick = { viewModel.onCurrencyChanged(option) },
                            label = { Text(option) }
                        )
                    }
                }

                Button(
                    onClick = viewModel::saveCurrency,
                    enabled = !state.isSavingCurrency && !state.isDeletingAccount
                ) {
                    Text(if (state.isSavingCurrency) "Saving..." else "Save currency")
                }
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Export my data", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Generate and share a portable export of your account data.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExportFormatOptions.forEach { option ->
                        val isSelected = state.selectedExportFormat == option
                        val borderColor = if (isSelected) Emerald400 else GlassBorder
                        val bgColor = if (isSelected) Emerald400.copy(alpha = 0.1f) else GlassSurface
                        Surface(
                            onClick = { viewModel.onExportFormatChanged(option) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = bgColor,
                            border = BorderStroke(2.dp, borderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (option == "JSON") Icons.Default.DataObject else Icons.Default.TableChart,
                                    contentDescription = null, modifier = Modifier.size(32.dp),
                                    tint = if (isSelected) Emerald400 else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(option, style = MaterialTheme.typography.titleSmall, color = if (isSelected) Emerald400 else Color.White)
                                Text(
                                    if (option == "JSON") "Structured data" else "Spreadsheet format",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = viewModel::exportMyData,
                    enabled = !state.isExportingData && !state.isDeletingAccount
                ) {
                    Text(
                        if (state.isExportingData) {
                            "Exporting..."
                        } else {
                            "Export data (${state.selectedExportFormat})"
                        }
                    )
                }
                state.exportFormat?.let { format ->
                    Text("Last export format: $format", style = MaterialTheme.typography.bodySmall)
                }
                state.exportGeneratedAt?.let { exportedAt ->
                    Text("Exported at: $exportedAt", style = MaterialTheme.typography.bodySmall)
                }
                if (!state.exportData.isNullOrBlank()) {
                    OutlinedButton(
                        enabled = !state.isDeletingAccount,
                        onClick = {
                            val exportFormat = state.exportFormat?.lowercase() ?: "json"
                            val mimeType = if (exportFormat == "csv") "text/csv" else "application/json"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = mimeType
                                putExtra(Intent.EXTRA_SUBJECT, "Balance Beacon export ($exportFormat)")
                                putExtra(Intent.EXTRA_TEXT, state.exportData)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Share export")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    ) {
                        Text("Share last export")
                    }
                }
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Delete account", style = MaterialTheme.typography.titleMedium)
                Text(
                    "This action is permanent. Type your account email to confirm.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Account email: ${state.accountEmail}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = state.deleteConfirmEmail,
                    onValueChange = viewModel::onDeleteConfirmEmailChanged,
                    label = { Text("Confirm email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { showDeleteAccountConfirm = true },
                    enabled = !state.isDeletingAccount && !state.isSavingCurrency && !state.isExportingData
                ) {
                    Text(if (state.isDeletingAccount) "Deleting..." else "Delete account")
                }
            }
        }

        if (state.isSavingCurrency || state.isExportingData || state.isDeletingAccount) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text(
                    when {
                        state.isDeletingAccount -> "Deleting account..."
                        state.isExportingData -> "Exporting your data..."
                        else -> "Saving your settings..."
                    }
                )
            }
        }

        state.message?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }

        state.error?.let { errorText ->
            Text(text = sanitizeError(errorText), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirm = false },
            title = { Text("Delete account") },
            text = { Text("This will permanently delete your account and all data. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.deleteAccount()
                    showDeleteAccountConfirm = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

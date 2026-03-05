package app.balancebeacon.mobileandroid.feature.settings.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel

private val CurrencyOptions = listOf("USD", "EUR", "ILS")
private val ExportFormatOptions = listOf("JSON", "CSV")

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator()
                Text("Loading settings...")
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineSmall)
                Text("Preferred currency", style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyOptions.forEach { option ->
                        OutlinedButton(
                            onClick = { viewModel.onCurrencyChanged(option) }
                        ) {
                            Text(option)
                        }
                    }
                }

                Text("Selected: ${state.currency}", style = MaterialTheme.typography.bodyMedium)

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormatOptions.forEach { option ->
                        OutlinedButton(
                            onClick = { viewModel.onExportFormatChanged(option) }
                        ) {
                            Text(if (state.selectedExportFormat == option) "• $option" else option)
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
                    onClick = viewModel::deleteAccount,
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

        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}

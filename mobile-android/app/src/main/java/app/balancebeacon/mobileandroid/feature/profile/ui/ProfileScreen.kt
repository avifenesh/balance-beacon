package app.balancebeacon.mobileandroid.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.ui.components.SkeletonFormScreen
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import app.balancebeacon.mobileandroid.ui.util.sanitizeError

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current
    var showDeleteAccountConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Profile", style = MaterialTheme.typography.headlineSmall)

                OutlinedTextField(
                    value = state.email,
                    onValueChange = {},
                    label = { Text("Email") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = viewModel::onDisplayNameChanged,
                    label = { Text("Display name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Preferred currency: ${state.preferredCurrency}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = viewModel::saveProfile,
                    enabled = !state.isSaving
                ) {
                    Text(if (state.isSaving) "Saving..." else "Save profile")
                }
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.exportFormat,
                    onValueChange = viewModel::onExportFormatChanged,
                    label = { Text("Export format (json/csv)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = viewModel::exportData,
                    enabled = !state.isExporting
                ) {
                    Text(if (state.isExporting) "Exporting..." else "Export data")
                }

                OutlinedTextField(
                    value = state.confirmEmail,
                    onValueChange = viewModel::onConfirmEmailChanged,
                    label = { Text("Confirm email for delete") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { showDeleteAccountConfirm = true },
                    enabled = !state.isDeleting
                ) {
                    Text(if (state.isDeleting) "Deleting..." else "Delete account")
                }
            }
        }

        state.accountStatus?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }

        state.exportStatus?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }

        state.error?.let {
            Text(text = sanitizeError(it), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

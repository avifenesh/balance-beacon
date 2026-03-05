package app.balancebeacon.mobileandroid.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
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
            onClick = viewModel::deleteAccount,
            enabled = !state.isDeleting
        ) {
            Text(if (state.isDeleting) "Deleting..." else "Delete account")
        }

        state.accountStatus?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }

        state.exportStatus?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }

        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}

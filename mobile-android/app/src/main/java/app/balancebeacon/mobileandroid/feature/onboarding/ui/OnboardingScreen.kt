package app.balancebeacon.mobileandroid.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Onboarding", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = state.currency,
            onValueChange = viewModel::onCurrencyChanged,
            label = { Text("Currency") },
            modifier = Modifier.padding(top = 16.dp)
        )
        Button(
            onClick = viewModel::complete,
            enabled = !state.isSubmitting,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(if (state.isSubmitting) "Saving..." else "Complete Onboarding")
        }
        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

package app.balancebeacon.mobileandroid.feature.subscription.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator()
            }

            state.error != null -> {
                Text(state.error ?: "Failed to load subscription", color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::load, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Retry")
                }
            }

            else -> {
                val subscription = state.snapshot
                Text("Subscription status: ${subscription?.status ?: "unknown"}")
                if (!subscription?.plan.isNullOrBlank()) {
                    Text("Plan: ${subscription?.plan}")
                }
                Button(onClick = viewModel::load, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Refresh")
                }
            }
        }
    }
}

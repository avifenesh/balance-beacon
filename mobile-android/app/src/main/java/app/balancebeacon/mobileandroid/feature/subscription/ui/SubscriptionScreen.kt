package app.balancebeacon.mobileandroid.feature.subscription.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import app.balancebeacon.mobileandroid.ui.util.sanitizeError

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
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        state.isLoading -> {
                            CircularProgressIndicator()
                        }

                        state.error != null -> {
                            Text(sanitizeError(state.error ?: "Failed to load subscription"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = viewModel::load) {
                                Text("Retry")
                            }
                        }

                        else -> {
                            val subscription = state.snapshot
                            Text("Subscription status: ${subscription?.status ?: "unknown"}")
                            if (!subscription?.plan.isNullOrBlank()) {
                                Text("Plan: ${subscription?.plan}")
                            }
                            Button(onClick = viewModel::load) {
                                Text("Refresh")
                            }
                        }
                    }
                }
            }
        }
    }
}

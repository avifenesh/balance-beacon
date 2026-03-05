package app.balancebeacon.mobileandroid.feature.paywall.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PaywallUiState(
    val title: String = "Subscription required",
    val message: String = "Upgrade to continue using Balance Beacon mobile."
)

class PaywallViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()
}


package app.balancebeacon.mobileandroid.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = SkyBlue,
    onPrimary = Slate900,
    background = Slate900,
    onBackground = androidx.compose.ui.graphics.Color.White,
    surface = Slate900,
    onSurface = androidx.compose.ui.graphics.Color.White,
    error = Red400
)

@Composable
fun BalanceBeaconTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content
    )
}

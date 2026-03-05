package app.balancebeacon.mobileandroid.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = GlassPrimary,
    onPrimary = White,
    primaryContainer = GlassSurfaceStrong,
    onPrimaryContainer = White,
    secondary = SkyBlue,
    onSecondary = White,
    background = Slate950,
    onBackground = Slate200,
    surface = GlassSurface,
    onSurface = White,
    surfaceVariant = GlassSurfaceStrong,
    onSurfaceVariant = Slate300,
    outline = GlassBorder,
    error = Rose400,
    onError = White
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun BalanceBeaconTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

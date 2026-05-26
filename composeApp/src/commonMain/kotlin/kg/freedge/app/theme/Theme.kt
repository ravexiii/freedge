package kg.freedge.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = Color(0xFF558B2F),
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Color(0xFF2E7D32),
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = BackgroundLight,
    surface = SurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003910),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFB8F0C1),
    secondary = Color(0xFFAED581),
    onSecondary = Color(0xFF1B3700),
    secondaryContainer = Color(0xFF33691E),
    onSecondaryContainer = Color(0xFFDCEDC8),
    tertiary = Color(0xFFA5D6A7),
    onTertiary = Color(0xFF003910),
    tertiaryContainer = Color(0xFF2E7D32),
    onTertiaryContainer = Color(0xFFE8F5E9),
    background = Color(0xFF0D1B0D),
    onBackground = Color(0xFFE6F2E6),
    surface = Color(0xFF121F12),
    onSurface = Color(0xFFE6F2E6),
    surfaceVariant = Color(0xFF1B2A1B),
    onSurfaceVariant = Color(0xFFA5C8A5),
)

@Composable
fun FreedgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FreedgeTypography,
        content = content
    )
}

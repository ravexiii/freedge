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
    background = Color(0xFF0D1B0D),
    surface = Color(0xFF121F12),
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

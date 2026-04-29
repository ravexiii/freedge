package kg.freedge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MintDarkPrimary,
    onPrimary = MintDarkOnPrimary,
    primaryContainer = MintDarkPrimaryContainer,
    onPrimaryContainer = MintDarkOnPrimaryContainer,
    secondary = ApricotDarkSecondary,
    secondaryContainer = ApricotDarkSecondaryContainer,
    onSecondaryContainer = ApricotDarkOnSecondaryContainer,
    tertiary = LavenderDarkTertiary,
    tertiaryContainer = LavenderDarkTertiaryContainer,
    onTertiaryContainer = LavenderDarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurfaceLow,
    surfaceContainerHigh = DarkSurfaceHigh,
    onSurfaceVariant = DarkTextMuted,
    outlineVariant = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = MintOnPrimary,
    primaryContainer = MintPrimaryContainer,
    onPrimaryContainer = MintOnPrimaryContainer,
    secondary = ApricotSecondary,
    secondaryContainer = ApricotSecondaryContainer,
    onSecondaryContainer = ApricotOnSecondaryContainer,
    tertiary = LavenderTertiary,
    tertiaryContainer = LavenderTertiaryContainer,
    onTertiaryContainer = LavenderOnTertiaryContainer,
    background = PastelBackground,
    onBackground = PastelText,
    surface = PastelSurface,
    onSurface = PastelText,
    surfaceContainerLow = PastelSurfaceLow,
    surfaceContainer = PastelSurfaceLow,
    surfaceContainerHigh = PastelSurfaceHigh,
    onSurfaceVariant = PastelTextMuted,
    outlineVariant = PastelOutline
)

@Composable
fun FreedgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

package pe.edu.upeu.gymvip.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = GoldOnPrimary,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = GoldPrimary,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurfacePanel,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    error = androidx.compose.ui.graphics.Color(0xFFF87171),
    errorContainer = androidx.compose.ui.graphics.Color(0xFF331618),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFF87171),
    onError = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightSurfaceElevated,
    onPrimaryContainer = LightPrimary,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurfacePanel,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    error = androidx.compose.ui.graphics.Color(0xFFB91C1C),
    onError = LightSurfacePanel
)

@Composable
fun GymVipTheme(
    themePreference: String = "DARK", // SYSTEM, DARK, LIGHT
    onThemeChanged: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val isDark = when (themePreference) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    androidx.compose.runtime.SideEffect { onThemeChanged(isDark) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VipTypography,
        shapes = VipShapes,
        content = { Surface(Modifier.fillMaxSize(), color = colorScheme.background, contentColor = colorScheme.onBackground) { content() } }
    )
}

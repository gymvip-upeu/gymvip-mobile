package com.gymvip.mobile.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalGymVipSystemTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = GoldOnPrimary,
    primaryContainer = Color(0xFF302619),
    onPrimaryContainer = GoldPrimary,
    secondary = TextPrimaryDark,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF292929),
    onSecondaryContainer = TextPrimaryDark,
    tertiary = TextSecondaryDark,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurfacePanel,
    surfaceTint = Color.Transparent,
    surfaceContainer = DarkSurfacePanel,
    surfaceContainerHigh = DarkSurfaceElevated,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = TextMutedDark,
    outlineVariant = DarkBorder,
    error = DangerRed,
    onError = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFF4E7CE),
    onPrimaryContainer = LightPrimary,
    secondary = TextSecondaryLight,
    onSecondary = LightSurfacePanel,
    secondaryContainer = Color(0xFFEDEAE4),
    onSecondaryContainer = TextPrimaryLight,
    tertiary = TextSecondaryLight,
    onTertiary = LightSurfacePanel,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurfacePanel,
    surfaceTint = Color.Transparent,
    surfaceContainer = LightSurfacePanel,
    surfaceContainerHigh = LightSurfaceElevated,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    outlineVariant = LightBorderSubtle,
    error = Color(0xFFB4232A),
    onError = LightSurfacePanel
)

private val SystemColorScheme = DarkColorScheme.copy(
    primary = Color(0xFF3B5E96),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E4E7D),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF3D587E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2F3E5B),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF365B83),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF294462),
    onTertiaryContainer = Color.White,
    background = Color(0xFF11161E),
    onBackground = Color.White,
    surface = Color(0xFF1B2635),
    onSurface = Color.White,
    surfaceDim = Color(0xFF11161E),
    surfaceBright = Color(0xFF293B52),
    surfaceContainerLowest = Color(0xFF101317),
    surfaceContainerLow = Color(0xFF161F2C),
    surfaceContainer = Color(0xFF1B2635),
    surfaceContainerHigh = Color(0xFF22334B),
    surfaceContainerHighest = Color(0xFF293B52),
    surfaceVariant = Color(0xFF22334B),
    onSurfaceVariant = Color(0xFFF1F4F8),
    outline = Color(0xFF91A5BD),
    outlineVariant = Color(0xFF3E506A),
    inverseSurface = Color(0xFF2E4E7D),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFD9E7FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5D1715),
    errorContainer = Color(0xFF702824),
    onErrorContainer = Color.White
)

internal val ColorScheme.accentForeground: Color
    @Composable get() = if (LocalGymVipSystemTheme.current) onSurface else primary

@Composable
internal fun vipOutlinedTextFieldColors(): TextFieldColors {
    val foreground = MaterialTheme.colorScheme.accentForeground
    return OutlinedTextFieldDefaults.colors(
        cursorColor = foreground,
        focusedBorderColor = foreground,
        focusedLabelColor = foreground
    )
}

@Composable
fun GymVipTheme(
    themePreference: String = "SYSTEM", // SYSTEM, DARK, LIGHT
    onDarkThemeChanged: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themePreference) {
        "DARK" -> DarkColorScheme
        "LIGHT" -> LightColorScheme
        else -> SystemColorScheme
    }
    val followsSystem = themePreference != "DARK" && themePreference != "LIGHT"
    SideEffect { onDarkThemeChanged?.invoke(themePreference != "LIGHT") }
    CompositionLocalProvider(LocalGymVipSystemTheme provides followsSystem) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VipTypography,
            shapes = VipShapes,
            content = content
        )
    }
}

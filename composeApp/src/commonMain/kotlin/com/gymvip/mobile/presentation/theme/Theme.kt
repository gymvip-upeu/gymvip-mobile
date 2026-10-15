package com.gymvip.mobile.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
    error = DangerRed,
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
    error = DangerRed,
    onError = LightSurfacePanel
)

@Composable
fun GymVipTheme(
    themePreference: String = "SYSTEM", // SYSTEM, DARK, LIGHT
    content: @Composable () -> Unit
) {
    val isDark = when (themePreference) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VipTypography,
        shapes = VipShapes,
        content = content
    )
}

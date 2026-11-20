package com.gymvip.mobile.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

internal val LocalGymVipSystemTheme = staticCompositionLocalOf { false }
val LocalReduceMotion = staticCompositionLocalOf { false }

// ==========================================
// TEMA AZUL OSCURO OFICIAL (VIP CENTER FIT)
// ==========================================
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


private val DarkBlueColorScheme = darkColorScheme(
    primary = BlueGymDarkPrimary,
    onPrimary = BlueGymDarkOnPrimary,
    primaryContainer = Color(0xFF1E3E62),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF38BDF8),
    onSecondary = BlueGymDarkBackground,
    secondaryContainer = Color(0xFF1E2D44),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF94A3B8),
    onTertiary = BlueGymDarkBackground,
    background = BlueGymDarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = BlueGymDarkSurface,
    surfaceTint = Color.Transparent,
    surfaceContainer = BlueGymDarkSurface,
    surfaceContainerHigh = BlueGymDarkSurfaceElevated,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = BlueGymDarkSurfaceElevated,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF64748B),
    outlineVariant = BlueGymDarkBorder,
    error = DangerRed,
    onError = Color.White
)

// ==========================================
// TEMA AZUL CLARO OFICIAL
// ==========================================
private val LightBlueColorScheme = lightColorScheme(
    primary = BlueGymLightPrimary,
    onPrimary = BlueGymLightOnPrimary,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF475569),
    onTertiary = Color.White,
    background = BlueGymLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = BlueGymLightSurface,
    surfaceTint = Color.Transparent,
    surfaceContainer = BlueGymLightSurface,
    surfaceContainerHigh = BlueGymLightSurfaceElevated,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = BlueGymLightSurfaceElevated,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = BlueGymLightBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

// ==========================================
// TEMA DARK & GOLD LUXURY (ALTERNATIVA)
// ==========================================
private val DarkGoldColorScheme = darkColorScheme(
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

internal val ColorScheme.accentForeground: Color
    @Composable get() = if (LocalGymVipSystemTheme.current) primary else primary

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
    themePreference: String = "SYSTEM", // SYSTEM, DARK, LIGHT, DARK_GOLD
    fontScale: Float = 1.0f,
    highContrast: Boolean = false,
    reduceMotion: Boolean = false,
    onDarkThemeChanged: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()

    var baseColorScheme = when (themePreference) {
        "DARK" -> DarkColorScheme
        "LIGHT" -> LightColorScheme
        "DARK_GOLD" -> DarkGoldColorScheme
        "BLUE_DARK" -> DarkBlueColorScheme
        "BLUE_LIGHT" -> LightBlueColorScheme
        else -> SystemColorScheme
    }

    // Modo Alto Contraste para máxima legibilidad
    if (highContrast) {
        val isDark = themePreference != "LIGHT" && themePreference != "BLUE_LIGHT"
        baseColorScheme = if (isDark) {
            baseColorScheme.copy(
                background = Color(0xFF000000),
                surface = Color(0xFF0A0A0A),
                surfaceVariant = Color(0xFF141414),
                onBackground = Color(0xFFFFFFFF),
                onSurface = Color(0xFFFFFFFF),
                outline = Color(0xFFE2E8F0),
                outlineVariant = Color(0xFFCBD5E1)
            )
        } else {
            baseColorScheme.copy(
                background = Color(0xFFFFFFFF),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFF1F5F9),
                onBackground = Color(0xFF000000),
                onSurface = Color(0xFF000000),
                outline = Color(0xFF0F172A),
                outlineVariant = Color(0xFF334155)
            )
        }
    }

    val isDarkThemeActive = themePreference != "LIGHT" && themePreference != "BLUE_LIGHT"
    SideEffect { onDarkThemeChanged?.invoke(isDarkThemeActive) }

    // Escalado de tipografía respetando preferencias de accesibilidad
    val scaledTypography = rememberScaledTypography(fontScale)

    CompositionLocalProvider(
        LocalGymVipSystemTheme provides (themePreference == "SYSTEM"),
        LocalReduceMotion provides reduceMotion
    ) {
        MaterialTheme(
            colorScheme = baseColorScheme,
            typography = scaledTypography,
            shapes = VipShapes,
            content = content
        )
    }
}

@Composable
private fun rememberScaledTypography(scale: Float): Typography {
    if (scale == 1.0f) return VipTypography
    fun scaleStyle(style: TextStyle): TextStyle {
        return style.copy(fontSize = (style.fontSize.value * scale).sp)
    }
    return Typography(
        displayLarge = scaleStyle(VipTypography.displayLarge),
        displayMedium = scaleStyle(VipTypography.displayMedium),
        displaySmall = scaleStyle(VipTypography.displaySmall),
        headlineLarge = scaleStyle(VipTypography.headlineLarge),
        headlineMedium = scaleStyle(VipTypography.headlineMedium),
        headlineSmall = scaleStyle(VipTypography.headlineSmall),
        titleLarge = scaleStyle(VipTypography.titleLarge),
        titleMedium = scaleStyle(VipTypography.titleMedium),
        titleSmall = scaleStyle(VipTypography.titleSmall),
        bodyLarge = scaleStyle(VipTypography.bodyLarge),
        bodyMedium = scaleStyle(VipTypography.bodyMedium),
        bodySmall = scaleStyle(VipTypography.bodySmall),
        labelLarge = scaleStyle(VipTypography.labelLarge),
        labelMedium = scaleStyle(VipTypography.labelMedium),
        labelSmall = scaleStyle(VipTypography.labelSmall)
    )
}

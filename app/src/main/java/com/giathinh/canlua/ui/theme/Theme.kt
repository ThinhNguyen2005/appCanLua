package com.giathinh.canlua.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import com.giathinh.canlua.data.model.AppThemeMode
import com.giathinh.canlua.data.model.AppUiMode
import com.giathinh.canlua.data.model.FontScale
import com.giathinh.canlua.ui.util.LocalFontScaleFactor

private val DarkHighContrastColorScheme = darkColorScheme(
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = Amber80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFB0B0B0),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF121212),
)

private val DarkOledColorScheme = darkColorScheme(
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = Amber80,
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF121212),
    surfaceContainer = Color(0xFF0F0F0F),
    onBackground = Color(0xFFE8E8E8),
    onSurface = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFFB0B0B0),
    inverseSurface = Color(0xFFE8E8E8),
    inverseOnSurface = Color(0xFF000000),
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = GreenGrey40,
    tertiary = Amber40,
    background = SurfaceGreen,
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8F5E9),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1C1A),
    onSurface = Color(0xFF1A1C1A),
    onSurfaceVariant = Color(0xFF424940)
)

private val LightColorSchemeNoDynamic = lightColorScheme(
    primary = Green40,
    secondary = GreenGrey40,
    tertiary = Amber40,
    background = SurfaceGreen,
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8F5E9),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1C1A),
    onSurface = Color(0xFF1A1C1A),
    onSurfaceVariant = Color(0xFF424940),
    inverseSurface = Color(0xFF1A1C1A),
    inverseOnSurface = Color(0xFFF1F8E9)
)

private val DarkHighContrastColorSchemeNoDynamic = darkColorScheme(
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = Amber80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onPrimary = Color(0xFF003300),
    onSecondary = Color(0xFF003300),
    onTertiary = Color(0xFF4A3000),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFB0B0B0),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF121212)
)

private val DarkOledColorSchemeNoDynamic = darkColorScheme(
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = Amber80,
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF121212),
    surfaceContainer = Color(0xFF0F0F0F),
    onPrimary = Color(0xFF003300),
    onSecondary = Color(0xFF003300),
    onTertiary = Color(0xFF4A3000),
    onBackground = Color(0xFFE8E8E8),
    onSurface = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFFB0B0B0),
    inverseSurface = Color(0xFFE8E8E8),
    inverseOnSurface = Color(0xFF000000),
)

internal val LocalAppThemeMode = staticCompositionLocalOf { AppThemeMode.AUTO }
internal val LocalAppUiMode = staticCompositionLocalOf { AppUiMode.STANDARD }

@Composable
fun CanLuaTheme(
    appThemeMode: AppThemeMode = AppThemeMode.AUTO,
    fontScale: FontScale = FontScale.NORMAL,
    uiMode: AppUiMode = AppUiMode.STANDARD,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    val baseColorScheme = when (appThemeMode) {
        AppThemeMode.LIGHT -> {
            if (isDynamicColorAvailable) dynamicLightColorScheme(context) else LightColorSchemeNoDynamic
        }

        AppThemeMode.HIGH_CONTRAST -> {
            if (isDynamicColorAvailable) dynamicDarkColorScheme(context) else DarkHighContrastColorSchemeNoDynamic
        }

        AppThemeMode.OLED -> {
            // OLED intentionally ignores dynamic color — we NEED pure #000000 background
            // for true AMOLED black and maximum power saving. The wallpaper-extracted
            // dynamic colors are "dark grey" at best, defeating the entire purpose.
            DarkOledColorSchemeNoDynamic
        }

        AppThemeMode.AUTO -> {
            if (isDynamicColorAvailable) {
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemDark) DarkHighContrastColorSchemeNoDynamic else LightColorSchemeNoDynamic
            }
        }
    }

    val scaledTypography = Typography.scale(fontScale.scale)

    CompositionLocalProvider(
        LocalFontScaleFactor provides fontScale.scale,
        LocalAppThemeMode provides appThemeMode,
        LocalAppUiMode provides uiMode,
    ) {
        MaterialTheme(
            colorScheme = baseColorScheme,
            typography = scaledTypography,
            content = content
        )
    }
}

private fun TextStyle.scale(factor: Float): TextStyle {
    fun TextUnit.scaleOrKeep() = if (isUnspecified) this else this * factor
    return copy(
        fontSize = fontSize.scaleOrKeep(),
        lineHeight = lineHeight.scaleOrKeep(),
        letterSpacing = letterSpacing // keep spacing
    )
}

private fun androidx.compose.material3.Typography.scale(factor: Float): androidx.compose.material3.Typography {
    return androidx.compose.material3.Typography(
        displayLarge = displayLarge.scale(factor),
        displayMedium = displayMedium.scale(factor),
        displaySmall = displaySmall.scale(factor),
        headlineLarge = headlineLarge.scale(factor),
        headlineMedium = headlineMedium.scale(factor),
        headlineSmall = headlineSmall.scale(factor),
        titleLarge = titleLarge.scale(factor),
        titleMedium = titleMedium.scale(factor),
        titleSmall = titleSmall.scale(factor),
        bodyLarge = bodyLarge.scale(factor),
        bodyMedium = bodyMedium.scale(factor),
        bodySmall = bodySmall.scale(factor),
        labelLarge = labelLarge.scale(factor),
        labelMedium = labelMedium.scale(factor),
        labelSmall = labelSmall.scale(factor)
    )
}
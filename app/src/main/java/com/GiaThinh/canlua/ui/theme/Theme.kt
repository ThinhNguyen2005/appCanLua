package com.GiaThinh.canlua.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.times
import com.GiaThinh.canlua.data.model.FontScale

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = Amber80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C)
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = GreenGrey40,
    tertiary = Amber40,
    background = SurfaceGreen,
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8F5E9), // Lighter green for variant surface to improve contrast
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1C1A), // Near black with green tint for high contrast text
    onSurface = Color(0xFF1A1C1A), // Near black with green tint
    onSurfaceVariant = Color(0xFF424940) // Dark gray with green tint for secondary text
)

@Composable
fun CanLuaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    fontScale: FontScale = FontScale.NORMAL,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Thêm animation chuyển đổi mượt mà giữa sáng/tối
    val colorScheme = baseColorScheme.copy(
        primary = androidx.compose.animation.animateColorAsState(baseColorScheme.primary, label = "primary").value,
        onPrimary = androidx.compose.animation.animateColorAsState(baseColorScheme.onPrimary, label = "onPrimary").value,
        primaryContainer = androidx.compose.animation.animateColorAsState(baseColorScheme.primaryContainer, label = "primaryContainer").value,
        onPrimaryContainer = androidx.compose.animation.animateColorAsState(baseColorScheme.onPrimaryContainer, label = "onPrimaryContainer").value,
        inversePrimary = androidx.compose.animation.animateColorAsState(baseColorScheme.inversePrimary, label = "inversePrimary").value,
        secondary = androidx.compose.animation.animateColorAsState(baseColorScheme.secondary, label = "secondary").value,
        onSecondary = androidx.compose.animation.animateColorAsState(baseColorScheme.onSecondary, label = "onSecondary").value,
        secondaryContainer = androidx.compose.animation.animateColorAsState(baseColorScheme.secondaryContainer, label = "secondaryContainer").value,
        onSecondaryContainer = androidx.compose.animation.animateColorAsState(baseColorScheme.onSecondaryContainer, label = "onSecondaryContainer").value,
        tertiary = androidx.compose.animation.animateColorAsState(baseColorScheme.tertiary, label = "tertiary").value,
        onTertiary = androidx.compose.animation.animateColorAsState(baseColorScheme.onTertiary, label = "onTertiary").value,
        tertiaryContainer = androidx.compose.animation.animateColorAsState(baseColorScheme.tertiaryContainer, label = "tertiaryContainer").value,
        onTertiaryContainer = androidx.compose.animation.animateColorAsState(baseColorScheme.onTertiaryContainer, label = "onTertiaryContainer").value,
        background = androidx.compose.animation.animateColorAsState(baseColorScheme.background, label = "background").value,
        onBackground = androidx.compose.animation.animateColorAsState(baseColorScheme.onBackground, label = "onBackground").value,
        surface = androidx.compose.animation.animateColorAsState(baseColorScheme.surface, label = "surface").value,
        onSurface = androidx.compose.animation.animateColorAsState(baseColorScheme.onSurface, label = "onSurface").value,
        surfaceVariant = androidx.compose.animation.animateColorAsState(baseColorScheme.surfaceVariant, label = "surfaceVariant").value,
        onSurfaceVariant = androidx.compose.animation.animateColorAsState(baseColorScheme.onSurfaceVariant, label = "onSurfaceVariant").value,
        surfaceTint = androidx.compose.animation.animateColorAsState(baseColorScheme.surfaceTint, label = "surfaceTint").value,
        inverseSurface = androidx.compose.animation.animateColorAsState(baseColorScheme.inverseSurface, label = "inverseSurface").value,
        inverseOnSurface = androidx.compose.animation.animateColorAsState(baseColorScheme.inverseOnSurface, label = "inverseOnSurface").value,
        error = androidx.compose.animation.animateColorAsState(baseColorScheme.error, label = "error").value,
        onError = androidx.compose.animation.animateColorAsState(baseColorScheme.onError, label = "onError").value,
        errorContainer = androidx.compose.animation.animateColorAsState(baseColorScheme.errorContainer, label = "errorContainer").value,
        onErrorContainer = androidx.compose.animation.animateColorAsState(baseColorScheme.onErrorContainer, label = "onErrorContainer").value,
        outline = androidx.compose.animation.animateColorAsState(baseColorScheme.outline, label = "outline").value,
        outlineVariant = androidx.compose.animation.animateColorAsState(baseColorScheme.outlineVariant, label = "outlineVariant").value,
        scrim = androidx.compose.animation.animateColorAsState(baseColorScheme.scrim, label = "scrim").value,
        surfaceBright = androidx.compose.animation.animateColorAsState(baseColorScheme.surfaceBright, label = "surfaceBright").value,
        surfaceDim = androidx.compose.animation.animateColorAsState(baseColorScheme.surfaceDim, label = "surfaceDim").value,
        surfaceContainer = androidx.compose.animation.animateColorAsState(baseColorScheme.surfaceContainer, label = "surfaceContainer").value,
        surfaceContainerHigh = androidx.compose.animation.animateColorAsState(baseColorScheme.surfaceContainerHigh, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = androidx.compose.animation.animateColorAsState(baseColorScheme.surfaceContainerHighest, label = "surfaceContainerHighest").value,
        surfaceContainerLow = androidx.compose.animation.animateColorAsState(baseColorScheme.surfaceContainerLow, label = "surfaceContainerLow").value,
        surfaceContainerLowest = androidx.compose.animation.animateColorAsState(baseColorScheme.surfaceContainerLowest, label = "surfaceContainerLowest").value
    )

    val scaledTypography = Typography.scale(fontScale.scale)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content
    )
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
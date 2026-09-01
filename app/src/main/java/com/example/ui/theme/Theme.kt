package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = ZenSlateBlueLight,
    onPrimary = ZenSlateBlueDark,
    primaryContainer = DarkZenMistContainer,
    onPrimaryContainer = ZenMistContainer,
    secondary = ZenSlateBlueLight,
    onSecondary = ZenSlateBlueDark,
    secondaryContainer = DarkZenMistContainer,
    onSecondaryContainer = ZenMistContainer,
    tertiary = ZenAccentBlue,
    onTertiary = Color.White,
    background = DarkZenCanvasBackground,
    surface = DarkZenCanvasBackground,
    surfaceVariant = DarkZenCardBackground,
    onSurface = DarkZenTextPrimary,
    onSurfaceVariant = DarkZenTextSecondary,
    outline = DarkZenBorder,
    outlineVariant = DarkZenBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ZenSlateBlue,
    onPrimary = Color.White,
    primaryContainer = ZenMistContainer,
    onPrimaryContainer = ZenMistContainerText,
    secondary = ZenAccentBlue,
    onSecondary = Color.White,
    secondaryContainer = ZenBannerBg,
    onSecondaryContainer = ZenSlateBlue,
    tertiary = ZenSlateBlue,
    onTertiary = Color.White,
    background = ZenCanvasBackground,
    surface = ZenCanvasBackground,
    surfaceVariant = ZenFooterBg,
    onSurface = ZenTextPrimary,
    onSurfaceVariant = ZenTextSecondary,
    outline = ZenBorder,
    outlineVariant = ZenBorder
)

@Composable
fun SplitSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

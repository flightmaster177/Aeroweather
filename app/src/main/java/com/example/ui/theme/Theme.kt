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
    primary = AeroBluePrimaryDark,
    onPrimary = AeroBlueOnPrimaryDark,
    primaryContainer = AeroBlueContainerDark,
    onPrimaryContainer = AeroBlueOnContainerDark,
    secondary = AeroSkySecondaryDark,
    onSecondary = Color(0xFF23323F),
    secondaryContainer = AeroSkyContainerDark,
    onSecondaryContainer = Color(0xFFD5E4F6),
    background = Color(0xFF101923),
    onBackground = Color(0xFFE1E8F0),
    surface = Color(0xFF16202C),
    onSurface = Color(0xFFE1E8F0),
    surfaceVariant = Color(0xFF1E2E42),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = AeroBluePrimary,
    onPrimary = AeroBlueOnPrimary,
    primaryContainer = AeroBlueContainer,
    onPrimaryContainer = AeroBlueOnContainer,
    secondary = AeroSkySecondary,
    onSecondary = Color.White,
    secondaryContainer = AeroSkyContainer,
    onSecondaryContainer = Color(0xFF0D1D2A),
    background = Color(0xFFEDF4FA),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE1F0FF),
    onSurfaceVariant = Color(0xFF41474D)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false so AeroWeather's signature aeronautical theme is preserved
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

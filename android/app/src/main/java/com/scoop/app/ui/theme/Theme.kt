package com.scoop.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors =
    lightColorScheme(
        primary = ScoopTealLight,
        background = ScoopBackgroundLight,
        surface = ScoopSurfaceLight,
        onBackground = ScoopOnSurfaceLight,
        onSurface = ScoopOnSurfaceLight,
    )

private val DarkColors =
    darkColorScheme(
        primary = ScoopTealDark,
        background = ScoopBackgroundDark,
        surface = ScoopSurfaceDark,
        onBackground = ScoopOnSurfaceDark,
        onSurface = ScoopOnSurfaceDark,
    )

@Composable
fun ScoopTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        when {
            useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            useDarkTheme -> DarkColors
            else -> LightColors
        }

    MaterialTheme(colorScheme = colorScheme, typography = ScoopTypography, content = content)
}

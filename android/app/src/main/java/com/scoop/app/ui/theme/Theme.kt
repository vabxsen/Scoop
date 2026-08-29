package com.scoop.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.scoop.app.core.model.AccentPalette
import com.scoop.app.core.model.ThemeMode

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun lightSchemeFor(accent: AccentColors) =
    lightColorScheme(
        primary = accent.primaryLight,
        onPrimary = accent.onPrimaryLight,
        primaryContainer = accent.primaryContainerLight,
        onPrimaryContainer = accent.onPrimaryContainerLight,
        secondary = accent.secondaryLight,
        onSecondary = accent.onSecondaryLight,
        secondaryContainer = accent.secondaryContainerLight,
        onSecondaryContainer = accent.onSecondaryContainerLight,
        tertiary = accent.tertiaryLight,
        onTertiary = accent.onTertiaryLight,
        tertiaryContainer = accent.tertiaryContainerLight,
        onTertiaryContainer = accent.onTertiaryContainerLight,
        error = ErrorLight,
        onError = OnErrorLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
        background = BackgroundLight,
        onBackground = OnBackgroundLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        surfaceContainerLow = SurfaceContainerLowLight,
        surfaceContainer = SurfaceContainerLight,
        surfaceContainerHigh = SurfaceContainerHighLight,
        outline = OutlineLight,
        outlineVariant = OutlineVariantLight,
    )

private fun darkSchemeFor(accent: AccentColors) =
    darkColorScheme(
        primary = accent.primaryDark,
        onPrimary = accent.onPrimaryDark,
        primaryContainer = accent.primaryContainerDark,
        onPrimaryContainer = accent.onPrimaryContainerDark,
        secondary = accent.secondaryDark,
        onSecondary = accent.onSecondaryDark,
        secondaryContainer = accent.secondaryContainerDark,
        onSecondaryContainer = accent.onSecondaryContainerDark,
        tertiary = accent.tertiaryDark,
        onTertiary = accent.onTertiaryDark,
        tertiaryContainer = accent.tertiaryContainerDark,
        onTertiaryContainer = accent.onTertiaryContainerDark,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        surfaceContainerLow = SurfaceContainerLowDark,
        surfaceContainer = SurfaceContainerDark,
        surfaceContainerHigh = SurfaceContainerHighDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
    )

@Composable
fun ScoopTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentPalette: AccentPalette = AccentPalette.MONOCHROME,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    val context = LocalContext.current
    val colorScheme: ColorScheme =
        when {
            useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            useDarkTheme -> darkSchemeFor(accentPalette.colors())
            else -> lightSchemeFor(accentPalette.colors())
        }

    // enableEdgeToEdge() only sets the status/nav bar icon color once, based on the *system's*
    // dark/light state at Activity creation - it knows nothing about ThemeMode.LIGHT/DARK
    // overriding that. Following useDarkTheme here instead keeps the icons legible against
    // whatever background the app is actually showing, independent of the system setting, and
    // keeps them in sync if the user changes the in-app theme without restarting the app.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = ScoopTypography, shapes = ScoopShapes, content = content)
}

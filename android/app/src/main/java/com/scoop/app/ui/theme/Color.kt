package com.scoop.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.scoop.app.core.model.AccentPalette

// Scoop's foundation is a fixed two-tone base: every background/surface/outline role below is a
// tint or shade blended between these two anchors, regardless of which accent palette is active.
private val Ink = Color(0xFF1F1E20)
private val Cream = Color(0xFFFDF8F4)

/** One accent palette's primary/secondary/tertiary roles, light and dark. Background, surface,
 * and error roles are shared across every palette — only these three role families change. */
data class AccentColors(
    val primaryLight: Color,
    val onPrimaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val primaryDark: Color,
    val onPrimaryDark: Color,
    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
    val secondaryLight: Color,
    val onSecondaryLight: Color,
    val secondaryContainerLight: Color,
    val onSecondaryContainerLight: Color,
    val secondaryDark: Color,
    val onSecondaryDark: Color,
    val secondaryContainerDark: Color,
    val onSecondaryContainerDark: Color,
    val tertiaryLight: Color,
    val onTertiaryLight: Color,
    val tertiaryContainerLight: Color,
    val onTertiaryContainerLight: Color,
    val tertiaryDark: Color,
    val onTertiaryDark: Color,
    val tertiaryContainerDark: Color,
    val onTertiaryContainerDark: Color,
)

// Monochrome — strictly ink and cream, full stop. Primary, secondary, and tertiary all reuse the
// same two anchors (no third gray tone anywhere) so every accent-colored element — FABs, chips,
// selected states — renders as pure black-on-cream in light mode, cream-on-black in dark.
private val MonochromeAccent =
    AccentColors(
        primaryLight = Ink,
        onPrimaryLight = Cream,
        primaryContainerLight = Ink,
        onPrimaryContainerLight = Cream,
        primaryDark = Cream,
        onPrimaryDark = Ink,
        primaryContainerDark = Cream,
        onPrimaryContainerDark = Ink,
        secondaryLight = Ink,
        onSecondaryLight = Cream,
        secondaryContainerLight = Ink,
        onSecondaryContainerLight = Cream,
        secondaryDark = Cream,
        onSecondaryDark = Ink,
        secondaryContainerDark = Cream,
        onSecondaryContainerDark = Ink,
        tertiaryLight = Ink,
        onTertiaryLight = Cream,
        tertiaryContainerLight = Ink,
        onTertiaryContainerLight = Cream,
        tertiaryDark = Cream,
        onTertiaryDark = Ink,
        tertiaryContainerDark = Cream,
        onTertiaryContainerDark = Ink,
    )

// Ocean — teal/blue-green, Scoop's original pre-dual-tone accent.
private val OceanAccent =
    AccentColors(
        primaryLight = Color(0xFF146C6B),
        onPrimaryLight = Color(0xFFFFFFFF),
        primaryContainerLight = Color(0xFFAAF0EA),
        onPrimaryContainerLight = Color(0xFF00201F),
        primaryDark = Color(0xFF82D5D1),
        onPrimaryDark = Color(0xFF003736),
        primaryContainerDark = Color(0xFF00504E),
        onPrimaryContainerDark = Color(0xFFAAF0EA),
        secondaryLight = Color(0xFF4A6360),
        onSecondaryLight = Color(0xFFFFFFFF),
        secondaryContainerLight = Color(0xFFCCE8E3),
        onSecondaryContainerLight = Color(0xFF06201D),
        secondaryDark = Color(0xFFB0CCC7),
        onSecondaryDark = Color(0xFF1B3532),
        secondaryContainerDark = Color(0xFF334B48),
        onSecondaryContainerDark = Color(0xFFCCE8E3),
        tertiaryLight = Color(0xFF6B5B2E),
        onTertiaryLight = Color(0xFFFFFFFF),
        tertiaryContainerLight = Color(0xFFF4DFA6),
        onTertiaryContainerLight = Color(0xFF231A00),
        tertiaryDark = Color(0xFFD7C38C),
        onTertiaryDark = Color(0xFF3A2E04),
        tertiaryContainerDark = Color(0xFF524319),
        onTertiaryContainerDark = Color(0xFFF4DFA6),
    )

// Forest — muted green with an olive/gold tertiary.
private val ForestAccent =
    AccentColors(
        primaryLight = Color(0xFF2E6B4F),
        onPrimaryLight = Color(0xFFFFFFFF),
        primaryContainerLight = Color(0xFFB2F1C9),
        onPrimaryContainerLight = Color(0xFF00210F),
        primaryDark = Color(0xFF97D4B1),
        onPrimaryDark = Color(0xFF00391E),
        primaryContainerDark = Color(0xFF0F5132),
        onPrimaryContainerDark = Color(0xFFB2F1C9),
        secondaryLight = Color(0xFF4E6355),
        onSecondaryLight = Color(0xFFFFFFFF),
        secondaryContainerLight = Color(0xFFD0E8D6),
        onSecondaryContainerLight = Color(0xFF0B1F13),
        secondaryDark = Color(0xFFB4CCBB),
        onSecondaryDark = Color(0xFF213528),
        secondaryContainerDark = Color(0xFF374B3D),
        onSecondaryContainerDark = Color(0xFFD0E8D6),
        tertiaryLight = Color(0xFF6E5D2E),
        onTertiaryLight = Color(0xFFFFFFFF),
        tertiaryContainerLight = Color(0xFFF7E1A6),
        onTertiaryContainerLight = Color(0xFF241A00),
        tertiaryDark = Color(0xFFDAC58D),
        onTertiaryDark = Color(0xFF3C2F04),
        tertiaryContainerDark = Color(0xFF554419),
        onTertiaryContainerDark = Color(0xFFF7E1A6),
    )

// Sunset — burnt orange with an olive-gold tertiary.
private val SunsetAccent =
    AccentColors(
        primaryLight = Color(0xFF9A4B1E),
        onPrimaryLight = Color(0xFFFFFFFF),
        primaryContainerLight = Color(0xFFFFDBC7),
        onPrimaryContainerLight = Color(0xFF351100),
        primaryDark = Color(0xFFFFB68F),
        onPrimaryDark = Color(0xFF582200),
        primaryContainerDark = Color(0xFF793300),
        onPrimaryContainerDark = Color(0xFFFFDBC7),
        secondaryLight = Color(0xFF77574B),
        onSecondaryLight = Color(0xFFFFFFFF),
        secondaryContainerLight = Color(0xFFFFDBCB),
        onSecondaryContainerLight = Color(0xFF2C160C),
        secondaryDark = Color(0xFFE7BDAF),
        onSecondaryDark = Color(0xFF44291F),
        secondaryContainerDark = Color(0xFF5D3F34),
        onSecondaryContainerDark = Color(0xFFFFDBCB),
        tertiaryLight = Color(0xFF63601A),
        onTertiaryLight = Color(0xFFFFFFFF),
        tertiaryContainerLight = Color(0xFFEBE68E),
        onTertiaryContainerLight = Color(0xFF1D1D00),
        tertiaryDark = Color(0xFFCECA79),
        onTertiaryDark = Color(0xFF333200),
        tertiaryContainerDark = Color(0xFF4A4900),
        onTertiaryContainerDark = Color(0xFFEBE68E),
    )

// Lavender — soft purple with a mauve/pink tertiary.
private val LavenderAccent =
    AccentColors(
        primaryLight = Color(0xFF6E5296),
        onPrimaryLight = Color(0xFFFFFFFF),
        primaryContainerLight = Color(0xFFEADDFF),
        onPrimaryContainerLight = Color(0xFF25005A),
        primaryDark = Color(0xFFD4BBFF),
        onPrimaryDark = Color(0xFF3D1F72),
        primaryContainerDark = Color(0xFF553A89),
        onPrimaryContainerDark = Color(0xFFEADDFF),
        secondaryLight = Color(0xFF635A6F),
        onSecondaryLight = Color(0xFFFFFFFF),
        secondaryContainerLight = Color(0xFFE9DEF8),
        onSecondaryContainerLight = Color(0xFF1F182A),
        secondaryDark = Color(0xFFCCC1DC),
        onSecondaryDark = Color(0xFF342C40),
        secondaryContainerDark = Color(0xFF4B4358),
        onSecondaryContainerDark = Color(0xFFE9DEF8),
        tertiaryLight = Color(0xFF7D5261),
        onTertiaryLight = Color(0xFFFFFFFF),
        tertiaryContainerLight = Color(0xFFFFD9E3),
        onTertiaryContainerLight = Color(0xFF31101C),
        tertiaryDark = Color(0xFFEFB8C8),
        onTertiaryDark = Color(0xFF4A2530),
        tertiaryContainerDark = Color(0xFF633B47),
        onTertiaryContainerDark = Color(0xFFFFD9E3),
    )

fun AccentPalette.colors(): AccentColors =
    when (this) {
        AccentPalette.MONOCHROME -> MonochromeAccent
        AccentPalette.OCEAN -> OceanAccent
        AccentPalette.FOREST -> ForestAccent
        AccentPalette.SUNSET -> SunsetAccent
        AccentPalette.LAVENDER -> LavenderAccent
    }

/** The two swatch colors shown for this palette in the picker (top half, bottom half). Monochrome
 * shows the actual ink/cream anchor pair rather than primary+tertiary, since that pairing *is*
 * the dual-tone look, not just an accent choice. */
fun AccentPalette.swatchColors(): Pair<Color, Color> {
    if (this == AccentPalette.MONOCHROME) return Ink to Cream
    val accent = colors()
    return accent.primaryLight to accent.tertiaryLight
}

// Error — always red, regardless of accent palette, so failed/error states stay spottable.
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Neutrals — background/surface are the two anchors directly; every container/outline step is a
// blend between them. Shared across every accent palette.
val BackgroundLight = Cream
val OnBackgroundLight = Ink
val SurfaceLight = Cream
val OnSurfaceLight = Ink
val SurfaceVariantLight = Color(0xFFE2DEDB)
val OnSurfaceVariantLight = Color(0xFF575553)
val SurfaceContainerLowLight = Color(0xFFF2EDE9)
val SurfaceContainerLight = Color(0xFFEBE7E3)
val SurfaceContainerHighLight = Color(0xFFE0DCD8)
val OutlineLight = Color(0xFF83807F)
val OutlineVariantLight = Color(0xFFBAB7B4)

val BackgroundDark = Ink
val OnBackgroundDark = Cream
val SurfaceDark = Ink
val OnSurfaceDark = Cream
val SurfaceVariantDark = Color(0xFF3A3839)
val OnSurfaceVariantDark = Color(0xFFD1CCCA)
val SurfaceContainerLowDark = Color(0xFF2A292B)
val SurfaceContainerDark = Color(0xFF333233)
val SurfaceContainerHighDark = Color(0xFF3E3D3E)
val OutlineDark = Color(0xFF999695)
val OutlineVariantDark = Color(0xFF625F60)

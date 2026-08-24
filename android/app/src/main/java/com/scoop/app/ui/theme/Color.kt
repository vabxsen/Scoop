package com.scoop.app.ui.theme

import androidx.compose.ui.graphics.Color

// Scoop is a strict two-tone UI: every color below (except error) is a tint or shade blended
// between these two anchors — no third hue is introduced anywhere in the app.
private val Ink = Color(0xFF1F1E20)
private val Cream = Color(0xFFFDF8F4)

// Primary — in light mode, ink-on-cream (dark button on a light field); in dark mode, the
// inverse (cream-on-ink). This is the only "accent"; it's just the other anchor tone.
val PrimaryLight = Ink
val OnPrimaryLight = Cream
val PrimaryContainerLight = Color(0xFFD1CCCA)
val OnPrimaryContainerLight = Ink

val PrimaryDark = Cream
val OnPrimaryDark = Ink
val PrimaryContainerDark = Color(0xFF504E4F)
val OnPrimaryContainerDark = Cream

// Secondary — a mid gray step between the two anchors, for chips and outlined buttons.
val SecondaryLight = Color(0xFF787575)
val OnSecondaryLight = Cream
val SecondaryContainerLight = Color(0xFFDCD7D4)
val OnSecondaryContainerLight = Ink

val SecondaryDark = Color(0xFFA4A19F)
val OnSecondaryDark = Ink
val SecondaryContainerDark = Color(0xFF403F40)
val OnSecondaryContainerDark = Cream

// Tertiary — a slightly lighter gray step than secondary, for audio-format badges and
// non-error highlights. Distinguished from secondary by weight, not hue.
val TertiaryLight = Color(0xFF8E8B8A)
val OnTertiaryLight = Cream
val TertiaryContainerLight = Color(0xFFCCC8C5)
val OnTertiaryContainerLight = Ink

val TertiaryDark = Color(0xFF8E8B8A)
val OnTertiaryDark = Ink
val TertiaryContainerDark = Color(0xFF595757)
val OnTertiaryContainerDark = Cream

// Error — the one deliberate exception: failed/error states keep a true red so they stay
// spottable at a glance in a list, everything else in the app is built from Ink/Cream alone.
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Neutrals — background/surface are the two anchors directly; every container/outline step is
// a blend between them.
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

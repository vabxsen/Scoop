package com.scoop.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.scoop.app.R

// Onest — a rounded geometric sans (OFL-licensed, bundled locally) chosen to match the softer,
// rounded-terminal look of the reference UI rather than the system default (Roboto).
private val baseFont =
    FontFamily(
        Font(R.font.onest_regular, FontWeight.Normal),
        Font(R.font.onest_medium, FontWeight.Medium),
        Font(R.font.onest_semibold, FontWeight.SemiBold),
        Font(R.font.onest_bold, FontWeight.Bold),
    )

/** A deliberate type scale so hierarchy stays consistent app-wide. */
val ScoopTypography =
    Typography(
        displayLarge = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
        displaySmall = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 38.sp),
        headlineLarge = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
        headlineMedium = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
        headlineSmall = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleLarge = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
        titleSmall = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
        bodyMedium = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.15.sp),
        bodySmall = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
        labelLarge = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
        labelSmall = TextStyle(fontFamily = baseFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.3.sp),
    )

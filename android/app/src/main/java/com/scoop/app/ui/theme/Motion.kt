package com.scoop.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

/** Shared animation timing so transitions feel consistent rather than ad hoc. */
object Motion {
    const val QUICK_MS = 150
    const val STANDARD_MS = 220
    const val EMPHASIZED_MS = 300
    const val CONTAINER_TRANSFORM_MS = 240

    /** Material 3 "emphasized decelerate" curve - for content entering the screen. */
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Material 3 "emphasized accelerate" curve - for content leaving the screen. */
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> quick() = tween<T>(durationMillis = QUICK_MS)
}

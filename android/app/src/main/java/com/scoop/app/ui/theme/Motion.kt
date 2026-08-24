package com.scoop.app.ui.theme

import androidx.compose.animation.core.tween

/** Shared animation timing so transitions feel consistent rather than ad hoc. */
object Motion {
    const val QUICK_MS = 150
    const val STANDARD_MS = 220
    const val EMPHASIZED_MS = 300

    fun <T> quick() = tween<T>(durationMillis = QUICK_MS)

    fun <T> standard() = tween<T>(durationMillis = STANDARD_MS)

    fun <T> emphasized() = tween<T>(durationMillis = EMPHASIZED_MS)
}

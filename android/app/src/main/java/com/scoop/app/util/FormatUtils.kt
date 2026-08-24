package com.scoop.app.util

import kotlin.math.ln
import kotlin.math.pow

/** Human-readable file size, e.g. 1536L -> "1.5 KB". Never fabricates a value for null/unknown sizes. */
fun Long.toHumanReadableSize(): String {
    if (this <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroup = (ln(this.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = this / 1024.0.pow(digitGroup)
    return if (digitGroup == 0) "$this B" else "%.1f %s".format(value, units[digitGroup])
}

/** Compact ETA label, e.g. 95 -> "1m 35s left". */
fun Int.toEtaLabel(): String {
    if (this <= 0) return ""
    val minutes = this / 60
    val seconds = this % 60
    return if (minutes > 0) "${minutes}m ${seconds}s left" else "${seconds}s left"
}

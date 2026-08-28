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

/** Decimal (SI, 1000-based) size, e.g. for device storage capacity - OS storage UIs and drive
 * capacities are marketed/reported in decimal GB, unlike [toHumanReadableSize]'s binary GiB math. */
fun Long.toDecimalStorageSize(): String {
    if (this <= 0) return "0 GB"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroup = (ln(this.toDouble()) / ln(1000.0)).toInt().coerceIn(0, units.size - 1)
    val value = this / 1000.0.pow(digitGroup)
    return if (digitGroup == 0) "$this B" else "%.1f %s".format(value, units[digitGroup])
}

/** Compact ETA label, e.g. 95 -> "1m 35s left". */
fun Int.toEtaLabel(): String {
    if (this <= 0) return ""
    val minutes = this / 60
    val seconds = this % 60
    return if (minutes > 0) "${minutes}m ${seconds}s left" else "${seconds}s left"
}

/** Compact relative-time label for a past timestamp, e.g. "5m ago", "3h ago", "2d ago". */
fun Long.toRelativeTimeLabel(): String {
    val diff = (System.currentTimeMillis() - this).coerceAtLeast(0)
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(this))
    }
}

package com.scoop.app.core.model

object FormatSelector {
    fun video(preferredHeight: Int? = null, low: Boolean = false): String =
        when {
            low -> "worstvideo+worstaudio/worst"
            preferredHeight != null -> "bestvideo[height<=${preferredHeight}]+bestaudio/best[height<=${preferredHeight}]"
            else -> "bestvideo*+bestaudio/best"
        }

    fun audio(low: Boolean = false): String = if (low) "worstaudio/worst" else "bestaudio/best"
}

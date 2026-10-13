package com.scoop.app.core.model

object FormatSelector {
    fun video(preferredHeight: Int? = null, low: Boolean = false): String =
        when {
            low -> "worstvideo+worstaudio/worst"
            // `<=?` keeps formats whose height is unknown (direct files, many smaller sites), which a
            // plain `<=` filter silently drops. The trailing fallback makes the cap a preference:
            // a source offering nothing at or below it still downloads instead of failing with
            // "Requested format is not available".
            preferredHeight != null ->
                "bestvideo[height<=?${preferredHeight}]+bestaudio/best[height<=?${preferredHeight}]/bestvideo*+bestaudio/best"
            else -> "bestvideo*+bestaudio/best"
        }

    fun audio(low: Boolean = false): String = if (low) "worstaudio/worst" else "bestaudio/best"
}

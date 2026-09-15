package com.scoop.app.extractor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the subset of yt-dlp's `--dump-json` output Scoop actually reads. Parsed directly from
 * yt-dlp's own stdout rather than relying on any wrapper library's mapper, so the field set tracks
 * yt-dlp's stable JSON schema regardless of which native runtime ships it.
 */
@Serializable
data class YtDlpVideoJson(
    val title: String = "",
    val thumbnail: String? = null,
    @SerialName("webpage_url") val webpageUrl: String? = null,
    @SerialName("original_url") val originalUrl: String? = null,
)

@Serializable
data class YtDlpPlaylistEntryJson(
    val url: String? = null,
    val title: String? = null,
    val duration: Double? = null,
    val thumbnail: String? = null,
)

@Serializable
data class YtDlpPlaylistJson(
    val title: String? = null,
    val entries: List<YtDlpPlaylistEntryJson>? = null,
)

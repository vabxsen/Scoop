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
    val id: String = "",
    val title: String = "",
    val formats: List<YtDlpFormatJson>? = null,
    val thumbnail: String? = null,
    val description: String? = null,
    val uploader: String? = null,
    val channel: String? = null,
    val duration: Double? = null,
    @SerialName("upload_date") val uploadDate: String? = null,
    @SerialName("webpage_url") val webpageUrl: String? = null,
    @SerialName("original_url") val originalUrl: String? = null,
    @SerialName("extractor_key") val extractorKey: String? = null,
    @SerialName("requested_formats") val requestedFormats: List<YtDlpFormatJson>? = null,
)

@Serializable
data class YtDlpFormatJson(
    @SerialName("format_id") val formatId: String? = null,
    @SerialName("format_note") val formatNote: String? = null,
    val ext: String? = null,
    val vcodec: String? = null,
    val acodec: String? = null,
    val width: Double? = null,
    val height: Double? = null,
    val fps: Double? = null,
    val abr: Double? = null,
    val tbr: Double? = null,
    val filesize: Double? = null,
    @SerialName("filesize_approx") val fileSizeApprox: Double? = null,
)

@Serializable
data class YtDlpPlaylistEntryJson(
    val id: String? = null,
    val url: String? = null,
    val title: String? = null,
    val uploader: String? = null,
    val duration: Double? = null,
    val thumbnail: String? = null,
)

@Serializable
data class YtDlpPlaylistJson(
    val title: String? = null,
    val uploader: String? = null,
    val entries: List<YtDlpPlaylistEntryJson>? = null,
)

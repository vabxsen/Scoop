package com.scoop.app.core.model

/** A single downloadable stream as reported by the extraction engine. */
data class MediaFormat(
    val formatId: String,
    val container: String?,
    val formatNote: String?,
    val videoCodec: String?,
    val audioCodec: String?,
    val width: Int?,
    val height: Int?,
    val fps: Int?,
    val audioBitrateKbps: Double?,
    val totalBitrateKbps: Double?,
    val fileSizeBytes: Long?,
) {
    val hasVideo: Boolean
        get() = !videoCodec.isNullOrBlank() && videoCodec != "none"

    val hasAudio: Boolean
        get() = !audioCodec.isNullOrBlank() && audioCodec != "none"

    val isVideoOnly: Boolean
        get() = hasVideo && !hasAudio

    val isAudioOnly: Boolean
        get() = hasAudio && !hasVideo

    val resolutionLabel: String?
        get() = height?.let { "${it}p" }
}

/** Metadata for a single piece of media (or one entry of a playlist) after analysis. */
data class MediaInfo(
    val id: String,
    val sourceUrl: String,
    val title: String,
    val uploader: String?,
    val durationSeconds: Int?,
    val uploadDate: String?,
    val thumbnailUrl: String?,
    val description: String?,
    val formats: List<MediaFormat>,
) {
    val videoFormats: List<MediaFormat>
        get() = formats.filter { it.hasVideo }

    val audioOnlyFormats: List<MediaFormat>
        get() = formats.filter { it.isAudioOnly }
}

/** A playlist's own metadata plus the (possibly partial) list of entries resolved so far. */
data class PlaylistInfo(
    val sourceUrl: String,
    val title: String?,
    val uploader: String?,
    val entries: List<PlaylistEntryInfo>,
)

data class PlaylistEntryInfo(
    val id: String?,
    val url: String?,
    val title: String?,
    val uploader: String?,
    val durationSeconds: Int?,
    val thumbnailUrl: String?,
)

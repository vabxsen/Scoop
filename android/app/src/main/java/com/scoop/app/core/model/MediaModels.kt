package com.scoop.app.core.model

/** Metadata for a single piece of media (or one entry of a playlist) after analysis. */
data class MediaInfo(
    val sourceUrl: String,
    val title: String,
    val thumbnailUrl: String?,
)

/** A playlist's own metadata plus the (possibly partial) list of entries resolved so far. */
data class PlaylistInfo(
    val title: String?,
    val entries: List<PlaylistEntryInfo>,
)

data class PlaylistEntryInfo(
    val url: String?,
    val title: String?,
    val durationSeconds: Int?,
    val thumbnailUrl: String?,
)

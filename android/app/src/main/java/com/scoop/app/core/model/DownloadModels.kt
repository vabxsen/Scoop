package com.scoop.app.core.model

enum class DownloadKind {
    VIDEO,
    AUDIO_ONLY,
}

/** What the user asked to have downloaded, independent of how the queue executes it. */
data class DownloadRequest(
    val url: String,
    val kind: DownloadKind,
    val formatId: String? = null,
    val audioContainer: String? = null,
    /** Burns in whatever subtitle tracks are available - video only, since embedding requires a
     * video container to mux into. */
    val embedSubtitles: Boolean = false,
    /** Embeds the source's thumbnail as cover art - works for both a video file and audio (ID3
     * cover art). */
    val embedThumbnail: Boolean = false,
    /** Set only when this request was expanded from a playlist entry; used purely for grouping
     * in history/UI, never sent to yt-dlp. */
    val playlistTitle: String? = null,
    /** Raw extra yt-dlp arguments, tokenized and appended after Scoop's own options - lets a
     * later flag here override an earlier default one (argparse last-wins). */
    val customArgs: String? = null,
)

/** One item in the download queue: the fixed request plus display info captured at enqueue time. */
data class DownloadTask(
    val id: String,
    val request: DownloadRequest,
    val title: String,
    val thumbnailUrl: String?,
    val createdAt: Long = System.currentTimeMillis(),
)

sealed interface DownloadStatus {
    data object Queued : DownloadStatus

    data object Analyzing : DownloadStatus

    data class Downloading(
        val progress: Float = -1f,
        val etaSeconds: Int = 0,
    ) : DownloadStatus

    data object Processing : DownloadStatus

    data class Completed(val filePath: String?) : DownloadStatus

    data class Failed(val message: String, val throwable: Throwable? = null) : DownloadStatus

    data object Cancelled : DownloadStatus
}

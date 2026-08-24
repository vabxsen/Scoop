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
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val speedBytesPerSecond: Long = 0L,
        val etaSeconds: Int = 0,
    ) : DownloadStatus

    data object Processing : DownloadStatus

    data class Completed(val filePath: String?) : DownloadStatus

    data class Failed(val message: String, val throwable: Throwable? = null) : DownloadStatus

    data object Cancelled : DownloadStatus
}

data class DownloadTaskState(val task: DownloadTask, val status: DownloadStatus)

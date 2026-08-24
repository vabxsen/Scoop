package com.scoop.app.downloader

import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.DownloadTask

/** Owns the in-memory download queue. UI observes [tasks] directly; it's a Compose snapshot map. */
interface DownloadManager {
    val tasks: SnapshotStateMap<DownloadTask, DownloadStatus>

    fun enqueue(request: DownloadRequest, title: String, thumbnailUrl: String?): DownloadTask

    fun cancel(taskId: String): Boolean

    fun retry(taskId: String)

    fun remove(taskId: String): Boolean
}

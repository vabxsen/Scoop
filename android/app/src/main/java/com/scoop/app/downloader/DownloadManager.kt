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

    /** Removes [taskId] from the queue/list only; never touches a completed download's file. */
    fun remove(taskId: String): Boolean

    /** Explicit user action: deletes the completed file on disk (if any) and removes the task. */
    suspend fun deleteTaskAndFile(taskId: String)
}

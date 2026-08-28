package com.scoop.app.downloader

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.DownloadTask

/** Owns the in-memory download queue. UI observes [tasks] directly; it's a Compose snapshot map. */
interface DownloadManager {
    val tasks: SnapshotStateMap<DownloadTask, DownloadStatus>

    /** Ids currently swiped-away and pending a real delete once [requestDelete]'s undo window
     * elapses. UI filters these out of the visible list and shows the undo snackbar while any
     * are present. */
    val pendingDeleteIds: SnapshotStateList<String>

    fun enqueue(request: DownloadRequest, title: String, thumbnailUrl: String?): DownloadTask

    fun cancel(taskId: String): Boolean

    fun retry(taskId: String)

    /** Explicit user action: deletes the completed file on disk (if any) and removes the task. */
    suspend fun deleteTaskAndFile(taskId: String)

    /**
     * Marks [taskId] as pending delete and schedules the real [deleteTaskAndFile] a couple of
     * seconds out, giving the user a window to [undoDelete]. Runs on this manager's own
     * process-lifetime scope rather than the calling screen's - the delete still fires even if the
     * user navigates away before the window closes, instead of the pending delete silently
     * evaporating along with a screen-scoped coroutine.
     */
    fun requestDelete(taskId: String)

    /** Cancels one pending delete from [requestDelete], if it hasn't already fired. */
    fun undoDelete(taskId: String)

    /** Cancels every pending delete from [requestDelete], if any haven't already fired. */
    fun undoAllDeletes()

    /** Deletes completed downloads (history entry + file) older than [days] - the auto-clear sweep. */
    suspend fun clearHistoryOlderThan(days: Int)

    /** Explicit user action: cancels every in-flight download and deletes every task, history
     * entry, and downloaded file. */
    suspend fun clearAll()
}

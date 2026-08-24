package com.scoop.app.ui.screen.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.downloader.DownloadManager
import kotlinx.coroutines.launch

enum class DownloadFilter {
    ALL,
    DOWNLOADING,
    COMPLETED,
    FAILED,
}

class DownloadsViewModel(private val downloadManager: DownloadManager) : ViewModel() {

    val tasks get() = downloadManager.tasks

    fun matches(status: DownloadStatus, filter: DownloadFilter): Boolean =
        when (filter) {
            DownloadFilter.ALL -> true
            DownloadFilter.DOWNLOADING ->
                status is DownloadStatus.Queued || status is DownloadStatus.Analyzing || status is DownloadStatus.Downloading || status is DownloadStatus.Processing
            DownloadFilter.COMPLETED -> status is DownloadStatus.Completed
            DownloadFilter.FAILED -> status is DownloadStatus.Failed || status is DownloadStatus.Cancelled
        }

    fun primaryAction(taskId: String, status: DownloadStatus) {
        when (status) {
            is DownloadStatus.Queued,
            is DownloadStatus.Analyzing,
            is DownloadStatus.Downloading,
            is DownloadStatus.Processing -> downloadManager.cancel(taskId)
            is DownloadStatus.Failed,
            is DownloadStatus.Cancelled -> downloadManager.retry(taskId)
            is DownloadStatus.Completed -> Unit // handled by the screen (opens the file directly)
        }
    }

    fun delete(taskId: String) {
        viewModelScope.launch { downloadManager.deleteTaskAndFile(taskId) }
    }
}

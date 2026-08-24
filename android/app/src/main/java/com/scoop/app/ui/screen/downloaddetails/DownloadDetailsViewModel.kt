package com.scoop.app.ui.screen.downloaddetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scoop.app.core.database.DownloadHistoryDao
import com.scoop.app.core.database.objects.DownloadedItem
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.downloader.DownloadManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DownloadDetailsViewModel(private val downloadManager: DownloadManager, private val downloadHistoryDao: DownloadHistoryDao) :
    ViewModel() {

    fun liveEntry(taskId: String): Map.Entry<DownloadTask, DownloadStatus>? = downloadManager.tasks.entries.find { it.key.id == taskId }

    fun historyItem(taskId: String): Flow<DownloadedItem?> = downloadHistoryDao.observeById(taskId)

    fun retry(taskId: String) = downloadManager.retry(taskId)

    fun cancel(taskId: String) = downloadManager.cancel(taskId)

    fun delete(taskId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            downloadManager.deleteTaskAndFile(taskId)
            onDeleted()
        }
    }
}

package com.scoop.app.ui.screen.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scoop.app.R
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.downloader.DownloadManager
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(downloadManager: DownloadManager = koinInject()) {
    val entries = downloadManager.tasks.entries.toList()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_downloads)) }) }) { innerPadding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.downloads_empty), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(entries) { (task, status) -> DownloadRow(task, status) }
            }
        }
    }
}

@Composable
private fun DownloadRow(task: DownloadTask, status: DownloadStatus) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.title.ifBlank { task.request.url }, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(status.label(), style = MaterialTheme.typography.bodySmall)
            if (status is DownloadStatus.Downloading && status.progress >= 0f) {
                LinearProgressIndicator(progress = { status.progress }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun DownloadStatus.label(): String =
    when (this) {
        is DownloadStatus.Queued -> "Queued"
        is DownloadStatus.Analyzing -> "Analyzing…"
        is DownloadStatus.Downloading -> if (progress >= 0f) "Downloading ${(progress * 100).toInt()}%" else "Downloading…"
        is DownloadStatus.Processing -> "Processing…"
        is DownloadStatus.Completed -> "Completed"
        is DownloadStatus.Failed -> "Failed: $message"
        is DownloadStatus.Cancelled -> "Cancelled"
    }

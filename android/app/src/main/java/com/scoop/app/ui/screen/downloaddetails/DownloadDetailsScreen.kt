package com.scoop.app.ui.screen.downloaddetails

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.AnnotatedString
import com.scoop.app.R
import com.scoop.app.core.database.objects.DownloadedItem
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.ui.common.MediaThumbnail
import com.scoop.app.ui.common.StatusChip
import com.scoop.app.ui.theme.Spacing
import com.scoop.app.util.FileShareUtils
import org.koin.androidx.compose.koinViewModel

private data class DownloadDetails(
    val title: String,
    val thumbnailUrl: String?,
    val sourceUrl: String,
    val kindLabel: String,
    val filePath: String?,
    val status: DownloadStatus?,
    val createdAt: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadDetailsScreen(taskId: String, onBack: () -> Unit, viewModel: DownloadDetailsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val liveEntry = viewModel.liveEntry(taskId)
    val historyFlow = remember(taskId) { viewModel.historyItem(taskId) }
    val historyItem by historyFlow.collectAsState(initial = null)

    val details = liveEntry.toDetails() ?: historyItem?.toDetails()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.title ?: stringResource(R.string.download_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        }
    ) { innerPadding ->
        if (details == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(Spacing.md)) {
            MediaThumbnail(url = details.thumbnailUrl, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Text(details.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = Spacing.md))
            Row(modifier = Modifier.padding(top = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (details.status != null) StatusChip(details.status)
                Text(details.kindLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                DateUtils.getRelativeTimeSpanString(details.createdAt).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            Text(
                details.sourceUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                modifier = Modifier.padding(top = Spacing.sm),
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (details.filePath != null) {
                    OutlinedButton(onClick = { FileShareUtils.openFile(context, details.filePath) }) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
                        Text(stringResource(R.string.action_open))
                    }
                    OutlinedButton(onClick = { FileShareUtils.shareFile(context, details.filePath) }) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
                        Text(stringResource(R.string.action_share))
                    }
                }
                if (details.status is DownloadStatus.Failed || details.status is DownloadStatus.Cancelled) {
                    OutlinedButton(onClick = { viewModel.retry(taskId) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = { clipboardManager.setText(AnnotatedString(details.sourceUrl)) }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
                    Text(stringResource(R.string.action_copy_url))
                }
                OutlinedButton(onClick = { viewModel.delete(taskId, onDeleted = onBack) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}

private fun Map.Entry<com.scoop.app.core.model.DownloadTask, DownloadStatus>?.toDetails(): DownloadDetails? =
    this?.let { (task, status) ->
        DownloadDetails(
            title = task.title.ifBlank { task.request.url },
            thumbnailUrl = task.thumbnailUrl,
            sourceUrl = task.request.url,
            kindLabel = if (task.request.kind.name == "AUDIO_ONLY") "Audio" else "Video",
            filePath = (status as? DownloadStatus.Completed)?.filePath,
            status = status,
            createdAt = task.createdAt,
        )
    }

private fun DownloadedItem.toDetails(): DownloadDetails =
    DownloadDetails(
        title = title,
        thumbnailUrl = thumbnailUrl,
        sourceUrl = sourceUrl,
        kindLabel = if (kind == "AUDIO_ONLY") "Audio" else "Video",
        filePath = filePath,
        status = filePath?.let { DownloadStatus.Completed(it) },
        createdAt = createdAt,
    )

package com.scoop.app.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.ui.theme.Motion
import com.scoop.app.ui.theme.Spacing

@Composable
fun DownloadCard(
    task: DownloadTask,
    status: DownloadStatus,
    onClick: () -> Unit,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(tween(Motion.STANDARD_MS)).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            MediaThumbnail(url = task.thumbnailUrl, modifier = Modifier.size(width = 72.dp, height = 48.dp))
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    task.title.ifBlank { task.request.url },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                StatusChip(status)
                if (status is DownloadStatus.Downloading) {
                    val animatedProgress by
                        animateFloatAsState(
                            targetValue = if (status.progress >= 0f) status.progress else 0f,
                            animationSpec = tween(Motion.STANDARD_MS),
                            label = "downloadProgress",
                        )
                    if (status.progress >= 0f) {
                        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            DownloadCardPrimaryAction(status = status, onClick = onPrimaryAction)
        }
    }
}

@Composable
private fun DownloadCardPrimaryAction(status: DownloadStatus, onClick: () -> Unit) {
    val (icon, description) =
        when (status) {
            is DownloadStatus.Queued,
            is DownloadStatus.Analyzing,
            is DownloadStatus.Downloading,
            is DownloadStatus.Processing -> Icons.Outlined.Close to "Cancel"
            is DownloadStatus.Failed,
            is DownloadStatus.Cancelled -> Icons.Outlined.Refresh to "Retry"
            is DownloadStatus.Completed -> Icons.Outlined.FileDownload to "Open"
        }
    IconButton(onClick = onClick) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = { (scaleIn(tween(Motion.QUICK_MS)) + fadeIn(tween(Motion.QUICK_MS))) togetherWith (scaleOut(tween(Motion.QUICK_MS)) + fadeOut(tween(Motion.QUICK_MS))) },
            label = "primaryActionIcon",
        ) { animatedIcon ->
            Icon(animatedIcon, contentDescription = description)
        }
    }
}

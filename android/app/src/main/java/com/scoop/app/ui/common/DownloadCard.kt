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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.ui.theme.Motion
import com.scoop.app.ui.theme.Spacing
import com.scoop.app.util.toRelativeTimeLabel

private val ThumbnailSize = DpSize(width = 84.dp, height = 64.dp)

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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box {
                MediaThumbnail(
                    url = task.thumbnailUrl,
                    modifier = Modifier.size(ThumbnailSize.width, ThumbnailSize.height),
                    cornerRadius = 10.dp,
                )
                KindBadge(task.request.kind, modifier = Modifier.align(Alignment.BottomStart).padding(6.dp))
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    task.title.ifBlank { task.request.url },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                DownloadCardMeta(task = task, status = status)
                if (status is DownloadStatus.Downloading) {
                    val animatedProgress by
                        animateFloatAsState(
                            targetValue = if (status.progress >= 0f) status.progress else 0f,
                            animationSpec = tween(Motion.STANDARD_MS),
                            label = "downloadProgress",
                        )
                    if (status.progress >= 0f) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    }
                }
            }
            DownloadCardPrimaryAction(status = status, onClick = onPrimaryAction)
        }
    }
}

/** Small circular kind indicator (video/audio) pinned to the thumbnail's corner. */
@Composable
private fun KindBadge(kind: DownloadKind, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (kind == DownloadKind.AUDIO_ONLY) Icons.Outlined.MusicNote else Icons.Outlined.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp),
        )
    }
}

/**
 * The line under the title. A finished download shows a quiet "kind · time" caption instead of a
 * status pill - the pill only earns its visual weight while something still needs the user's
 * attention (queued, in flight, or failed), which keeps a long history from turning into a wall
 * of identical "Completed" badges.
 */
@Composable
private fun DownloadCardMeta(task: DownloadTask, status: DownloadStatus) {
    when (status) {
        is DownloadStatus.Completed -> {
            val kindLabel = if (task.request.kind == DownloadKind.AUDIO_ONLY) "Audio" else "Video"
            Text(
                "$kindLabel · ${task.createdAt.toRelativeTimeLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> StatusChip(status)
    }
}

@Composable
private fun DownloadCardPrimaryAction(status: DownloadStatus, onClick: () -> Unit) {
    val (icon, description, tone) =
        when (status) {
            is DownloadStatus.Queued,
            is DownloadStatus.Analyzing,
            is DownloadStatus.Downloading,
            is DownloadStatus.Processing -> Triple(Icons.Outlined.Close, "Cancel", ActionTone.NEUTRAL)
            is DownloadStatus.Failed,
            is DownloadStatus.Cancelled -> Triple(Icons.Outlined.Refresh, "Retry", ActionTone.ACCENT)
            is DownloadStatus.Completed -> Triple(Icons.Outlined.FileDownload, "Open", ActionTone.ACCENT)
        }
    val colors =
        when (tone) {
            ActionTone.NEUTRAL ->
                IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            ActionTone.ACCENT -> IconButtonDefaults.filledTonalIconButtonColors()
        }
    FilledTonalIconButton(onClick = onClick, colors = colors) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = { (scaleIn(tween(Motion.QUICK_MS)) + fadeIn(tween(Motion.QUICK_MS))) togetherWith (scaleOut(tween(Motion.QUICK_MS)) + fadeOut(tween(Motion.QUICK_MS))) },
            label = "primaryActionIcon",
        ) { animatedIcon ->
            Icon(animatedIcon, contentDescription = description)
        }
    }
}

private enum class ActionTone { NEUTRAL, ACCENT }

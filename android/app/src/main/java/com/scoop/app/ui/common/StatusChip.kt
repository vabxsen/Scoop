package com.scoop.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.ui.theme.Motion
import com.scoop.app.util.toEtaLabel

@Composable
fun StatusChip(status: DownloadStatus, modifier: Modifier = Modifier) {
    val (label, targetContainer, targetContent) = status.chipColors()
    val containerColor by animateColorAsState(targetContainer, tween(Motion.STANDARD_MS), label = "chipContainer")
    val contentColor by animateColorAsState(targetContent, tween(Motion.STANDARD_MS), label = "chipContent")
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor,
        modifier = modifier.clip(RoundedCornerShape(50)).background(containerColor).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun DownloadStatus.chipColors(): Triple<String, Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        is DownloadStatus.Queued -> Triple("Queued", scheme.surfaceContainerHigh, scheme.onSurfaceVariant)
        is DownloadStatus.Analyzing -> Triple("Analyzing…", scheme.secondaryContainer, scheme.onSecondaryContainer)
        is DownloadStatus.Downloading -> {
            val pct = if (progress >= 0f) "${(progress * 100).toInt()}%" else "Downloading…"
            val eta = if (etaSeconds > 0) " · ${etaSeconds.toEtaLabel()}" else ""
            Triple("$pct$eta", scheme.primaryContainer, scheme.onPrimaryContainer)
        }
        is DownloadStatus.Processing -> Triple("Processing…", scheme.secondaryContainer, scheme.onSecondaryContainer)
        is DownloadStatus.Completed -> Triple("Completed", scheme.tertiaryContainer, scheme.onTertiaryContainer)
        is DownloadStatus.Failed -> Triple("Failed", scheme.errorContainer, scheme.onErrorContainer)
        is DownloadStatus.Cancelled -> Triple("Cancelled", scheme.surfaceContainerHigh, scheme.onSurfaceVariant)
    }
}

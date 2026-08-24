package com.scoop.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.scoop.app.core.model.MediaFormat
import com.scoop.app.ui.theme.Motion
import com.scoop.app.ui.theme.Spacing
import com.scoop.app.util.toHumanReadableSize

@Composable
fun FormatOptionTile(format: MediaFormat, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor by
        animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            animationSpec = Motion.quick(),
            label = "formatTileContainer",
        )
    val borderColor by
        animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            animationSpec = Motion.quick(),
            label = "formatTileBorder",
        )
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier =
            modifier
                .clip(shape)
                .background(containerColor)
                .border(BorderStroke(1.dp, borderColor), shape)
                .clickable(onClick = onClick)
                .padding(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                format.resolutionLabel ?: if (format.isAudioOnly) "Audio" else "Unknown",
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (format.hasVideo) {
                    Icon(Icons.Outlined.Videocam, contentDescription = "Video", modifier = Modifier.padding(1.dp), tint = contentColor)
                }
                if (format.hasAudio) {
                    Icon(Icons.Outlined.GraphicEq, contentDescription = "Audio", modifier = Modifier.padding(1.dp), tint = contentColor)
                }
            }
        }
        Text(
            (format.fileSizeBytes?.toHumanReadableSize() ?: "—") +
                (format.totalBitrateKbps?.let { " · %.0f kbps".format(it) } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
        )
        Text(
            listOfNotNull(format.container?.uppercase(), format.videoCodec, format.audioCodec).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
        )
    }
}

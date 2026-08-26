package com.scoop.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
    val subtitleColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(14.dp)

    val subtitle =
        listOfNotNull(
            format.fileSizeBytes?.toHumanReadableSize(),
            format.totalBitrateKbps?.let { "%.0f kbps".format(it) },
            format.container?.uppercase(),
        ).joinToString(" · ")

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(containerColor)
                .border(BorderStroke(1.dp, borderColor), shape)
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (format.hasVideo) Icons.Outlined.Videocam else Icons.Outlined.GraphicEq,
            contentDescription = null,
            tint = contentColor,
        )
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.sm)) {
            Text(
                format.resolutionLabel ?: if (format.isAudioOnly) "Audio" else "Unknown",
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                maxLines = 1,
            )
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
            }
        }
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = contentColor))
    }
}

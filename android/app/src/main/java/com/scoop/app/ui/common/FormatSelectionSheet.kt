package com.scoop.app.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scoop.app.R
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.MediaFormat
import com.scoop.app.core.model.MediaInfo
import com.scoop.app.ui.theme.Motion
import com.scoop.app.ui.theme.Spacing
import com.scoop.app.util.toHumanReadableSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectionSheet(
    mediaInfo: MediaInfo,
    kind: DownloadKind,
    selectedFormat: MediaFormat?,
    onFormatSelected: (MediaFormat?) -> Unit,
    onConfirmDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.heightIn(max = maxSheetHeight).padding(horizontal = Spacing.md)) {
            Text(
                if (kind == DownloadKind.VIDEO) stringResource(R.string.format_sheet_title_video) else stringResource(R.string.format_sheet_title_audio),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Spacing.sm),
            )

            BestAvailableOption(selected = selectedFormat == null, onClick = { onFormatSelected(null) })

            val sections =
                if (kind == DownloadKind.VIDEO) {
                    listOf(
                        stringResource(R.string.format_section_suggested) to mediaInfo.formats.filter { it.hasVideo && it.hasAudio },
                        stringResource(R.string.format_section_video_only) to mediaInfo.formats.filter { it.isVideoOnly },
                    )
                } else {
                    listOf(stringResource(R.string.format_section_audio) to mediaInfo.audioOnlyFormats)
                }

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.md),
            ) {
                sections.forEach { (label, formats) ->
                    if (formats.isEmpty()) return@forEach
                    item {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
                        )
                    }
                    items(formats, key = { it.formatId }) { format ->
                        FormatOptionTile(
                            format = format,
                            selected = selectedFormat?.formatId == format.formatId,
                            onClick = { onFormatSelected(format) },
                        )
                    }
                }
            }

            HorizontalDivider()
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md)) {
                val summary =
                    selectedFormat?.let {
                        listOfNotNull(it.resolutionLabel ?: "Audio", it.container?.uppercase(), it.fileSizeBytes?.toHumanReadableSize())
                            .joinToString(" · ")
                    } ?: stringResource(R.string.quality_best_available)
                AnimatedContent(
                    targetState = summary,
                    transitionSpec = { fadeIn(tween(Motion.QUICK_MS)) togetherWith fadeOut(tween(Motion.QUICK_MS)) },
                    modifier = Modifier.padding(bottom = Spacing.sm),
                    label = "formatSummary",
                ) { text ->
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = onConfirmDownload, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_download))
                }
            }
        }
    }
}

@Composable
private fun BestAvailableOption(selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val containerColor by
        animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            animationSpec = tween(Motion.QUICK_MS),
            label = "bestAvailableContainer",
        )
    val borderColor by
        animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            animationSpec = tween(Motion.QUICK_MS),
            label = "bestAvailableBorder",
        )
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(shape)
                .background(containerColor)
                .border(BorderStroke(1.dp, borderColor), shape)
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = if (selected) contentColor else MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.sm)) {
            Text(stringResource(R.string.quality_best_available), style = MaterialTheme.typography.titleSmall, color = contentColor)
            Text(stringResource(R.string.quality_best_available_subtitle), style = MaterialTheme.typography.bodySmall, color = subtitleColor)
        }
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = contentColor))
    }
}

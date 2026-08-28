package com.scoop.app.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scoop.app.R
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.MediaInfo
import com.scoop.app.ui.common.BestAvailableOption
import com.scoop.app.ui.common.ErrorState
import com.scoop.app.ui.common.FormatOptionTile
import com.scoop.app.ui.common.LoadingState
import com.scoop.app.ui.common.SettingSectionLabel
import com.scoop.app.ui.theme.Motion
import com.scoop.app.ui.theme.Spacing
import com.scoop.app.util.DownloadBlockReason
import com.scoop.app.util.DownloadGate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureDownloadSheet(viewModel: HomeViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeTaskId = viewModel.activeDownloadTaskId

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm).animateContentSize(tween(Motion.STANDARD_MS)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (activeTaskId != null) {
                DownloadProgressContent(status = viewModel.activeDownloadStatus, onDone = onDismiss)
            } else {
                Icon(Icons.Outlined.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.configure_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                Text(
                    stringResource(R.string.configure_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.md),
                )

                AnimatedContent(
                    targetState = viewModel.configureState,
                    transitionSpec = { (fadeIn(tween(Motion.STANDARD_MS))) togetherWith (fadeOut(tween(Motion.QUICK_MS))) },
                    modifier = Modifier.animateContentSize(tween(Motion.STANDARD_MS)),
                    label = "configureSheetState",
                ) { state ->
                    when (state) {
                        is ConfigureUiState.Hidden -> Unit
                        is ConfigureUiState.Loading -> LoadingState(message = stringResource(R.string.analyzing))
                        is ConfigureUiState.Error ->
                            ErrorState(
                                title = stringResource(R.string.analyze_error_title),
                                message = stringResource(R.string.analyze_error_body),
                                detail = state.message,
                                retryLabel = stringResource(R.string.action_retry),
                                onRetry = viewModel::retryAnalyze,
                            )
                        is ConfigureUiState.Loaded -> ConfigureForm(viewModel = viewModel, info = state.info, onDismiss = onDismiss)
                    }
                }
            }
        }
    }
}

/** Shown in place of the configure form once a download has been enqueued: live 0-100% progress,
 * then a completed/failed end state, mirroring the task's real [DownloadStatus]. */
@Composable
private fun DownloadProgressContent(status: DownloadStatus?, onDone: () -> Unit) {
    when (status) {
        is DownloadStatus.Completed -> {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp).padding(top = Spacing.md),
            )
            Text(
                stringResource(R.string.download_complete_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = Spacing.md),
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg), shape = RoundedCornerShape(50)) {
                Text(stringResource(R.string.action_done))
            }
        }
        is DownloadStatus.Failed ->
            ErrorState(
                title = stringResource(R.string.download_failed_title),
                message = status.message,
                retryLabel = stringResource(R.string.action_done),
                onRetry = onDone,
            )
        is DownloadStatus.Cancelled, null -> {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.md))
            Text(
                stringResource(R.string.download_cancelled_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.md),
            )
            OutlinedButton(onClick = onDone, shape = RoundedCornerShape(50)) { Text(stringResource(R.string.action_done)) }
        }
        is DownloadStatus.Queued -> {
            // A queued task isn't "downloading" yet - most often it's deliberately held back by
            // Wi-Fi-only or the low-battery pause, and naming the real reason here (instead of a
            // generic "Queued") is what makes those gates legible: without it, the task just
            // looks stuck since nothing else on this sheet distinguishes "waiting on purpose"
            // from "broken".
            val context = LocalContext.current
            val blockReason = DownloadGate.blockedReason(context)
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.md).size(48.dp),
            )
            Text(
                stringResource(
                    when (blockReason) {
                        DownloadBlockReason.METERED_CONNECTION -> R.string.download_waiting_wifi_title
                        DownloadBlockReason.LOW_BATTERY -> R.string.download_waiting_battery_title
                        null -> R.string.download_queued_title
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.md),
            )
        }
        else -> {
            val progress = (status as? DownloadStatus.Downloading)?.progress ?: -1f
            val animatedProgress by
                animateFloatAsState(
                    targetValue = if (progress >= 0f) progress else 0f,
                    animationSpec = tween(Motion.STANDARD_MS),
                    label = "sheetDownloadProgress",
                )
            Box(modifier = Modifier.padding(vertical = Spacing.md).size(72.dp), contentAlignment = Alignment.Center) {
                if (progress >= 0f) {
                    CircularProgressIndicator(progress = { animatedProgress }, modifier = Modifier.size(72.dp), strokeWidth = 5.dp)
                    Text("${(animatedProgress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(72.dp), strokeWidth = 5.dp)
                }
            }
            Text(
                when (status) {
                    is DownloadStatus.Analyzing -> stringResource(R.string.analyzing)
                    is DownloadStatus.Processing -> stringResource(R.string.download_processing_title)
                    else -> stringResource(R.string.download_progress_title)
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = Spacing.md),
            )
        }
    }
}

@Composable
private fun ConfigureForm(viewModel: HomeViewModel, info: MediaInfo, onDismiss: () -> Unit) {
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .verticalScroll(rememberScrollState())
                .animateContentSize(tween(Motion.STANDARD_MS)),
    ) {
        SettingSectionLabel(stringResource(R.string.configure_download_type), modifier = Modifier.padding(horizontal = 0.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = viewModel.selectedKind == DownloadKind.AUDIO_ONLY,
                onClick = { viewModel.selectKind(DownloadKind.AUDIO_ONLY) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(R.string.kind_audio)) }
            SegmentedButton(
                selected = viewModel.selectedKind == DownloadKind.VIDEO,
                onClick = { viewModel.selectKind(DownloadKind.VIDEO) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(R.string.kind_video)) }
        }

        SettingSectionLabel(stringResource(R.string.configure_format_selection), modifier = Modifier.padding(top = Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(
                selected = viewModel.formatMode == FormatMode.AUTO,
                onClick = { viewModel.selectFormatMode(FormatMode.AUTO) },
                label = { Text(stringResource(R.string.option_auto)) },
                leadingIcon =
                    if (viewModel.formatMode == FormatMode.AUTO) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
            )
            FilterChip(
                selected = viewModel.formatMode == FormatMode.CUSTOM,
                onClick = { viewModel.selectFormatMode(FormatMode.CUSTOM) },
                label = { Text(stringResource(R.string.option_custom)) },
                leadingIcon =
                    if (viewModel.formatMode == FormatMode.CUSTOM) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
            )
        }

        // The format list used to live behind a "Custom" chip that opened its own sheet on top of
        // this one; it's inlined here instead so picking an exact format doesn't feel like leaving
        // the configure flow.
        AnimatedVisibility(
            visible = viewModel.formatMode == FormatMode.CUSTOM,
            enter = fadeIn(tween(Motion.STANDARD_MS)) + expandVertically(tween(Motion.STANDARD_MS)),
            exit = fadeOut(tween(Motion.QUICK_MS)) + shrinkVertically(tween(Motion.QUICK_MS)),
        ) {
            Column(modifier = Modifier.padding(top = Spacing.md)) {
                SettingSectionLabel(stringResource(R.string.configure_format_preference), modifier = Modifier.padding(horizontal = 0.dp))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.padding(top = Spacing.xs)) {
                    BestAvailableOption(selected = viewModel.selectedFormat == null, onClick = { viewModel.selectFormat(null) })

                    val sections =
                        if (viewModel.selectedKind == DownloadKind.VIDEO) {
                            listOf(
                                stringResource(R.string.format_section_suggested) to info.formats.filter { it.hasVideo && it.hasAudio },
                                stringResource(R.string.format_section_video_only) to info.formats.filter { it.isVideoOnly },
                            )
                        } else {
                            listOf(stringResource(R.string.format_section_audio) to info.audioOnlyFormats)
                        }
                    sections.forEach { (label, formats) ->
                        if (formats.isEmpty()) return@forEach
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                        formats.forEach { format ->
                            FormatOptionTile(format = format, selected = viewModel.selectedFormat?.formatId == format.formatId, onClick = { viewModel.selectFormat(format) })
                        }
                    }
                }
            }
        }

        SettingSectionLabel(stringResource(R.string.configure_additional_settings), modifier = Modifier.padding(top = Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (viewModel.selectedKind == DownloadKind.VIDEO) {
                FilterChip(
                    selected = viewModel.embedSubtitles,
                    onClick = viewModel::toggleEmbedSubtitles,
                    label = { Text(stringResource(R.string.option_embed_subtitles)) },
                    leadingIcon = { Icon(Icons.Outlined.Subtitles, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
                )
            }
            FilterChip(
                selected = viewModel.embedThumbnail,
                onClick = viewModel::toggleEmbedThumbnail,
                label = { Text(stringResource(R.string.option_embed_thumbnail)) },
                leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(50)) {
                Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.action_cancel), modifier = Modifier.padding(start = Spacing.xs))
            }
            Button(
                onClick = { viewModel.confirmDownload() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.action_download), modifier = Modifier.padding(start = Spacing.xs))
            }
        }
    }
}

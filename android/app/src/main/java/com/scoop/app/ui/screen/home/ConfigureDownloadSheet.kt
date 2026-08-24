package com.scoop.app.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scoop.app.R
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.MediaInfo
import com.scoop.app.ui.common.ErrorState
import com.scoop.app.ui.common.FormatSelectionSheet
import com.scoop.app.ui.common.LoadingState
import com.scoop.app.ui.common.SettingSectionLabel
import com.scoop.app.ui.common.SettingRow
import com.scoop.app.ui.theme.Motion
import com.scoop.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureDownloadSheet(viewModel: HomeViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

@Composable
private fun ConfigureForm(viewModel: HomeViewModel, info: MediaInfo, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().animateContentSize(tween(Motion.STANDARD_MS))) {
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
            )
            FilterChip(
                selected = viewModel.formatMode == FormatMode.CUSTOM,
                onClick = { viewModel.selectFormatMode(FormatMode.CUSTOM) },
                label = { Text(stringResource(R.string.option_custom)) },
            )
        }

        AnimatedVisibility(
            visible = viewModel.formatMode == FormatMode.CUSTOM,
            enter = fadeIn(tween(Motion.STANDARD_MS)) + expandVertically(tween(Motion.STANDARD_MS)),
            exit = fadeOut(tween(Motion.QUICK_MS)) + shrinkVertically(tween(Motion.QUICK_MS)),
        ) {
            Column {
                SettingSectionLabel(stringResource(R.string.configure_format_preference), modifier = Modifier.padding(top = Spacing.md))
                SettingRow(
                    title =
                        if (viewModel.selectedKind == DownloadKind.VIDEO) stringResource(R.string.quality_chip_label)
                        else stringResource(R.string.audio_format_chip_label),
                    subtitle = viewModel.selectedFormat?.resolutionLabel ?: viewModel.selectedFormat?.container?.uppercase(),
                    trailingContent = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
                    onClick = viewModel::openFormatPicker,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.action_cancel), modifier = Modifier.padding(start = Spacing.xs))
            }
            Button(
                onClick = { if (viewModel.confirmDownload()) onDismiss() },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.action_download), modifier = Modifier.padding(start = Spacing.xs))
            }
        }
    }

    if (viewModel.showFormatPickerSheet) {
        FormatSelectionSheet(
            mediaInfo = info,
            kind = viewModel.selectedKind,
            selectedFormat = viewModel.selectedFormat,
            onFormatSelected = viewModel::selectFormat,
            onConfirmDownload = { if (viewModel.confirmDownload()) onDismiss() },
            onDismiss = viewModel::dismissFormatPicker,
        )
    }
}

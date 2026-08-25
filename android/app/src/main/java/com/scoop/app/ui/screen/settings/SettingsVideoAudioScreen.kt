package com.scoop.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.scoop.app.R
import com.scoop.app.core.model.AudioQuality
import com.scoop.app.core.model.DefaultAudioFormat
import com.scoop.app.core.model.DefaultVideoContainer
import com.scoop.app.core.model.DefaultVideoQuality
import com.scoop.app.ui.common.SettingRadioSheet
import com.scoop.app.ui.common.SettingRow
import org.koin.androidx.compose.koinViewModel

private enum class ActiveSheet { NONE, QUALITY, VIDEO_CONTAINER, AUDIO_FORMAT, AUDIO_QUALITY, CONCURRENCY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsVideoAudioScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var activeSheet by remember { mutableStateOf(ActiveSheet.NONE) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_video_audio_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                SettingRow(
                    title = stringResource(R.string.settings_default_video_quality),
                    subtitle = viewModel.defaultVideoQuality.label,
                    leadingIcon = Icons.Outlined.HighQuality,
                    onClick = { activeSheet = ActiveSheet.QUALITY },
                )
            }
            item {
                SettingRow(
                    title = stringResource(R.string.settings_default_video_container),
                    subtitle = viewModel.defaultVideoContainer.label,
                    leadingIcon = Icons.Outlined.Movie,
                    onClick = { activeSheet = ActiveSheet.VIDEO_CONTAINER },
                )
            }
            item {
                SettingRow(
                    title = stringResource(R.string.settings_default_audio_format),
                    subtitle = viewModel.defaultAudioFormat.label,
                    leadingIcon = Icons.Outlined.AudioFile,
                    onClick = { activeSheet = ActiveSheet.AUDIO_FORMAT },
                )
            }
            item {
                SettingRow(
                    title = stringResource(R.string.settings_audio_quality),
                    subtitle = viewModel.audioQuality.label,
                    leadingIcon = Icons.Outlined.GraphicEq,
                    onClick = { activeSheet = ActiveSheet.AUDIO_QUALITY },
                )
            }
            item {
                SettingRow(
                    title = stringResource(R.string.settings_concurrent_downloads),
                    subtitle = viewModel.maxConcurrentDownloads.toString(),
                    leadingIcon = Icons.Outlined.Speed,
                    onClick = { activeSheet = ActiveSheet.CONCURRENCY },
                )
            }
        }
    }

    when (activeSheet) {
        ActiveSheet.QUALITY ->
            SettingRadioSheet(
                title = stringResource(R.string.settings_default_video_quality),
                options = DefaultVideoQuality.entries,
                selected = viewModel.defaultVideoQuality,
                optionLabel = { it.label },
                onSelect = viewModel::updateDefaultVideoQuality,
                onDismiss = { activeSheet = ActiveSheet.NONE },
            )
        ActiveSheet.VIDEO_CONTAINER ->
            SettingRadioSheet(
                title = stringResource(R.string.settings_default_video_container),
                options = DefaultVideoContainer.entries,
                selected = viewModel.defaultVideoContainer,
                optionLabel = { it.label },
                onSelect = viewModel::updateDefaultVideoContainer,
                onDismiss = { activeSheet = ActiveSheet.NONE },
            )
        ActiveSheet.AUDIO_FORMAT ->
            SettingRadioSheet(
                title = stringResource(R.string.settings_default_audio_format),
                options = DefaultAudioFormat.entries,
                selected = viewModel.defaultAudioFormat,
                optionLabel = { it.label },
                onSelect = viewModel::updateDefaultAudioFormat,
                onDismiss = { activeSheet = ActiveSheet.NONE },
            )
        ActiveSheet.AUDIO_QUALITY ->
            SettingRadioSheet(
                title = stringResource(R.string.settings_audio_quality),
                options = AudioQuality.entries,
                selected = viewModel.audioQuality,
                optionLabel = { it.label },
                onSelect = viewModel::updateAudioQuality,
                onDismiss = { activeSheet = ActiveSheet.NONE },
            )
        ActiveSheet.CONCURRENCY ->
            SettingRadioSheet(
                title = stringResource(R.string.settings_concurrent_downloads),
                options = listOf(1, 2, 3, 4, 5),
                selected = viewModel.maxConcurrentDownloads,
                optionLabel = { it.toString() },
                onSelect = viewModel::updateMaxConcurrentDownloads,
                onDismiss = { activeSheet = ActiveSheet.NONE },
            )
        ActiveSheet.NONE -> Unit
    }
}

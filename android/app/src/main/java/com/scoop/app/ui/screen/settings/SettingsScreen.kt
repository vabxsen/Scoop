package com.scoop.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.scoop.app.R
import com.scoop.app.ui.common.SettingHubRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    onBack: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenVideoAudio: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_general_title),
                    subtitle = stringResource(R.string.settings_general_subtitle),
                    leadingIcon = Icons.Filled.Palette,
                    onClick = onOpenGeneral,
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_downloads_title),
                    subtitle = stringResource(R.string.settings_downloads_subtitle),
                    leadingIcon = Icons.Filled.Download,
                    onClick = onOpenDownloads,
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_video_audio_title),
                    subtitle = stringResource(R.string.settings_video_audio_hub_subtitle),
                    leadingIcon = Icons.Filled.VideoLibrary,
                    onClick = onOpenVideoAudio,
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_storage_title),
                    subtitle = stringResource(R.string.settings_storage_subtitle),
                    leadingIcon = Icons.Filled.Storage,
                    onClick = onOpenStorage,
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_about_title),
                    subtitle = stringResource(R.string.settings_about_subtitle),
                    leadingIcon = Icons.Filled.Info,
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

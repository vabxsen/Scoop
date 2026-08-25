package com.scoop.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Storage
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
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.downloader.DownloadPaths
import com.scoop.app.ui.common.SettingRow
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsStorageScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_storage_title)) },
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
                    title = stringResource(R.string.settings_storage_used),
                    subtitle = viewModel.storageUsedLabel ?: stringResource(R.string.settings_storage_used_empty),
                    leadingIcon = Icons.Outlined.Storage,
                )
            }
            item {
                SettingRow(
                    title = stringResource(R.string.settings_video_location),
                    subtitle = DownloadPaths.displayLabel(DownloadKind.VIDEO),
                    leadingIcon = Icons.Outlined.Folder,
                )
            }
            item {
                SettingRow(
                    title = stringResource(R.string.settings_audio_location),
                    subtitle = DownloadPaths.displayLabel(DownloadKind.AUDIO_ONLY),
                    leadingIcon = Icons.Outlined.Folder,
                )
            }
        }
    }
}

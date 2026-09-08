package com.scoop.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistAddCheck
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scoop.app.R
import com.scoop.app.ui.common.SettingHubRow
import com.scoop.app.ui.common.SettingsScreenTitle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMiscScreen(onBack: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val ytDlpUpdateState = viewModel.ytDlpUpdateState

    LaunchedEffect(ytDlpUpdateState) {
        val message =
            when (val state = ytDlpUpdateState) {
                is YtDlpUpdateState.Done -> state.message
                is YtDlpUpdateState.Error -> state.message
                else -> null
            }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeYtDlpUpdateState()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { SettingsScreenTitle(stringResource(R.string.settings_misc_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_update_ytdlp),
                    subtitle =
                        if (ytDlpUpdateState is YtDlpUpdateState.Checking) {
                            stringResource(R.string.settings_update_ytdlp_checking)
                        } else {
                            stringResource(R.string.settings_update_ytdlp_subtitle)
                        },
                    leadingIcon = Icons.Outlined.Update,
                    onClick = viewModel::checkForYtDlpUpdate,
                    trailingContent = {
                        if (ytDlpUpdateState is YtDlpUpdateState.Checking) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_configure_before_download),
                    subtitle = stringResource(R.string.settings_configure_before_download_subtitle),
                    leadingIcon = Icons.AutoMirrored.Outlined.PlaylistAddCheck,
                    onClick = { viewModel.updateConfigureBeforeDownload(!viewModel.configureBeforeDownload) },
                    trailingContent = { Switch(checked = viewModel.configureBeforeDownload, onCheckedChange = null) },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_save_thumbnail_file),
                    subtitle = stringResource(R.string.settings_save_thumbnail_file_subtitle),
                    leadingIcon = Icons.Outlined.Image,
                    onClick = { viewModel.updateSaveThumbnailFile(!viewModel.saveThumbnailFile) },
                    trailingContent = { Switch(checked = viewModel.saveThumbnailFile, onCheckedChange = null) },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_incognito),
                    subtitle = stringResource(R.string.settings_incognito_subtitle),
                    leadingIcon = Icons.Filled.VisibilityOff,
                    onClick = { viewModel.updateIncognito(!viewModel.incognito) },
                    trailingContent = { Switch(checked = viewModel.incognito, onCheckedChange = null) },
                )
            }
        }
    }
}

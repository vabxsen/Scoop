package com.scoop.app.ui.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scoop.app.R
import com.scoop.app.core.update.UpdateCheckState
import com.scoop.app.ui.common.SettingHubRow
import com.scoop.app.util.FileShareUtils
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    onBack: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenVideoAudio: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val updateState = viewModel.updateState

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateCheckState.UpToDate -> {
                snackbarHostState.showSnackbar(context.getString(R.string.update_up_to_date))
                viewModel.consumeUpdateState()
            }
            is UpdateCheckState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.consumeUpdateState()
            }
            is UpdateCheckState.ReadyToInstall -> {
                FileShareUtils.installApk(context, state.filePath)
                viewModel.consumeUpdateState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn {
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

            ExtendedFloatingActionButton(
                onClick = viewModel::checkForUpdate,
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                icon = {
                    when (val state = updateState) {
                        is UpdateCheckState.Checking ->
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        is UpdateCheckState.Downloading ->
                            CircularProgressIndicator(progress = { state.progress }, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else ->
                            Icon(Icons.Outlined.Update, contentDescription = null)
                    }
                },
                text = {
                    val label =
                        when (updateState) {
                            is UpdateCheckState.Checking -> stringResource(R.string.update_checking)
                            is UpdateCheckState.Downloading -> stringResource(R.string.update_downloading)
                            else -> stringResource(R.string.action_check_for_update)
                        }
                    Text(label)
                },
            )
        }
    }
}

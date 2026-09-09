package com.scoop.app.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.scoop.app.R
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.downloader.DownloadPaths
import com.scoop.app.ui.common.SettingHubRow
import com.scoop.app.ui.common.SettingsScreenTitle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsImagesScreen(
    onBack: () -> Unit,
    onOpenStorage: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var saveFolder by remember { mutableStateOf(DownloadPaths.displayLabel(DownloadKind.IMAGE)) }
    LifecycleResumeEffect(context) {
        saveFolder = DownloadPaths.customFolderUri(context)?.let { DownloadPaths.customFolderLabel(context, it) }
            ?: DownloadPaths.displayLabel(DownloadKind.IMAGE)
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { SettingsScreenTitle(stringResource(R.string.mode_images)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_images_select_all),
                    subtitle = stringResource(R.string.settings_images_select_all_subtitle),
                    leadingIcon = Icons.Filled.SelectAll,
                    onClick = { viewModel.updateSelectAllGalleryImages(!viewModel.selectAllGalleryImages) },
                    trailingContent = { Switch(checked = viewModel.selectAllGalleryImages, onCheckedChange = null) },
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_images_quality),
                    subtitle = stringResource(R.string.settings_images_quality_subtitle),
                    leadingIcon = Icons.Filled.HighQuality,
                )
            }
            item {
                SettingHubRow(
                    title = stringResource(R.string.settings_save_folder),
                    subtitle = stringResource(R.string.settings_images_folder_subtitle, saveFolder),
                    leadingIcon = Icons.Filled.Folder,
                    onClick = onOpenStorage,
                )
            }
        }
    }
}

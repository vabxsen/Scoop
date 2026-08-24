package com.scoop.app.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import com.scoop.app.R
import com.scoop.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    startUrl: String? = null,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(startUrl) {
        if (!startUrl.isNullOrBlank()) viewModel.onUrlChange(startUrl)
    }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize().padding(Spacing.md)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.nav_settings))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onOpenDownloads) {
                        Icon(Icons.Outlined.VideoLibrary, contentDescription = stringResource(R.string.nav_downloads))
                    }
                }

                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(top = Spacing.lg),
                )

                OutlinedTextField(
                    value = viewModel.url,
                    onValueChange = viewModel::onUrlChange,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
                    placeholder = { Text(stringResource(R.string.url_input_placeholder)) },
                    singleLine = true,
                    trailingIcon = {
                        if (viewModel.url.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onUrlChange("") }) {
                                Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.action_clear))
                            }
                        }
                    },
                )
            }

            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FloatingActionButton(
                    onClick = { clipboardManager.getText()?.text?.let { viewModel.onUrlChange(it) } }
                ) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = stringResource(R.string.action_paste))
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                FloatingActionButton(onClick = viewModel::startDownloadFlow) {
                    Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.action_download))
                }
            }
        }
    }

    if (viewModel.configureState != ConfigureUiState.Hidden) {
        ConfigureDownloadSheet(viewModel = viewModel, onDismiss = viewModel::dismissConfigureSheet)
    }
}

package com.scoop.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.scoop.app.R
import com.scoop.app.core.model.DownloadKind
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(startUrl: String? = null, viewModel: HomeViewModel = koinViewModel()) {
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(startUrl) {
        if (!startUrl.isNullOrBlank()) viewModel.onUrlChange(startUrl)
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.home_title), style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = viewModel.url,
                onValueChange = viewModel::onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.url_input_placeholder)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            clipboardManager.getText()?.text?.let { viewModel.onUrlChange(it) }
                        }
                    ) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = stringResource(R.string.action_paste))
                    }
                },
            )

            Button(onClick = viewModel::analyze, modifier = Modifier.fillMaxWidth(), enabled = viewModel.url.isNotBlank()) {
                Text(stringResource(R.string.action_analyze))
            }

            when (val state = viewModel.uiState) {
                is AnalyzeUiState.Idle -> {
                    Text(stringResource(R.string.recent_empty), style = MaterialTheme.typography.bodyMedium)
                }
                is AnalyzeUiState.Analyzing -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.analyzing))
                    }
                }
                is AnalyzeUiState.Error -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.analyze_error_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.analyze_error_body), style = MaterialTheme.typography.bodyMedium)
                            Text(state.message, style = MaterialTheme.typography.labelSmall)
                            OutlinedButton(onClick = viewModel::analyze) { Text(stringResource(R.string.action_retry)) }
                        }
                    }
                }
                is AnalyzeUiState.Success -> {
                    MediaInfoCard(state = state, onDownload = viewModel::download)
                }
            }
        }
    }
}

@Composable
private fun MediaInfoCard(state: AnalyzeUiState.Success, onDownload: (DownloadKind) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.info.thumbnailUrl != null) {
                    AsyncImage(
                        model = state.info.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp, 64.dp).clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    Box(modifier = Modifier.size(96.dp, 64.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.info.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    state.info.uploader?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onDownload(DownloadKind.VIDEO) }, modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Text("Video")
                }
                OutlinedButton(
                    onClick = { onDownload(DownloadKind.AUDIO_ONLY) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    Text("Audio")
                }
            }
        }
    }
}

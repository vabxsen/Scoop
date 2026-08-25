package com.scoop.app.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold(containerColor = Color(0xFFFDF8F4)) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFDF8F4)).padding(innerPadding)) {
            Image(
                painter = painterResource(R.drawable.home_background_dotted),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                alpha = 0.35f,
                modifier = Modifier.fillMaxSize(),
            )
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
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = Spacing.lg),
                )

                val urlFieldInteractionSource = remember { MutableInteractionSource() }
                val urlFieldFocused by urlFieldInteractionSource.collectIsFocusedAsState()
                val urlFieldBorderColor = if (urlFieldFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                val urlFieldBorderWidth = if (urlFieldFocused) 2.dp else 1.5.dp

                OutlinedTextField(
                    value = viewModel.url,
                    onValueChange = viewModel::onUrlChange,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(top = Spacing.lg)
                            .border(width = urlFieldBorderWidth, color = urlFieldBorderColor, shape = OutlinedTextFieldDefaults.shape),
                    placeholder = { Text(stringResource(R.string.url_input_placeholder)) },
                    singleLine = true,
                    interactionSource = urlFieldInteractionSource,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
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
                horizontalAlignment = Alignment.End,
            ) {
                SmallFloatingActionButton(
                    onClick = { clipboardManager.getText()?.text?.let { viewModel.onUrlChange(it) } },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 1.dp, pressedElevation = 1.dp),
                ) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = stringResource(R.string.action_paste))
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                ExtendedFloatingActionButton(
                    onClick = viewModel::startDownloadFlow,
                    icon = { Icon(Icons.Filled.Download, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_download)) },
                )
            }
        }
    }

    if (viewModel.configureState != ConfigureUiState.Hidden) {
        ConfigureDownloadSheet(viewModel = viewModel, onDismiss = viewModel::dismissConfigureSheet)
    }
}

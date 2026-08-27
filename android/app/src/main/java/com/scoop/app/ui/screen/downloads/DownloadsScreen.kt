package com.scoop.app.ui.screen.downloads

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.scoop.app.R
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.ui.common.DownloadCard
import com.scoop.app.ui.common.EmptyDownloadsIllustration
import com.scoop.app.ui.common.SettingsScreenTitle
import com.scoop.app.ui.theme.Motion
import com.scoop.app.ui.theme.Spacing
import com.scoop.app.util.FileShareUtils
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(onBack: () -> Unit, onOpenDownload: (String) -> Unit, viewModel: DownloadsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var filter by remember { mutableStateOf(DownloadFilter.ALL) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    val entries = viewModel.tasks.entries.filter { viewModel.matches(it.value, filter) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsScreenTitle(stringResource(R.string.nav_downloads)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                actions = {
                    if (viewModel.tasks.isNotEmpty()) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.downloads_clear_all))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        Crossfade(targetState = viewModel.tasks.isEmpty(), animationSpec = tween(Motion.EMPHASIZED_MS), label = "downloadsEmptyState") { isEmpty ->
            if (isEmpty) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyDownloadsIllustration()
                        Text(
                            stringResource(R.string.downloads_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = Spacing.md),
                        )
                        Text(
                            stringResource(R.string.downloads_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = Spacing.xs, start = Spacing.lg, end = Spacing.lg),
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding), contentPadding = PaddingValues(Spacing.md)) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            modifier = Modifier.padding(bottom = Spacing.md),
                        ) {
                            DownloadFilterChip(DownloadFilter.ALL, filter, stringResource(R.string.filter_all)) { filter = it }
                            DownloadFilterChip(DownloadFilter.DOWNLOADING, filter, stringResource(R.string.filter_downloading)) { filter = it }
                            DownloadFilterChip(DownloadFilter.COMPLETED, filter, stringResource(R.string.filter_completed)) { filter = it }
                            DownloadFilterChip(DownloadFilter.FAILED, filter, stringResource(R.string.filter_failed)) { filter = it }
                        }
                    }
                    items(entries, key = { it.key.id }) { (task, status) ->
                        DownloadCard(
                            task = task,
                            status = status,
                            onClick = { onOpenDownload(task.id) },
                            onPrimaryAction = {
                                if (status is DownloadStatus.Completed) {
                                    status.filePath?.let { FileShareUtils.openFile(context, it) }
                                } else {
                                    viewModel.primaryAction(task.id, status)
                                }
                            },
                            modifier = Modifier.animateItem(tween(Motion.STANDARD_MS)).padding(bottom = Spacing.sm),
                        )
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.downloads_clear_all_title)) },
            text = { Text(stringResource(R.string.downloads_clear_all_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearAllDialog = false
                    }
                ) {
                    Text(stringResource(R.string.downloads_clear_all), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearAllDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun DownloadFilterChip(value: DownloadFilter, selected: DownloadFilter, label: String, onSelect: (DownloadFilter) -> Unit) {
    FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
}

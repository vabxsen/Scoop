package com.scoop.app.ui.screen.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scoop.app.R
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.ui.common.DownloadCard
import com.scoop.app.ui.common.EmptyDownloadsIllustration
import com.scoop.app.ui.common.SettingsScreenTitle
import com.scoop.app.ui.theme.Motion
import com.scoop.app.ui.theme.Spacing
import com.scoop.app.util.FileShareUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

private const val DAY_MS = 86_400_000L

private fun startOfDay(timeMillis: Long): Long =
    Calendar.getInstance()
        .apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        .timeInMillis

/** Buckets a timestamp into a scannable group header - "Today"/"Yesterday" for the last two days,
 * a weekday name for the rest of the current week, then a plain month/day for anything older. */
private fun dateGroupLabel(createdAt: Long): String {
    val diffDays = (startOfDay(System.currentTimeMillis()) - startOfDay(createdAt)) / DAY_MS
    return when {
        diffDays <= 0L -> "Today"
        diffDays == 1L -> "Yesterday"
        diffDays in 2..6 -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(createdAt))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(createdAt))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DownloadsScreen(onBack: () -> Unit, onOpenDownload: (String) -> Unit, viewModel: DownloadsViewModel = koinViewModel()) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var filter by remember { mutableStateOf(DownloadFilter.ALL) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    val pendingDeleteIds = viewModel.pendingDeleteIds
    // Derives only the task list/order/grouping - never DownloadStatus - so a progress tick that
    // doesn't add/remove/reclassify a task produces an equal Map and skips recomposing the whole
    // list. Each row reads its own live status separately (see DownloadHistoryRow) instead of
    // getting it baked into this derivation, which would defeat the point.
    val groupedTasks by
        remember(filter) {
            derivedStateOf {
                viewModel.tasks.entries
                    .filter { viewModel.matches(it.value, filter) && it.key.id !in pendingDeleteIds }
                    .map { it.key }
                    .sortedByDescending { it.createdAt }
                    .groupBy { dateGroupLabel(it.createdAt) }
            }
        }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            AnimatedVisibility(
                visible = pendingDeleteIds.isNotEmpty(),
                enter = fadeIn(tween(Motion.QUICK_MS)) + slideInVertically(tween(Motion.QUICK_MS)) { it },
                exit = fadeOut(tween(Motion.QUICK_MS)) + slideOutVertically(tween(Motion.QUICK_MS)) { it },
            ) {
                Snackbar(
                    modifier = Modifier.padding(Spacing.md),
                    action = {
                        TextButton(onClick = viewModel::undoDeletes) {
                            Text(
                                stringResource(R.string.action_undo),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                            )
                        }
                    },
                ) {
                    Text(stringResource(R.string.downloads_removed))
                }
            }
        },
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
                    groupedTasks.forEach { (groupLabel, groupTasksForLabel) ->
                        stickyHeader(key = groupLabel) {
                            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                                Text(
                                    groupLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = Spacing.sm),
                                )
                            }
                        }
                        items(groupTasksForLabel, key = { it.id }) { task ->
                            DownloadHistoryRow(
                                task = task,
                                tasks = viewModel.tasks,
                                onOpen = { onOpenDownload(task.id) },
                                onPrimaryAction = { status ->
                                    if (status is DownloadStatus.Completed) {
                                        status.filePath?.let { FileShareUtils.openFile(context, it) }
                                    } else {
                                        viewModel.primaryAction(task.id, status)
                                    }
                                },
                                onRequestDelete = { viewModel.requestDelete(task.id) },
                            )
                        }
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

/** Reads its own live status from [tasks] rather than taking one as a parameter - this scopes the
 * SnapshotStateMap read to just this row's own recomposition, so a progress tick on one task only
 * recomposes its own row instead of the whole list (see [groupedTasks] above, which deliberately
 * excludes DownloadStatus from what it derives). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LazyItemScope.DownloadHistoryRow(
    task: DownloadTask,
    tasks: Map<DownloadTask, DownloadStatus>,
    onOpen: () -> Unit,
    onPrimaryAction: (DownloadStatus) -> Unit,
    onRequestDelete: () -> Unit,
) {
    val status = tasks[task] ?: return
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value != SwipeToDismissBoxValue.Settled) onRequestDelete()
                true
            }
        )
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.animateItem(tween(Motion.STANDARD_MS)).padding(bottom = Spacing.sm),
        backgroundContent = { DeleteSwipeBackground(dismissState) },
    ) {
        DownloadCard(task = task, status = status, onClick = onOpen, onPrimaryAction = { onPrimaryAction(status) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteSwipeBackground(dismissState: SwipeToDismissBoxState) {
    val alignment =
        when (dismissState.dismissDirection) {
            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            SwipeToDismissBoxValue.Settled -> Alignment.Center
        }
    Box(
        modifier =
            Modifier.fillMaxSize()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = Spacing.lg),
        contentAlignment = alignment,
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.onErrorContainer)
    }
}

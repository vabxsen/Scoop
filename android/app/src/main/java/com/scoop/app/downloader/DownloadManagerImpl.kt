package com.scoop.app.downloader

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.extractor.MediaExtractor
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_CONCURRENCY = 3

class DownloadManagerImpl(
    private val extractor: MediaExtractor,
    private val appContext: Context,
) : DownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableMapOf<String, Job>()

    override val tasks: SnapshotStateMap<DownloadTask, DownloadStatus> = mutableStateMapOf()

    init {
        val stateFlow = snapshotFlow { tasks.toMap() }
        // Runs on every state change so a freshly queued task is picked up even when the
        // concurrently-running count itself hasn't moved (e.g. going from 0 running to 0 running
        // because the new task is still Queued).
        scope.launch { stateFlow.collect { dispatchNext() } }
        scope.launch {
            stateFlow
                .map { it.values.count { s -> s is DownloadStatus.Analyzing || s is DownloadStatus.Downloading } }
                .distinctUntilChanged()
                .collect { runningCount ->
                    if (runningCount > 0) DownloadService.start(appContext) else DownloadService.stop(appContext)
                }
        }
    }

    override fun enqueue(request: DownloadRequest, title: String, thumbnailUrl: String?): DownloadTask {
        val task = DownloadTask(id = UUID.randomUUID().toString(), request = request, title = title, thumbnailUrl = thumbnailUrl)
        tasks[task] = DownloadStatus.Queued
        return task
    }

    override fun cancel(taskId: String): Boolean {
        val task = tasks.keys.find { it.id == taskId } ?: return false
        YoutubeDL.destroyProcessById(taskId)
        jobs.remove(taskId)?.cancel()
        tasks[task] = DownloadStatus.Cancelled
        return true
    }

    override fun retry(taskId: String) {
        val task = tasks.keys.find { it.id == taskId } ?: return
        val status = tasks[task]
        if (status is DownloadStatus.Failed || status is DownloadStatus.Cancelled) {
            tasks[task] = DownloadStatus.Queued
        }
    }

    override fun remove(taskId: String): Boolean {
        val task = tasks.keys.find { it.id == taskId } ?: return false
        cancel(taskId)
        tasks.remove(task)
        return true
    }

    private fun dispatchNext() {
        val runningCount = tasks.values.count { it is DownloadStatus.Analyzing || it is DownloadStatus.Downloading }
        if (runningCount >= MAX_CONCURRENCY) return
        val (task, _) = tasks.entries.firstOrNull { it.value is DownloadStatus.Queued } ?: return
        runTask(task)
    }

    private fun runTask(task: DownloadTask) {
        tasks[task] = DownloadStatus.Analyzing
        jobs[task.id] =
            scope.launch {
                extractor
                    .analyze(task.request.url)
                    .onSuccess { info ->
                        tasks[task] = DownloadStatus.Downloading()
                        executeDownload(task)
                            .onSuccess { filePath -> tasks[task] = DownloadStatus.Completed(filePath) }
                            .onFailure { error -> tasks[task] = DownloadStatus.Failed(error.message ?: "Download failed", error) }
                    }
                    .onFailure { error -> tasks[task] = DownloadStatus.Failed(error.message ?: "Analysis failed", error) }
                jobs.remove(task.id)
            }
    }

    private suspend fun executeDownload(task: DownloadTask): Result<String?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val outputDir =
                    (when (task.request.kind) {
                        DownloadKind.VIDEO -> appContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                        DownloadKind.AUDIO_ONLY -> appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                    } ?: appContext.filesDir).apply { mkdirs() }

                val request =
                    YoutubeDLRequest(task.request.url).apply {
                        addOption("--no-mtime")
                        addOption("--no-playlist")
                        addOption("-o", File(outputDir, "%(title)s.%(ext)s").absolutePath)
                        when (task.request.kind) {
                            DownloadKind.VIDEO -> {
                                addOption("-f", task.request.formatId ?: "bestvideo*+bestaudio/best")
                                addOption("--merge-output-format", "mp4")
                            }
                            DownloadKind.AUDIO_ONLY -> {
                                addOption("-x")
                                addOption("--audio-format", task.request.audioContainer ?: "mp3")
                            }
                        }
                    }

                YoutubeDL.getInstance().execute(request, task.id) { progress, _, _ ->
                    val current = tasks[task] as? DownloadStatus.Downloading ?: DownloadStatus.Downloading()
                    tasks[task] = current.copy(progress = progress / 100f)
                }

                null
            }
        }
}

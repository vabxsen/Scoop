package com.scoop.app.downloader

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.scoop.app.core.database.DownloadHistoryDao
import com.scoop.app.core.database.objects.DownloadedItem
import com.scoop.app.core.model.AudioQuality
import com.scoop.app.core.model.DefaultVideoContainer
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.core.media.MediaEngineReadiness
import com.scoop.app.extractor.MediaExtractor
import com.scoop.app.util.NetworkUtils
import com.scoop.app.util.PrefKeys
import com.scoop.app.util.PreferenceUtil
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

private const val DEFAULT_MAX_CONCURRENCY = 3

class DownloadManagerImpl(
    private val extractor: MediaExtractor,
    private val appContext: Context,
    private val downloadHistoryDao: DownloadHistoryDao,
    private val mediaEngineReadiness: MediaEngineReadiness,
) : DownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableMapOf<String, Job>()

    override val tasks: SnapshotStateMap<DownloadTask, DownloadStatus> = mutableStateMapOf()

    init {
        DownloadPaths.sweepStaleTempWorkspaces(appContext)

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

        // Re-check the queue whenever the network changes so downloads held back by Wi-Fi-only
        // resume automatically the moment Wi-Fi becomes available, instead of staying stuck until
        // the user reopens the app.
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivityManager?.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = dispatchNext()

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: android.net.NetworkCapabilities) = dispatchNext()
            }
        )
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

    override suspend fun deleteTaskAndFile(taskId: String) {
        val task = tasks.keys.find { it.id == taskId }
        val status = task?.let { tasks[it] }
        val filePath = (status as? DownloadStatus.Completed)?.filePath
        withContext(Dispatchers.IO) {
            if (filePath != null) File(filePath).delete()
            downloadHistoryDao.deleteById(taskId)
        }
        if (task != null) {
            cancel(taskId)
            tasks.remove(task)
        }
    }

    private fun dispatchNext() {
        val maxConcurrency = PreferenceUtil.getInt(PrefKeys.MAX_CONCURRENT_DOWNLOADS, DEFAULT_MAX_CONCURRENCY)
        val runningCount = tasks.values.count { it is DownloadStatus.Analyzing || it is DownloadStatus.Downloading }
        if (runningCount >= maxConcurrency) return
        if (PreferenceUtil.getBoolean(PrefKeys.WIFI_ONLY_DOWNLOADS, false) && !NetworkUtils.isOnWifi(appContext)) return
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
                            .onSuccess { filePath ->
                                tasks[task] = DownloadStatus.Completed(filePath)
                                if (filePath != null) {
                                    downloadHistoryDao.upsert(
                                        DownloadedItem(
                                            id = task.id,
                                            sourceUrl = task.request.url,
                                            title = task.title,
                                            filePath = filePath,
                                            thumbnailUrl = task.thumbnailUrl,
                                            kind = task.request.kind.name,
                                            createdAt = task.createdAt,
                                        )
                                    )
                                } else {
                                    // A yt-dlp run that "succeeds" without a resolvable output path
                                    // isn't a usable completed download - surface it as a failure
                                    // rather than a broken/blank Completed state.
                                    tasks[task] = DownloadStatus.Failed("Download finished but the output file could not be located")
                                }
                            }
                            .onFailure { error -> tasks[task] = DownloadStatus.Failed(error.message ?: "Download failed", error) }
                    }
                    .onFailure { error -> tasks[task] = DownloadStatus.Failed(error.message ?: "Analysis failed", error) }
                jobs.remove(task.id)
            }
    }

    private suspend fun executeDownload(task: DownloadTask): Result<String?> =
        withContext(Dispatchers.IO) {
            runCatching {
                mediaEngineReadiness.awaitReady()
                val tempDir = DownloadPaths.tempWorkspace(appContext, task.id)

                val request =
                    YoutubeDLRequest(task.request.url).apply {
                        addOption("--no-mtime")
                        addOption("--no-playlist")
                        addOption("-o", File(tempDir, "%(title)s.%(ext)s").absolutePath)
                        addOption("--print", "after_move:filepath")
                        when (task.request.kind) {
                            DownloadKind.VIDEO -> {
                                addOption("-f", task.request.formatId ?: "bestvideo*+bestaudio/best")
                                val container = PreferenceUtil.getString(PrefKeys.DEFAULT_VIDEO_CONTAINER, DefaultVideoContainer.MP4.name)
                                val containerValue = DefaultVideoContainer.entries.firstOrNull { it.name == container }?.ytDlpValue ?: "mp4"
                                addOption("--merge-output-format", containerValue)
                            }
                            DownloadKind.AUDIO_ONLY -> {
                                addOption("-f", task.request.formatId ?: "bestaudio/best")
                                addOption("-x")
                                addOption("--audio-format", task.request.audioContainer ?: "mp3")
                                val quality = PreferenceUtil.getString(PrefKeys.AUDIO_QUALITY, AudioQuality.BEST.name)
                                val qualityValue = AudioQuality.entries.firstOrNull { it.name == quality }?.ytDlpValue ?: "0"
                                addOption("--audio-quality", qualityValue)
                            }
                        }
                    }

                val response =
                    YoutubeDL.getInstance().execute(request, task.id) { progress, eta, _ ->
                        val current = tasks[task] as? DownloadStatus.Downloading ?: DownloadStatus.Downloading()
                        tasks[task] = current.copy(progress = progress / 100f, etaSeconds = eta.toInt())
                    }

                // `--print after_move:filepath` writes the final resolved path as its own stdout
                // line, independent of the progress-line regex the callback above matches against
                // - response.out captures full stdout regardless, so read it directly rather than
                // relying on that line reaching the callback.
                val printedPath =
                    response.out
                        .lineSequence()
                        .map { it.trim() }
                        .lastOrNull { it.isNotEmpty() && File(it).exists() }

                val movedFile =
                    printedPath?.let { path ->
                        val sourceFile = File(path)
                        DownloadPaths.moveWithDedup(
                            source = sourceFile,
                            targetDir = DownloadPaths.outputDir(appContext, task.request.kind),
                            desiredName = sourceFile.name,
                        )
                    }
                DownloadPaths.clearTempWorkspace(appContext, task.id)

                movedFile?.absolutePath
            }
        }
}

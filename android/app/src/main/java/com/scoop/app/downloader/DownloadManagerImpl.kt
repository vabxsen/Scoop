package com.scoop.app.downloader

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scoop.app.R
import com.scoop.app.core.database.DownloadHistoryDao
import com.scoop.app.core.database.objects.DownloadedItem
import com.scoop.app.core.model.AudioQuality
import com.scoop.app.core.model.AutoRetryPolicy
import com.scoop.app.core.model.DefaultVideoContainer
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.DownloadSpeedLimit
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.core.media.MediaEngineReadiness
import com.scoop.app.extractor.MediaExtractor
import com.scoop.app.extractor.YOUTUBE_PLAYER_CLIENT_ARG
import com.scoop.app.util.DownloadGate
import com.scoop.app.util.FileShareUtils
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_MAX_CONCURRENCY = 3
private const val RETRY_BACKOFF_BASE_MS = 8_000L
private const val DELETE_UNDO_WINDOW_MS = 2_000L

class DownloadManagerImpl(
    private val extractor: MediaExtractor,
    private val appContext: Context,
    private val downloadHistoryDao: DownloadHistoryDao,
    private val mediaEngineReadiness: MediaEngineReadiness,
) : DownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableMapOf<String, Job>()
    private val retryAttempts = mutableMapOf<String, Int>()
    private val pendingDeleteJobs = mutableMapOf<String, Job>()

    override val tasks: SnapshotStateMap<DownloadTask, DownloadStatus> = mutableStateMapOf()
    override val pendingDeleteIds: SnapshotStateList<String> = mutableStateListOf()

    init {
        DownloadPaths.sweepStaleTempWorkspaces(appContext)
        hydrateFromHistory()

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

        // Same idea for the battery-pause gate: re-check on every level/charge-state change so a
        // download held back by low battery resumes the moment the level rises or a charger is
        // plugged in.
        val batteryFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
        ContextCompat.registerReceiver(
            appContext,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) = dispatchNext()
            },
            batteryFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    /** The task map is purely in-memory, so a fresh process starts with an empty Downloads screen
     * unless past completions are re-loaded from the Room history table here. */
    private fun hydrateFromHistory() {
        scope.launch {
            val items = withContext(Dispatchers.IO) { downloadHistoryDao.getAll() }
            items.forEach { item ->
                val kind = DownloadKind.entries.firstOrNull { it.name == item.kind } ?: return@forEach
                val task =
                    DownloadTask(
                        id = item.id,
                        request = DownloadRequest(url = item.sourceUrl, kind = kind),
                        title = item.title,
                        thumbnailUrl = item.thumbnailUrl,
                        createdAt = item.createdAt,
                    )
                tasks[task] = DownloadStatus.Completed(item.filePath)
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
        retryAttempts.remove(taskId)
        tasks[task] = DownloadStatus.Cancelled
        return true
    }

    override fun retry(taskId: String) {
        val task = tasks.keys.find { it.id == taskId } ?: return
        val status = tasks[task]
        if (status is DownloadStatus.Failed || status is DownloadStatus.Cancelled) {
            // A manual retry is the user explicitly asking again, so it gets a fresh auto-retry
            // budget rather than inheriting whatever the automatic attempts already used up.
            retryAttempts.remove(taskId)
            tasks[task] = DownloadStatus.Queued
        }
    }

    override suspend fun deleteTaskAndFile(taskId: String) {
        val task = tasks.keys.find { it.id == taskId }
        val status = task?.let { tasks[it] }
        val filePath = (status as? DownloadStatus.Completed)?.filePath
        withContext(Dispatchers.IO) {
            if (filePath != null) deleteFile(filePath)
            downloadHistoryDao.deleteById(taskId)
        }
        if (task != null) {
            cancel(taskId)
            tasks.remove(task)
        }
    }

    override fun requestDelete(taskId: String) {
        if (taskId in pendingDeleteIds) return
        pendingDeleteIds.add(taskId)
        pendingDeleteJobs[taskId] =
            scope.launch {
                delay(DELETE_UNDO_WINDOW_MS)
                pendingDeleteJobs.remove(taskId)
                pendingDeleteIds.remove(taskId)
                deleteTaskAndFile(taskId)
            }
    }

    override fun undoDelete(taskId: String) {
        pendingDeleteJobs.remove(taskId)?.cancel()
        pendingDeleteIds.remove(taskId)
    }

    override fun undoAllDeletes() {
        pendingDeleteJobs.values.forEach { it.cancel() }
        pendingDeleteJobs.clear()
        pendingDeleteIds.clear()
    }

    override suspend fun clearHistoryOlderThan(days: Int) {
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        withContext(Dispatchers.IO) {
            downloadHistoryDao.getOlderThan(cutoff).forEach { item ->
                item.filePath?.let { deleteFile(it) }
                downloadHistoryDao.deleteById(item.id)
            }
        }
    }

    override suspend fun clearAll() {
        tasks.keys.toList().forEach { task ->
            YoutubeDL.destroyProcessById(task.id)
            jobs.remove(task.id)?.cancel()
        }
        retryAttempts.clear()
        withContext(Dispatchers.IO) {
            downloadHistoryDao.getAll().forEach { item -> item.filePath?.let { deleteFile(it) } }
            downloadHistoryDao.deleteAll()
        }
        tasks.clear()
    }

    private fun deleteFile(filePath: String) {
        if (filePath.startsWith("content://")) {
            runCatching { appContext.contentResolver.delete(Uri.parse(filePath), null, null) }
        } else {
            File(filePath).delete()
        }
    }

    private fun dispatchNext() {
        val maxConcurrency = PreferenceUtil.getInt(PrefKeys.MAX_CONCURRENT_DOWNLOADS, DEFAULT_MAX_CONCURRENCY)
        val runningCount = tasks.values.count { it is DownloadStatus.Analyzing || it is DownloadStatus.Downloading }
        if (runningCount >= maxConcurrency) return
        if (DownloadGate.blockedReason(appContext) != null) return
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
                                if (filePath != null) {
                                    retryAttempts.remove(task.id)
                                    tasks[task] = DownloadStatus.Completed(filePath)
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
                                    notifyDownloadComplete(task, filePath)
                                } else {
                                    // A yt-dlp run that "succeeds" without a resolvable output path
                                    // isn't a usable completed download - surface it as a failure
                                    // rather than a broken/blank Completed state.
                                    handleFailure(task, "Download finished but the output file could not be located")
                                }
                            }
                            .onFailure { error -> handleFailure(task, error.message ?: "Download failed", error) }
                    }
                    .onFailure { error -> handleFailure(task, error.message ?: "Analysis failed", error) }
                jobs.remove(task.id)
            }
    }

    /** On failure, auto-retries with a linear backoff (attempt N waits N * 8s) up to the
     * configured policy's budget before finally surfacing DownloadStatus.Failed. */
    private suspend fun handleFailure(task: DownloadTask, message: String, error: Throwable? = null) {
        val policy =
            AutoRetryPolicy.entries.firstOrNull { it.name == PreferenceUtil.getString(PrefKeys.AUTO_RETRY_POLICY, AutoRetryPolicy.OFF.name) }
                ?: AutoRetryPolicy.OFF
        val attempt = (retryAttempts[task.id] ?: 0) + 1
        if (attempt <= policy.maxAttempts) {
            retryAttempts[task.id] = attempt
            delay(RETRY_BACKOFF_BASE_MS * attempt)
            tasks[task] = DownloadStatus.Queued
        } else {
            retryAttempts.remove(task.id)
            tasks[task] = DownloadStatus.Failed(message, error)
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
                        addOption("--extractor-args", YOUTUBE_PLAYER_CLIENT_ARG)
                        val speedLimit =
                            DownloadSpeedLimit.entries.firstOrNull { it.name == PreferenceUtil.getString(PrefKeys.DOWNLOAD_SPEED_LIMIT, DownloadSpeedLimit.UNLIMITED.name) }
                                ?.ytDlpValue
                        if (speedLimit != null) addOption("--limit-rate", speedLimit)
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

                val savedLocation =
                    printedPath?.let { path ->
                        val sourceFile = File(path)
                        DownloadPaths.saveToCustomFolder(appContext, sourceFile, sourceFile.name)
                            ?: DownloadPaths.publishToMediaStore(appContext, task.request.kind, sourceFile, sourceFile.name)
                            ?: DownloadPaths
                                .moveWithDedup(
                                    source = sourceFile,
                                    targetDir = DownloadPaths.outputDir(appContext, task.request.kind),
                                    desiredName = sourceFile.name,
                                )
                                .absolutePath
                    }
                DownloadPaths.clearTempWorkspace(appContext, task.id)

                savedLocation
            }
        }

    /** One-shot "Download complete" notification per finished task, separate from the ongoing
     * foreground-service notification - tapping it opens the downloaded file. */
    private fun notifyDownloadComplete(task: DownloadTask, filePath: String) {
        val openIntent = FileShareUtils.openFileIntent(appContext, filePath)
        val pendingIntent =
            PendingIntent.getActivity(appContext, task.id.hashCode(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification =
            NotificationCompat.Builder(appContext, DownloadService.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(appContext.getString(R.string.notification_download_complete_title))
                .setContentText(task.title)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        runCatching { NotificationManagerCompat.from(appContext).notify(task.id.hashCode(), notification) }
    }
}

package com.scoop.app.downloader

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.scoop.app.core.model.FormatSelector
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.DownloadSpeedLimit
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.core.network.SecureUrl
import com.scoop.app.core.network.PublicHttpsProxy
import com.scoop.app.core.media.MediaEngineReadiness
import com.scoop.app.extractor.MediaExtractor
import com.scoop.app.util.DownloadGate
import com.scoop.app.util.FileShareUtils
import com.scoop.app.util.PrefKeys
import com.scoop.app.util.PreferenceUtil
import com.scoop.app.util.tokenizeShellArgs
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DEFAULT_MAX_CONCURRENCY = 3
private const val RETRY_BACKOFF_BASE_MS = 8_000L
private const val DELETE_UNDO_WINDOW_MS = 2_000L
private val THUMBNAIL_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

class DownloadManagerImpl(
    private val extractor: MediaExtractor,
    private val appContext: Context,
    private val downloadHistoryDao: DownloadHistoryDao,
    private val mediaEngineReadiness: MediaEngineReadiness,
    private val imageDownloader: ImageDownloader,
    private val queueStore: DownloadQueueStore,
) : DownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = ConcurrentHashMap<String, Job>()
    @Volatile private var clearingAll = false
    private val retryAttempts = ConcurrentHashMap<String, Int>()
    private val pendingDeleteJobs = ConcurrentHashMap<String, Job>()
    @Volatile private var executionAuthorized = false
    private val queueWriteRevision = AtomicLong()
    private val queueWriteMutex = Mutex()

    override val tasks: SnapshotStateMap<DownloadTask, DownloadStatus> = mutableStateMapOf()
    override var isInitialized by mutableStateOf(false)
        private set
    override val pendingDeleteIds: SnapshotStateList<String> = mutableStateListOf()

    init {
        DownloadPaths.sweepStaleTempWorkspaces(appContext)
        initializeState()

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

    private fun initializeState() {
        scope.launch {
            val (savedQueue, items) = withContext(Dispatchers.IO) {
                val retention =
                    com.scoop.app.core.model.HistoryRetention.entries.firstOrNull {
                        it.name == PreferenceUtil.getString(PrefKeys.HISTORY_RETENTION, com.scoop.app.core.model.HistoryRetention.OFF.name)
                    } ?: com.scoop.app.core.model.HistoryRetention.OFF
                retention.days?.let { clearHistoryRecordsOlderThan(it) }
                queueStore.read() to downloadHistoryDao.getAll()
            }
            savedQueue.forEach { entry ->
                val task = entry.task
                retryAttempts[task.id] = entry.retryAttempt
                tasks[task] =
                    if (entry.requiresReanalysis) {
                        DownloadStatus.Failed("This authenticated image must be analyzed again after the app restarted")
                    } else {
                        DownloadStatus.Queued
                    }
            }
            items.forEach { item ->
                if (tasks.keys.any { it.id == item.id }) return@forEach
                val kind = DownloadKind.entries.firstOrNull { it.name == item.kind } ?: return@forEach
                val task =
                    DownloadTask(
                        id = item.id,
                        request = DownloadRequest(url = item.sourceUrl, kind = kind, playlistTitle = item.playlistTitle),
                        title = item.title,
                        thumbnailUrl = item.thumbnailUrl,
                        createdAt = item.createdAt,
                    )
                tasks[task] = DownloadStatus.Completed(item.filePath)
            }
            isInitialized = true
            if (executionAuthorized) resumePendingDownloads()
        }
    }

    override fun enqueue(request: DownloadRequest, title: String, thumbnailUrl: String?): DownloadTask {
        val capturedRequest = request.copy(saveToHistory = !PreferenceUtil.getBoolean(PrefKeys.INCOGNITO, false))
        val task = DownloadTask(id = UUID.randomUUID().toString(), request = capturedRequest, title = title, thumbnailUrl = thumbnailUrl)
        tasks[task] = DownloadStatus.Queued
        runBlocking { persistQueueNow() }
        executionAuthorized = true
        DownloadService.start(appContext)
        dispatchNext()
        return task
    }

    override fun cancel(taskId: String): Boolean {
        val task = tasks.keys.find { it.id == taskId } ?: return false
        YoutubeDL.destroyProcessById(taskId)
        jobs.remove(taskId)?.cancel()
        retryAttempts.remove(taskId)
        tasks[task] = DownloadStatus.Cancelled
        runBlocking { persistQueueNow() }
        return true
    }

    override fun refreshQueue() = dispatchNext()

    override fun resumePendingDownloads() {
        executionAuthorized = true
        val hasActive = tasks.values.any { it is DownloadStatus.Analyzing || it is DownloadStatus.Downloading || it is DownloadStatus.Processing }
        if (!hasActive && tasks.values.any { it is DownloadStatus.Queued }) {
            DownloadService.start(appContext)
        }
        dispatchNext()
    }

    override fun retry(taskId: String) {
        val task = tasks.keys.find { it.id == taskId } ?: return
        val status = tasks[task]
        if (status is DownloadStatus.Failed || status is DownloadStatus.Cancelled) {
            // A manual retry is the user explicitly asking again, so it gets a fresh auto-retry
            // budget rather than inheriting whatever the automatic attempts already used up.
            retryAttempts.remove(taskId)
            tasks[task] = DownloadStatus.Queued
            runBlocking { persistQueueNow() }
            resumePendingDownloads()
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
            persistQueueNow()
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
        val expiredIds = withContext(Dispatchers.IO) { clearHistoryRecordsOlderThan(days) }
        expiredIds.forEach(::undoDelete)
        tasks.keys.toList().filter { it.id in expiredIds }.forEach { tasks.remove(it) }
    }

    private suspend fun clearHistoryRecordsOlderThan(days: Int): Set<String> {
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        return downloadHistoryDao.getOlderThan(cutoff).map { item ->
            downloadHistoryDao.deleteById(item.id)
            item.id
        }.toSet()
    }

    override suspend fun clearAll() {
        clearingAll = true
        try {
            undoAllDeletes()
            val activeJobs = jobs.values.toList()
            tasks.keys.toList().forEach { task -> YoutubeDL.destroyProcessById(task.id) }
            activeJobs.forEach { it.cancel() }
            activeJobs.joinAll()
            jobs.clear()
            retryAttempts.clear()
            val sessionFiles = tasks.values.filterIsInstance<DownloadStatus.Completed>().mapNotNull { it.filePath }
            withContext(Dispatchers.IO) {
                val savedFiles = downloadHistoryDao.getAll().mapNotNull { it.filePath }
                (sessionFiles + savedFiles).distinct().forEach(::deleteFile)
                downloadHistoryDao.deleteAll()
            }
            tasks.clear()
            queueWriteRevision.incrementAndGet()
            withContext(Dispatchers.IO) {
                queueWriteMutex.withLock { queueStore.write(emptyList()) }
            }
        } finally {
            clearingAll = false
        }
    }

    private fun deleteFile(filePath: String) {
        if (filePath.startsWith("content://")) {
            runCatching { appContext.contentResolver.delete(Uri.parse(filePath), null, null) }
        } else {
            File(filePath).delete()
        }
    }

    @Synchronized
    private fun dispatchNext() {
        if (clearingAll || !executionAuthorized) return
        val maxConcurrency = PreferenceUtil.getInt(PrefKeys.MAX_CONCURRENT_DOWNLOADS, DEFAULT_MAX_CONCURRENCY)
        val runningCount = tasks.values.count { it is DownloadStatus.Analyzing || it is DownloadStatus.Downloading }
        if (runningCount >= maxConcurrency) return
        if (DownloadGate.blockedReason(appContext) != null) return
        val (task, _) = tasks.entries.firstOrNull { it.value is DownloadStatus.Queued } ?: return
        runTask(task)
    }

    private fun runTask(task: DownloadTask) {
        if (SecureUrl.parse(task.request.url) == null) {
            tasks[task] = DownloadStatus.Failed("Only public HTTPS links can be downloaded")
            runBlocking { persistQueueNow() }
            return
        }
        if (task.request.kind == DownloadKind.IMAGE) {
            runImageTask(task)
            return
        }
        tasks[task] = DownloadStatus.Analyzing
        val job = scope.launch {
            try {
                extractor
                    .analyze(task.request.url)
                    .onSuccess { info ->
                        tasks[task] = DownloadStatus.Downloading()
                        executeDownload(task)
                            .onSuccess { filePath ->
                                if (filePath != null) {
                                    retryAttempts.remove(task.id)
                                    // Incognito means "disable download history" - the completed task
                                    // still shows in this session's live queue via the in-memory `tasks`
                                    // map above, it just never gets persisted to survive a restart.
                                    if (task.request.saveToHistory) {
                                        downloadHistoryDao.upsert(
                                            DownloadedItem(
                                                id = task.id,
                                                sourceUrl = task.request.url,
                                                title = task.title,
                                                filePath = filePath,
                                                thumbnailUrl = task.thumbnailUrl,
                                                kind = task.request.kind.name,
                                                createdAt = task.createdAt,
                                                playlistTitle = task.request.playlistTitle,
                                            )
                                        )
                                    }
                                    persistQueueNow(queueSnapshot().filterNot { it.task.id == task.id })
                                    tasks[task] = DownloadStatus.Completed(filePath)
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
            } finally {
                if (jobs[task.id] == coroutineContext[Job]) jobs.remove(task.id)
                persistQueueNow()
                dispatchNext()
            }
        }
        jobs[task.id] = job
        dispatchNext()
    }

    private fun runImageTask(task: DownloadTask) {
        tasks[task] = DownloadStatus.Downloading()
        jobs[task.id] = scope.launch {
            try {
                val path = imageDownloader.download(task) { progress ->
                    if (tasks[task] is DownloadStatus.Downloading) tasks[task] = DownloadStatus.Downloading(progress)
                }
                if (tasks[task] is DownloadStatus.Cancelled) return@launch
                retryAttempts.remove(task.id)
                if (task.request.saveToHistory) {
                    downloadHistoryDao.upsert(DownloadedItem(
                        id = task.id, sourceUrl = task.request.url, title = task.title,
                        filePath = path, thumbnailUrl = path, kind = DownloadKind.IMAGE.name,
                        createdAt = task.createdAt, playlistTitle = task.request.playlistTitle,
                    ))
                }
                persistQueueNow(queueSnapshot().filterNot { it.task.id == task.id })
                tasks[task] = DownloadStatus.Completed(path)
                notifyDownloadComplete(task, path)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFailure(task, e.message ?: "Image download failed", e)
            } finally {
                if (jobs[task.id] == coroutineContext[Job]) jobs.remove(task.id)
                persistQueueNow()
                dispatchNext()
            }
        }
        dispatchNext()
    }

    /** On failure, auto-retries with a linear backoff (attempt N waits N * 8s) up to the
     * configured policy's budget before finally surfacing DownloadStatus.Failed. */
    private suspend fun handleFailure(task: DownloadTask, message: String, error: Throwable? = null) {
        currentCoroutineContext().ensureActive()
        if (error is CancellationException) throw error
        val policy =
            AutoRetryPolicy.entries.firstOrNull { it.name == PreferenceUtil.getString(PrefKeys.AUTO_RETRY_POLICY, AutoRetryPolicy.OFF.name) }
                ?: AutoRetryPolicy.OFF
        val attempt = (retryAttempts[task.id] ?: 0) + 1
        if (attempt <= policy.maxAttempts) {
            retryAttempts[task.id] = attempt
            persistQueue()
            delay(RETRY_BACKOFF_BASE_MS * attempt)
            tasks[task] = DownloadStatus.Queued
        } else {
            retryAttempts.remove(task.id)
            tasks[task] = DownloadStatus.Failed(message)
            persistQueueNow()
        }
    }

    private fun persistQueue() {
        val entries = queueSnapshot()
        val revision = queueWriteRevision.incrementAndGet()
        scope.launch(Dispatchers.IO) {
            queueWriteMutex.withLock {
                if (revision == queueWriteRevision.get()) runCatching { queueStore.write(entries) }
            }
        }
    }

    private fun queueSnapshot(): List<PersistedQueueEntry> =
        tasks.entries.mapNotNull { (task, status) ->
            if (status !is DownloadStatus.Queued && status !is DownloadStatus.Analyzing && status !is DownloadStatus.Downloading && status !is DownloadStatus.Processing) return@mapNotNull null
            if (!task.request.saveToHistory) return@mapNotNull null
            task.toPersistedQueueEntry(retryAttempts[task.id] ?: 0)
        }

    private suspend fun persistQueueNow(entries: List<PersistedQueueEntry> = queueSnapshot()) {
        val revision = queueWriteRevision.incrementAndGet()
        withContext(Dispatchers.IO) {
            queueWriteMutex.withLock {
                if (revision == queueWriteRevision.get()) queueStore.write(entries)
            }
        }
    }

    private suspend fun executeDownload(task: DownloadTask): Result<String?> =
        withContext(Dispatchers.IO) {
            val tempDir = DownloadPaths.tempWorkspace(appContext, task.id)
            try {
                runCatching {
                mediaEngineReadiness.awaitReady()

                val request =
                    YoutubeDLRequest(task.request.url).apply {
                        addOption("--no-mtime")
                        addOption("--no-playlist")
                        addOption("-o", File(tempDir, "%(title)s.%(ext)s").absolutePath)
                        addOption("--print", "after_move:filepath")
                        val speedLimit =
                            DownloadSpeedLimit.entries.firstOrNull { it.name == PreferenceUtil.getString(PrefKeys.DOWNLOAD_SPEED_LIMIT, DownloadSpeedLimit.UNLIMITED.name) }
                                ?.ytDlpValue
                        if (speedLimit != null) addOption("--limit-rate", speedLimit)
                        if (task.request.embedThumbnail) addOption("--embed-thumbnail")
                        if (PreferenceUtil.getBoolean(PrefKeys.SAVE_THUMBNAIL_FILE, false)) addOption("--write-thumbnail")
                        when (task.request.kind) {
                            DownloadKind.IMAGE -> error("Images use the image downloader")
                            DownloadKind.VIDEO -> {
                                addOption("-f", task.request.formatId ?: FormatSelector.video())
                                val container = PreferenceUtil.getString(PrefKeys.DEFAULT_VIDEO_CONTAINER, DefaultVideoContainer.MP4.name)
                                val containerValue = DefaultVideoContainer.entries.firstOrNull { it.name == container }?.ytDlpValue ?: "mp4"
                                addOption("--merge-output-format", containerValue)
                                // Embedding requires a video container to mux the subtitle track into,
                                // so this only applies for DownloadKind.VIDEO. Writes whatever tracks
                                // (manual or auto-generated) the source actually has - yt-dlp silently
                                // skips this rather than failing the download if none exist.
                                if (task.request.embedSubtitles) {
                                    addOption("--write-subs")
                                    addOption("--write-auto-subs")
                                    addOption("--embed-subs")
                                }
                            }
                            DownloadKind.AUDIO_ONLY -> {
                                addOption("-f", task.request.formatId ?: FormatSelector.audio())
                                addOption("-x")
                                addOption("--audio-format", task.request.audioContainer ?: "mp3")
                                val quality = PreferenceUtil.getString(PrefKeys.AUDIO_QUALITY, AudioQuality.BEST.name)
                                val qualityValue = AudioQuality.entries.firstOrNull { it.name == quality }?.ytDlpValue ?: "0"
                                addOption("--audio-quality", qualityValue)
                            }
                        }
                        // Appended last so a user-typed flag here can override any default above -
                        // yt-dlp/argparse takes the last occurrence for non-list options.
                        task.request.customArgs?.let { addCommands(tokenizeShellArgs(it)) }
                    }

                val response = PublicHttpsProxy().use { proxy ->
                    // Appended after custom arguments so an advanced option cannot replace the
                    // enforcing transport.
                    request.addOption("--proxy", proxy.url)
                    YoutubeDL.getInstance().execute(request, task.id) { progress, eta, _ ->
                        val current = tasks[task] as? DownloadStatus.Downloading ?: DownloadStatus.Downloading()
                        // yt-dlp reports progress far more often than the UI needs - writing every
                        // callback straight into the SnapshotStateMap floods Compose with dozens of
                        // recompositions a second. Rounding to the nearest whole percent bounds this
                        // to ~100 writes per download; animateFloatAsState in DownloadCard still
                        // tweens smoothly between the coarser steps.
                        val roundedProgress = (progress / 100f * 100).roundToInt() / 100f
                        val roundedEta = eta.toInt()
                        if (roundedProgress != current.progress || roundedEta != current.etaSeconds) {
                            tasks[task] = current.copy(progress = roundedProgress, etaSeconds = roundedEta)
                        }
                    }
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
                        // --write-thumbnail (added above when "Save thumbnails" is on) drops the
                        // thumbnail next to the media file in the same temp workspace, sharing its
                        // base name - grab it before the move below so it can be saved alongside
                        // the finished download using the same save-location logic.
                        val thumbnailFile =
                            sourceFile.parentFile
                                ?.listFiles { f -> f != sourceFile && f.nameWithoutExtension == sourceFile.nameWithoutExtension && f.extension.lowercase() in THUMBNAIL_EXTENSIONS }
                                ?.firstOrNull()
                        val location =
                            DownloadPaths.saveToCustomFolder(appContext, sourceFile, sourceFile.name)
                                ?: DownloadPaths.publishToMediaStore(appContext, task.request.kind, sourceFile, sourceFile.name)
                                ?: DownloadPaths
                                    .moveWithDedup(
                                        source = sourceFile,
                                        targetDir = DownloadPaths.outputDir(appContext, task.request.kind),
                                        desiredName = sourceFile.name,
                                    )
                                    .absolutePath
                        thumbnailFile?.let { thumb ->
                            DownloadPaths.saveToCustomFolder(appContext, thumb, thumb.name)
                                ?: DownloadPaths.publishToMediaStore(appContext, task.request.kind, thumb, thumb.name)
                                ?: DownloadPaths.moveWithDedup(thumb, DownloadPaths.outputDir(appContext, task.request.kind), thumb.name).absolutePath
                        }
                        location
                    }
                savedLocation
                }
            } finally {
                DownloadPaths.clearTempWorkspace(appContext, task.id)
            }
        }

    /** One-shot "Download complete" notification per finished task, separate from the ongoing
     * foreground-service notification - tapping it opens the downloaded file. */
    private fun notifyDownloadComplete(task: DownloadTask, filePath: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val openIntent = FileShareUtils.openFileIntent(appContext, filePath) ?: return
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
        // Permission may be revoked after the check; a notification failure must not fail the download.
        runCatching { NotificationManagerCompat.from(appContext).notify(task.id.hashCode(), notification) }
    }
}

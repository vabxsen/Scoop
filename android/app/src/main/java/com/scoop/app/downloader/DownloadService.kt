package com.scoop.app.downloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.scoop.app.R
import com.scoop.app.core.model.DownloadStatus
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Minimal foreground service kept alive only while [DownloadManager] has active tasks. It shows a
 * single static "downloading" notification; per-task progress notifications are follow-up work.
 */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val downloads: DownloadManager by inject()
    private var monitor: Job? = null
    private var latestStartId: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        val notification =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.notification_channel_downloads))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
        startForeground(NOTIFICATION_ID, notification)
        if (monitor == null) {
            monitor = scope.launch {
                snapshotFlow {
                    !downloads.isInitialized || downloads.tasks.values.any {
                        it is DownloadStatus.Queued || it is DownloadStatus.Analyzing ||
                            it is DownloadStatus.Downloading || it is DownloadStatus.Processing
                    }
                }.collect { active ->
                    if (!active) {
                        val observedStartId = latestStartId
                        val stillInactive = downloads.isInitialized && downloads.tasks.values.none {
                            it is DownloadStatus.Queued || it is DownloadStatus.Analyzing ||
                                it is DownloadStatus.Downloading || it is DownloadStatus.Processing
                        }
                        if (stillInactive) stopSelfResult(observedStartId)
                    }
                }
            }
        }
        downloads.resumePendingDownloads()
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "scoop_downloads"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, DownloadService::class.java))
        }
    }
}

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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
                    downloads.tasks.values.any { it is DownloadStatus.Analyzing || it is DownloadStatus.Downloading }
                }.collect { active ->
                    if (!active) stopSelf()
                }
            }
        }
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

        fun stop(context: Context) {
            context.stopService(Intent(context, DownloadService::class.java))
        }
    }
}

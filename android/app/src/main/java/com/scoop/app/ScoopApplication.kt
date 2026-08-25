package com.scoop.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.scoop.app.core.media.MediaEngineReadiness
import com.scoop.app.di.appModule
import com.scoop.app.downloader.DownloadService
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

class ScoopApplication : Application(), KoinComponent {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mediaEngineReadiness: MediaEngineReadiness by inject()

    override fun onCreate() {
        super.onCreate()

        MMKV.initialize(this)

        startKoin {
            androidLogger()
            androidContext(this@ScoopApplication)
            modules(appModule)
        }

        createNotificationChannel()

        mediaEngineReadiness.startInitializing(applicationScope)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                DownloadService.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_downloads),
                NotificationManager.IMPORTANCE_LOW,
            )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

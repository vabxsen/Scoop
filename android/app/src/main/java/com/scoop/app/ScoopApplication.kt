package com.scoop.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.scoop.app.di.appModule
import com.scoop.app.downloader.DownloadService
import com.tencent.mmkv.MMKV
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

private const val TAG = "ScoopApplication"

class ScoopApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        MMKV.initialize(this)

        startKoin {
            androidLogger()
            androidContext(this@ScoopApplication)
            modules(appModule)
        }

        createNotificationChannel()

        applicationScope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.init(this@ScoopApplication)
                FFmpeg.init(this@ScoopApplication)
                Aria2c.init(this@ScoopApplication)
            } catch (t: Throwable) {
                // The media engine failing to initialize shouldn't crash app startup; extraction
                // and download calls will surface a clear error once attempted.
                Log.e(TAG, "Failed to initialize the media engine", t)
            }
        }
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

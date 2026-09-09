package com.scoop.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scoop.app.core.model.*
import com.scoop.app.downloader.DownloadManager
import com.scoop.app.util.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class DownloadRecoveryDeviceTest {
    @Test fun transientFailureRetriesAndDeleteCanBeUndone() = runBlocking {
        val manager = GlobalContext.get().get<DownloadManager>()
        val oldRetry = PreferenceUtil.getString(PrefKeys.AUTO_RETRY_POLICY, "OFF")
        val oldWifi = PreferenceUtil.getBoolean(PrefKeys.WIFI_ONLY_DOWNLOADS, false)
        val oldBattery = PreferenceUtil.getString(PrefKeys.BATTERY_PAUSE_THRESHOLD, "OFF")
        var task: DownloadTask? = null
        try {
            PreferenceUtil.putString(PrefKeys.AUTO_RETRY_POLICY, "ONCE")
            PreferenceUtil.putBoolean(PrefKeys.WIFI_ONLY_DOWNLOADS, false)
            PreferenceUtil.putString(PrefKeys.BATTERY_PAUSE_THRESHOLD, "OFF")
            ActivityScenario.launch(MainActivity::class.java).use {
                ImageDownloadDeviceTest.ImageServer().use { server ->
                    task = manager.enqueue(DownloadRequest(server.url("/transient"), DownloadKind.IMAGE,
                        image = ImageCandidate(server.url("/transient"))), "Recovery audit", null)
                    withTimeout(20_000) { while (manager.tasks[task] !is DownloadStatus.Completed) delay(100) }
                    assertEquals(2, server.transientRequests.get())
                    manager.requestDelete(task!!.id)
                    assertTrue(task!!.id in manager.pendingDeleteIds)
                    manager.undoDelete(task!!.id)
                    delay(2500)
                    assertTrue(manager.tasks[task] is DownloadStatus.Completed)
                    manager.requestDelete(task!!.id)
                    withTimeout(5000) { while (manager.tasks.containsKey(task)) delay(100) }
                }
            }
        } finally {
            task?.let { manager.undoDelete(it.id); manager.deleteTaskAndFile(it.id) }
            PreferenceUtil.putString(PrefKeys.AUTO_RETRY_POLICY, oldRetry)
            PreferenceUtil.putBoolean(PrefKeys.WIFI_ONLY_DOWNLOADS, oldWifi)
            PreferenceUtil.putString(PrefKeys.BATTERY_PAUSE_THRESHOLD, oldBattery)
            manager.refreshQueue()
        }
    }
}

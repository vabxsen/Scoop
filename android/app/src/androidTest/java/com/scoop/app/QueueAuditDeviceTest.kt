package com.scoop.app

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.scoop.app.core.database.DownloadHistoryDao
import com.scoop.app.core.database.objects.DownloadedItem
import com.scoop.app.core.model.*
import com.scoop.app.downloader.DownloadManager
import com.scoop.app.ui.screen.settings.SettingsViewModel
import com.scoop.app.util.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class QueueAuditDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager get() = GlobalContext.get().get<DownloadManager>()
    private val history get() = GlobalContext.get().get<DownloadHistoryDao>()

    private suspend fun requireEmptyTestQueue() {
        delay(500) // Allow startup history hydration to finish.
        assumeTrue("Bulk cleanup checks require an empty test app", manager.tasks.isEmpty() && history.getAll().isEmpty())
    }

    private fun fixture(file: File, ageDays: Int = 0) = DownloadTask(
        UUID.randomUUID().toString(), DownloadRequest("https://example.com/audit.png", DownloadKind.IMAGE),
        "Scoop audit fixture", null, System.currentTimeMillis() - ageDays * 86_400_000L,
    ).also { manager.tasks[it] = DownloadStatus.Completed(file.absolutePath) }

    @Test fun retentionRemovesFileDatabaseAndVisibleTask() = runBlocking {
        requireEmptyTestQueue()
        val file = File.createTempFile("retention-audit", ".png", context.cacheDir)
        val task = fixture(file, 10)
        try {
            history.upsert(DownloadedItem(task.id, task.request.url, task.title, file.absolutePath, null, "IMAGE", task.createdAt))
            manager.clearHistoryOlderThan(7)
            assertFalse(file.exists())
            assertTrue(history.getAll().none { it.id == task.id })
            assertFalse("Expired task must disappear without restarting", manager.tasks.containsKey(task))
        } finally { manager.deleteTaskAndFile(task.id); file.delete() }
    }

    @Test fun clearAllDeletesIncognitoFileAndPendingUndo() = runBlocking {
        requireEmptyTestQueue()
        val file = File.createTempFile("incognito-audit", ".png", context.cacheDir)
        val task = fixture(file)
        try {
            manager.requestDelete(task.id)
            manager.clearAll()
            assertFalse("Clear all must delete session-only files", file.exists())
            assertTrue(manager.tasks.isEmpty())
            assertTrue("Pending delete state must be cleared", manager.pendingDeleteIds.isEmpty())
        } finally { manager.undoAllDeletes(); manager.deleteTaskAndFile(task.id); file.delete() }
    }

    @Test fun disablingWifiOnlyStartsWaitingDownload() = runBlocking {
        requireEmptyTestQueue()
        val oldBattery = PreferenceUtil.getString(PrefKeys.BATTERY_PAUSE_THRESHOLD, "OFF")
        val oldWifi = PreferenceUtil.getBoolean(PrefKeys.WIFI_ONLY_DOWNLOADS, false)
        val settings = GlobalContext.get().get<SettingsViewModel>()
        var task: DownloadTask? = null
        try {
            shell("svc wifi disable")
            shell("svc data disable")
            PreferenceUtil.putString(PrefKeys.BATTERY_PAUSE_THRESHOLD, "OFF")
            withContext(Dispatchers.Main) { settings.updateWifiOnlyDownloads(true) }
            withTimeout(10_000) {
                while (DownloadGate.blockedReason(context) != DownloadBlockReason.METERED_CONNECTION) delay(100)
            }
            ActivityScenario.launch(MainActivity::class.java).use {
                ImageDownloadDeviceTest.ImageServer().use { server ->
                    task = manager.enqueue(DownloadRequest(server.url("/original"), DownloadKind.IMAGE,
                        image = ImageCandidate(server.url("/original"))), "Wi-Fi gate audit", null)
                    delay(500)
                    assertEquals(DownloadStatus.Queued, manager.tasks[task])
                    withContext(Dispatchers.Main) { settings.updateWifiOnlyDownloads(false) }
                    withTimeout(10_000) {
                        while (manager.tasks[task] !is DownloadStatus.Completed) delay(100)
                    }
                }
            }
        } finally {
            task?.let { manager.deleteTaskAndFile(it.id) }
            PreferenceUtil.putString(PrefKeys.BATTERY_PAUSE_THRESHOLD, oldBattery)
            PreferenceUtil.putBoolean(PrefKeys.WIFI_ONLY_DOWNLOADS, oldWifi)
            shell("svc wifi enable")
            shell("svc data enable")
        }
    }

    private fun shell(command: String) {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).use { it.readBytes() }
    }
}

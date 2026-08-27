package com.scoop.app.ui.screen.settings

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scoop.app.core.database.DownloadHistoryDao
import com.scoop.app.core.model.AccentPalette
import com.scoop.app.core.model.AudioQuality
import com.scoop.app.core.model.DefaultAudioFormat
import com.scoop.app.core.model.DefaultVideoContainer
import com.scoop.app.core.model.DefaultVideoQuality
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.ThemeMode
import com.scoop.app.core.update.AppUpdateChecker
import com.scoop.app.core.update.UpdateAvailability
import com.scoop.app.core.update.UpdateCheckState
import com.scoop.app.util.FileShareUtils
import com.scoop.app.util.PrefKeys
import com.scoop.app.util.PreferenceUtil
import com.scoop.app.util.ThemePreferences
import com.scoop.app.util.toDecimalStorageSize
import com.scoop.app.util.toHumanReadableSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val appContext: Context,
    private val themePreferences: ThemePreferences,
    private val downloadHistoryDao: DownloadHistoryDao,
    private val updateChecker: AppUpdateChecker,
) : ViewModel() {

    val themeMode get() = themePreferences.themeMode
    val accentPalette get() = themePreferences.accentPalette
    val dynamicColorEnabled get() = themePreferences.dynamicColorEnabled

    var defaultVideoQuality by
        mutableStateOf(
            DefaultVideoQuality.entries.firstOrNull {
                it.name == PreferenceUtil.getString(PrefKeys.DEFAULT_VIDEO_QUALITY, DefaultVideoQuality.BEST.name)
            } ?: DefaultVideoQuality.BEST
        )
        private set

    var defaultAudioFormat by
        mutableStateOf(
            DefaultAudioFormat.entries.firstOrNull {
                it.name == PreferenceUtil.getString(PrefKeys.DEFAULT_AUDIO_FORMAT, DefaultAudioFormat.MP3.name)
            } ?: DefaultAudioFormat.MP3
        )
        private set

    var defaultVideoContainer by
        mutableStateOf(
            DefaultVideoContainer.entries.firstOrNull {
                it.name == PreferenceUtil.getString(PrefKeys.DEFAULT_VIDEO_CONTAINER, DefaultVideoContainer.MP4.name)
            } ?: DefaultVideoContainer.MP4
        )
        private set

    var audioQuality by
        mutableStateOf(
            AudioQuality.entries.firstOrNull { it.name == PreferenceUtil.getString(PrefKeys.AUDIO_QUALITY, AudioQuality.BEST.name) }
                ?: AudioQuality.BEST
        )
        private set

    var maxConcurrentDownloads by mutableIntStateOf(PreferenceUtil.getInt(PrefKeys.MAX_CONCURRENT_DOWNLOADS, 3))
        private set

    var wifiOnlyDownloads by mutableStateOf(PreferenceUtil.getBoolean(PrefKeys.WIFI_ONLY_DOWNLOADS, false))
        private set

    var deviceStorageLabel by mutableStateOf<String?>(null)
        private set

    var videoStorageLabel by mutableStateOf<String?>(null)
        private set

    var audioStorageLabel by mutableStateOf<String?>(null)
        private set

    var updateState by mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle)
        private set

    private data class StorageSnapshot(val deviceLabel: String, val videoBytes: Long, val videoCount: Int, val audioBytes: Long, val audioCount: Int)

    init {
        downloadHistoryDao
            .observeAll()
            .map { items ->
                withContext(Dispatchers.IO) {
                    var videoBytes = 0L
                    var videoCount = 0
                    var audioBytes = 0L
                    var audioCount = 0
                    items.forEach { item ->
                        val path = item.filePath ?: return@forEach
                        val size = FileShareUtils.sizeBytes(appContext, path) ?: return@forEach
                        if (item.kind == DownloadKind.AUDIO_ONLY.name) {
                            audioBytes += size
                            audioCount++
                        } else {
                            videoBytes += size
                            videoCount++
                        }
                    }
                    StorageSnapshot(deviceStorageLabel(), videoBytes, videoCount, audioBytes, audioCount)
                }
            }
            .onEach { snapshot ->
                deviceStorageLabel = snapshot.deviceLabel
                videoStorageLabel = formatStorageBreakdown(snapshot.videoBytes, snapshot.videoCount)
                audioStorageLabel = formatStorageBreakdown(snapshot.audioBytes, snapshot.audioCount)
            }
            .launchIn(viewModelScope)
    }

    private fun formatStorageBreakdown(bytes: Long, count: Int): String? =
        if (count == 0) null else "${bytes.toHumanReadableSize()} across $count ${if (count == 1) "download" else "downloads"}"

    /**
     * Real used/total space on the device's primary storage volume, via the OS - not just what
     * Scoop itself has downloaded. Uses StorageStatsManager (the same API the system Settings
     * app's own storage summary is built on) rather than raw StatFs: under scoped storage's FUSE
     * layer, StatFs on getExternalStorageDirectory() can report a noticeably smaller total/used
     * than the device's real capacity for a sandboxed app. Falls back to StatFs pre-Android 8.
     */
    private fun deviceStorageLabel(): String {
        val (totalBytes, usedBytes) =
            runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val statsManager = appContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                        val total = statsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
                        val free = statsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
                        total to (total - free)
                    } else {
                        legacyDeviceStorageBytes()
                    }
                }
                .getOrElse { legacyDeviceStorageBytes() }
        return "${usedBytes.toDecimalStorageSize()} used of ${totalBytes.toDecimalStorageSize()}"
    }

    private fun legacyDeviceStorageBytes(): Pair<Long, Long> {
        val statFs = StatFs(Environment.getExternalStorageDirectory().path)
        val total = statFs.blockCountLong * statFs.blockSizeLong
        val available = statFs.availableBlocksLong * statFs.blockSizeLong
        return total to (total - available)
    }

    fun setThemeMode(mode: ThemeMode) = themePreferences.setThemeMode(mode)

    fun setAccentPalette(palette: AccentPalette) = themePreferences.setAccentPalette(palette)

    fun setDynamicColorEnabled(enabled: Boolean) = themePreferences.setDynamicColorEnabled(enabled)

    fun updateDefaultVideoQuality(quality: DefaultVideoQuality) {
        defaultVideoQuality = quality
        PreferenceUtil.putString(PrefKeys.DEFAULT_VIDEO_QUALITY, quality.name)
    }

    fun updateDefaultAudioFormat(format: DefaultAudioFormat) {
        defaultAudioFormat = format
        PreferenceUtil.putString(PrefKeys.DEFAULT_AUDIO_FORMAT, format.name)
    }

    fun updateDefaultVideoContainer(container: DefaultVideoContainer) {
        defaultVideoContainer = container
        PreferenceUtil.putString(PrefKeys.DEFAULT_VIDEO_CONTAINER, container.name)
    }

    fun updateAudioQuality(quality: AudioQuality) {
        audioQuality = quality
        PreferenceUtil.putString(PrefKeys.AUDIO_QUALITY, quality.name)
    }

    fun updateMaxConcurrentDownloads(count: Int) {
        maxConcurrentDownloads = count
        PreferenceUtil.putInt(PrefKeys.MAX_CONCURRENT_DOWNLOADS, count)
    }

    fun updateWifiOnlyDownloads(enabled: Boolean) {
        wifiOnlyDownloads = enabled
        PreferenceUtil.putBoolean(PrefKeys.WIFI_ONLY_DOWNLOADS, enabled)
    }

    fun checkForUpdate() {
        if (updateState is UpdateCheckState.Checking || updateState is UpdateCheckState.Downloading) return
        updateState = UpdateCheckState.Checking
        viewModelScope.launch {
            when (val availability = updateChecker.findUpdate()) {
                is UpdateAvailability.UpToDate -> updateState = UpdateCheckState.UpToDate
                is UpdateAvailability.Error -> updateState = UpdateCheckState.Error(availability.message)
                is UpdateAvailability.Available -> {
                    updateState = UpdateCheckState.Downloading(0f)
                    try {
                        val file = updateChecker.downloadApk(availability.downloadUrl) { progress -> updateState = UpdateCheckState.Downloading(progress) }
                        updateState = UpdateCheckState.ReadyToInstall(file.path)
                    } catch (e: java.io.IOException) {
                        updateState = UpdateCheckState.Error(updateChecker.genericErrorMessage())
                    }
                }
            }
        }
    }

    fun consumeUpdateState() {
        updateState = UpdateCheckState.Idle
    }
}

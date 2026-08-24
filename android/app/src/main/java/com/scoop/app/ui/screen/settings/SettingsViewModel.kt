package com.scoop.app.ui.screen.settings

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
import com.scoop.app.core.model.CookieSite
import com.scoop.app.core.model.ThemeMode
import com.scoop.app.util.CookieRepository
import com.scoop.app.util.PrefKeys
import com.scoop.app.util.PreferenceUtil
import com.scoop.app.util.ThemePreferences
import com.scoop.app.util.toHumanReadableSize
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val themePreferences: ThemePreferences,
    private val downloadHistoryDao: DownloadHistoryDao,
    private val cookieRepository: CookieRepository,
) : ViewModel() {

    val themeMode get() = themePreferences.themeMode
    val accentPalette get() = themePreferences.accentPalette
    val dynamicColorEnabled get() = themePreferences.dynamicColorEnabled
    val signedInSites get() = cookieRepository.signedInSites

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

    var storageUsedLabel by mutableStateOf<String?>(null)
        private set

    init {
        downloadHistoryDao
            .observeAll()
            .map { items ->
                withContext(Dispatchers.IO) {
                    var totalBytes = 0L
                    var fileCount = 0
                    items.forEach { item ->
                        val path = item.filePath ?: return@forEach
                        val file = File(path)
                        if (file.exists()) {
                            totalBytes += file.length()
                            fileCount++
                        }
                    }
                    totalBytes to fileCount
                }
            }
            .onEach { (totalBytes, fileCount) ->
                storageUsedLabel = if (fileCount == 0) null else "${totalBytes.toHumanReadableSize()} across $fileCount ${if (fileCount == 1) "download" else "downloads"}"
            }
            .launchIn(viewModelScope)
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

    fun signOut(site: CookieSite) = cookieRepository.signOut(site)
}

package com.scoop.app.ui.screen.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.scoop.app.core.model.AccentPalette
import com.scoop.app.core.model.DefaultAudioFormat
import com.scoop.app.core.model.DefaultVideoQuality
import com.scoop.app.core.model.ThemeMode
import com.scoop.app.util.PrefKeys
import com.scoop.app.util.PreferenceUtil
import com.scoop.app.util.ThemePreferences

class SettingsViewModel(private val themePreferences: ThemePreferences) : ViewModel() {

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

    var maxConcurrentDownloads by mutableIntStateOf(PreferenceUtil.getInt(PrefKeys.MAX_CONCURRENT_DOWNLOADS, 3))
        private set

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

    fun updateMaxConcurrentDownloads(count: Int) {
        maxConcurrentDownloads = count
        PreferenceUtil.putInt(PrefKeys.MAX_CONCURRENT_DOWNLOADS, count)
    }
}

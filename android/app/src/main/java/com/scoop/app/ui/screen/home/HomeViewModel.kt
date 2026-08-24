package com.scoop.app.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scoop.app.core.model.DefaultAudioFormat
import com.scoop.app.core.model.DefaultVideoQuality
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.MediaFormat
import com.scoop.app.core.model.MediaInfo
import com.scoop.app.downloader.DownloadManager
import com.scoop.app.extractor.MediaExtractor
import com.scoop.app.util.PrefKeys
import com.scoop.app.util.PreferenceUtil
import kotlinx.coroutines.launch

sealed interface ConfigureUiState {
    data object Hidden : ConfigureUiState

    data object Loading : ConfigureUiState

    data class Error(val message: String) : ConfigureUiState

    data class Loaded(val info: MediaInfo) : ConfigureUiState
}

enum class FormatMode {
    AUTO,
    CUSTOM,
}

class HomeViewModel(private val extractor: MediaExtractor, private val downloadManager: DownloadManager) : ViewModel() {

    var url by mutableStateOf("")
        private set

    var configureState by mutableStateOf<ConfigureUiState>(ConfigureUiState.Hidden)
        private set

    var selectedKind by mutableStateOf(DownloadKind.VIDEO)
        private set

    var formatMode by mutableStateOf(FormatMode.AUTO)
        private set

    var selectedFormat by mutableStateOf<MediaFormat?>(null)
        private set

    var showFormatPickerSheet by mutableStateOf(false)
        private set

    fun onUrlChange(value: String) {
        url = value
    }

    /** Entry point for the download FAB: analyzes the current URL and opens the configure sheet. */
    fun startDownloadFlow() {
        val target = url.trim()
        if (target.isEmpty()) return
        configureState = ConfigureUiState.Loading
        viewModelScope.launch {
            extractor
                .analyze(target)
                .onSuccess { info ->
                    selectedKind = DownloadKind.VIDEO
                    formatMode = FormatMode.AUTO
                    selectedFormat = null
                    configureState = ConfigureUiState.Loaded(info)
                }
                .onFailure { configureState = ConfigureUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun retryAnalyze() = startDownloadFlow()

    fun dismissConfigureSheet() {
        configureState = ConfigureUiState.Hidden
        showFormatPickerSheet = false
    }

    fun selectKind(kind: DownloadKind) {
        selectedKind = kind
        formatMode = FormatMode.AUTO
        selectedFormat = null
    }

    fun selectFormatMode(mode: FormatMode) {
        formatMode = mode
        if (mode == FormatMode.AUTO) {
            selectedFormat = null
        } else {
            showFormatPickerSheet = true
        }
    }

    fun selectFormat(format: MediaFormat?) {
        selectedFormat = format
    }

    fun openFormatPicker() {
        showFormatPickerSheet = true
    }

    fun dismissFormatPicker() {
        showFormatPickerSheet = false
    }

    /** Enqueues the current selection. Returns false if there's nothing loaded yet to download. */
    fun confirmDownload(): Boolean {
        val info = (configureState as? ConfigureUiState.Loaded)?.info ?: return false
        val audioContainer =
            if (selectedKind == DownloadKind.AUDIO_ONLY) {
                DefaultAudioFormat.entries
                    .firstOrNull { it.name == PreferenceUtil.getString(PrefKeys.DEFAULT_AUDIO_FORMAT, DefaultAudioFormat.MP3.name) }
                    ?.container ?: "mp3"
            } else {
                null
            }
        // In Auto mode we still honor the user's configured default quality (Settings > Downloads)
        // rather than always falling back to yt-dlp's own "best" heuristic - Custom mode is the only
        // place the picked-format UI is shown.
        val formatId =
            when (formatMode) {
                FormatMode.AUTO -> defaultFormatFor(info, selectedKind)?.formatId
                FormatMode.CUSTOM -> selectedFormat?.formatId
            }
        downloadManager.enqueue(
            request = DownloadRequest(url = info.sourceUrl, kind = selectedKind, formatId = formatId, audioContainer = audioContainer),
            title = info.title,
            thumbnailUrl = info.thumbnailUrl,
        )
        dismissConfigureSheet()
        url = ""
        return true
    }

    private fun defaultFormatFor(info: MediaInfo, kind: DownloadKind): MediaFormat? =
        if (kind == DownloadKind.VIDEO) {
            val preference =
                DefaultVideoQuality.entries.firstOrNull {
                    it.name == PreferenceUtil.getString(PrefKeys.DEFAULT_VIDEO_QUALITY, DefaultVideoQuality.BEST.name)
                } ?: DefaultVideoQuality.BEST
            val targetHeight = preference.heightPx ?: return null
            val candidates = info.formats.filter { it.hasVideo && it.hasAudio }.ifEmpty { info.videoFormats }
            candidates.minByOrNull { format -> format.height?.let { kotlin.math.abs(it - targetHeight) } ?: Int.MAX_VALUE }
        } else {
            null
        }
}

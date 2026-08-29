package com.scoop.app.ui.screen.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scoop.app.core.model.DefaultAudioFormat
import com.scoop.app.core.model.DefaultVideoQuality
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.core.model.MediaFormat
import com.scoop.app.core.model.MediaInfo
import com.scoop.app.core.model.PlaylistInfo
import com.scoop.app.downloader.DownloadManager
import com.scoop.app.extractor.MediaExtractor
import com.scoop.app.util.PrefKeys
import com.scoop.app.util.PreferenceUtil
import com.scoop.app.util.isPlaylistUrl
import kotlinx.coroutines.launch

sealed interface ConfigureUiState {
    data object Hidden : ConfigureUiState

    data object Loading : ConfigureUiState

    data class Error(val message: String) : ConfigureUiState

    data class Loaded(val info: MediaInfo) : ConfigureUiState

    data class PlaylistLoaded(val info: PlaylistInfo) : ConfigureUiState
}

private const val TAG = "HomeViewModel"

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

    var embedSubtitles by mutableStateOf(false)
        private set

    var embedThumbnail by mutableStateOf(false)
        private set

    var customCommandEnabled by mutableStateOf(false)
        private set

    var customArgs by mutableStateOf("")
        private set

    /** Entry URLs currently checked in the playlist selection list. Modeled by URL (not id) since
     * that's what actually becomes each expanded [DownloadRequest.url] at confirm time. */
    var selectedPlaylistEntryUrls by mutableStateOf<Set<String>>(emptySet())
        private set

    /** The just-enqueued task the configure sheet switches to showing live progress for, if any. */
    var activeDownloadTaskId by mutableStateOf<String?>(null)
        private set

    val activeDownloadStatus: DownloadStatus?
        get() = activeDownloadTaskId?.let { id -> downloadManager.tasks.entries.firstOrNull { it.key.id == id }?.value }

    fun onUrlChange(value: String) {
        url = value
    }

    /** Entry point for the download FAB: analyzes the current URL and opens the configure sheet.
     * A "pure" playlist link (list= with no v=) routes through [MediaExtractor.getPlaylist]
     * instead - a video link that merely carries a list= param keeps today's single-video path. */
    fun startDownloadFlow() {
        val target = url.trim()
        if (target.isEmpty()) return
        configureState = ConfigureUiState.Loading
        viewModelScope.launch {
            if (isPlaylistUrl(target)) {
                extractor
                    .getPlaylist(target)
                    .onSuccess { info ->
                        if (info.entries.isEmpty()) {
                            configureState = ConfigureUiState.Error("Playlist has no videos")
                            return@onSuccess
                        }
                        selectedKind = DownloadKind.VIDEO
                        embedSubtitles = false
                        embedThumbnail = false
                        selectedPlaylistEntryUrls = info.entries.mapNotNull { it.url }.toSet()
                        configureState = ConfigureUiState.PlaylistLoaded(info)
                    }
                    .onFailure {
                        Log.e(TAG, "getPlaylist failed for $target", it)
                        configureState = ConfigureUiState.Error(it.message ?: "Unknown error")
                    }
            } else {
                extractor
                    .analyze(target)
                    .onSuccess { info ->
                        selectedKind = DownloadKind.VIDEO
                        formatMode = FormatMode.AUTO
                        selectedFormat = null
                        embedSubtitles = false
                        embedThumbnail = false
                        customCommandEnabled = false
                        customArgs = ""
                        configureState = ConfigureUiState.Loaded(info)
                    }
                    .onFailure {
                        Log.e(TAG, "analyze failed for $target", it)
                        configureState = ConfigureUiState.Error(it.message ?: "Unknown error")
                    }
            }
        }
    }

    fun retryAnalyze() = startDownloadFlow()

    fun dismissConfigureSheet() {
        configureState = ConfigureUiState.Hidden
        activeDownloadTaskId = null
        selectedPlaylistEntryUrls = emptySet()
        customCommandEnabled = false
        customArgs = ""
    }

    fun togglePlaylistEntry(entryUrl: String) {
        selectedPlaylistEntryUrls =
            if (entryUrl in selectedPlaylistEntryUrls) selectedPlaylistEntryUrls - entryUrl else selectedPlaylistEntryUrls + entryUrl
    }

    fun selectAllPlaylistEntries(info: PlaylistInfo) {
        selectedPlaylistEntryUrls = info.entries.mapNotNull { it.url }.toSet()
    }

    fun deselectAllPlaylistEntries() {
        selectedPlaylistEntryUrls = emptySet()
    }

    fun selectKind(kind: DownloadKind) {
        selectedKind = kind
        formatMode = FormatMode.AUTO
        selectedFormat = null
        // Subtitle embedding only applies to video; switching to audio would silently carry a
        // toggle over that no longer means anything.
        if (kind == DownloadKind.AUDIO_ONLY) embedSubtitles = false
    }

    fun selectFormatMode(mode: FormatMode) {
        formatMode = mode
        if (mode == FormatMode.AUTO) selectedFormat = null
    }

    fun selectFormat(format: MediaFormat?) {
        selectedFormat = format
    }

    fun toggleEmbedSubtitles() {
        embedSubtitles = !embedSubtitles
    }

    fun toggleEmbedThumbnail() {
        embedThumbnail = !embedThumbnail
    }

    fun toggleCustomCommand() {
        customCommandEnabled = !customCommandEnabled
    }

    fun onCustomArgsChange(value: String) {
        customArgs = value
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
        val task =
            downloadManager.enqueue(
                request =
                    DownloadRequest(
                        url = info.sourceUrl,
                        kind = selectedKind,
                        formatId = formatId,
                        audioContainer = audioContainer,
                        embedSubtitles = embedSubtitles,
                        embedThumbnail = embedThumbnail,
                        customArgs = customArgs.trim().takeIf { customCommandEnabled && it.isNotBlank() },
                    ),
                title = info.title,
                thumbnailUrl = info.thumbnailUrl,
            )
        activeDownloadTaskId = task.id
        url = ""
        return true
    }

    /** Enqueues the currently checked playlist entries, one [DownloadRequest] each - the existing
     * queue already handles any number of independent single-video tasks, so this just expands
     * the playlist into N ordinary enqueue() calls rather than reusing the single-task progress
     * view. Always Auto quality: playlist entries carry no per-video format list. */
    fun confirmPlaylistDownload(): Boolean {
        val info = (configureState as? ConfigureUiState.PlaylistLoaded)?.info ?: return false
        val audioContainer =
            if (selectedKind == DownloadKind.AUDIO_ONLY) {
                DefaultAudioFormat.entries
                    .firstOrNull { it.name == PreferenceUtil.getString(PrefKeys.DEFAULT_AUDIO_FORMAT, DefaultAudioFormat.MP3.name) }
                    ?.container ?: "mp3"
            } else {
                null
            }
        val selectedEntries = info.entries.filter { it.url != null && it.url in selectedPlaylistEntryUrls }
        if (selectedEntries.isEmpty()) return false
        selectedEntries.forEach { entry ->
            downloadManager.enqueue(
                request =
                    DownloadRequest(
                        url = entry.url!!,
                        kind = selectedKind,
                        formatId = null,
                        audioContainer = audioContainer,
                        embedSubtitles = embedSubtitles,
                        embedThumbnail = embedThumbnail,
                        playlistTitle = info.title,
                    ),
                title = entry.title ?: entry.url,
                thumbnailUrl = entry.thumbnailUrl,
            )
        }
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

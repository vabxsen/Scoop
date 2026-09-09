package com.scoop.app.ui.screen.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scoop.app.core.model.DefaultAudioFormat
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import com.scoop.app.core.model.ImageCollection
import com.scoop.app.extractor.ImageDiscovery
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

sealed interface ConfigureUiState {
    data object Hidden : ConfigureUiState

    data object Loading : ConfigureUiState

    data class Error(val message: String) : ConfigureUiState

    data class Loaded(val info: MediaInfo) : ConfigureUiState

    data class PlaylistLoaded(val info: PlaylistInfo) : ConfigureUiState

    data class ImagesLoaded(val collection: ImageCollection) : ConfigureUiState
}

private const val TAG = "HomeViewModel"

enum class FormatMode {
    HIGHEST,
    LOW,
}

class HomeViewModel(
    private val extractor: MediaExtractor,
    private val downloadManager: DownloadManager,
    private val imageDiscovery: ImageDiscovery,
) : ViewModel() {
    private var analysisJob: Job? = null
    var imagesOnly by mutableStateOf(false)
        private set
    var selectedImageUrls by mutableStateOf<Set<String>>(emptySet())
        private set

    fun selectImagesOnly(value: Boolean) { imagesOnly = value }

    private fun showImages(collection: ImageCollection) {
        selectedImageUrls =
            if (collection.images.size == 1 || PreferenceUtil.getBoolean(PrefKeys.SELECT_ALL_GALLERY_IMAGES, true)) {
                collection.images.map { it.url }.toSet()
            } else {
                emptySet()
            }
        configureState = ConfigureUiState.ImagesLoaded(collection)
        if (collection.images.size == 1 && !PreferenceUtil.getBoolean(PrefKeys.CONFIGURE_BEFORE_DOWNLOAD, true)) confirmImagesDownload()
    }

    fun toggleImage(imageUrl: String) {
        selectedImageUrls = if (imageUrl in selectedImageUrls) selectedImageUrls - imageUrl else selectedImageUrls + imageUrl
    }

    fun selectAllImages(selected: Boolean) {
        val collection = (configureState as? ConfigureUiState.ImagesLoaded)?.collection ?: return
        selectedImageUrls = if (selected) collection.images.map { it.url }.toSet() else emptySet()
    }

    fun confirmImagesDownload(): Int {
        val collection = (configureState as? ConfigureUiState.ImagesLoaded)?.collection ?: return 0
        val selected = collection.images.filter { it.url in selectedImageUrls }
        if (selected.isEmpty()) return 0
        val tasks = selected.mapIndexed { index, image ->
            downloadManager.enqueue(
                DownloadRequest(url = collection.sourceUrl, kind = DownloadKind.IMAGE, image = image,
                    playlistTitle = collection.title.takeIf { selected.size > 1 }),
                title = image.title.ifBlank { "Image ${index + 1}" }, thumbnailUrl = image.url,
            )
        }
        if (tasks.size == 1) activeDownloadTaskId = tasks.single().id
        url = ""
        return tasks.size
    }


    var url by mutableStateOf("")
        private set

    var configureState by mutableStateOf<ConfigureUiState>(ConfigureUiState.Hidden)
        private set

    var selectedKind by mutableStateOf(DownloadKind.VIDEO)
        private set

    var formatMode by mutableStateOf(FormatMode.HIGHEST)
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
        val target = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE).find(url.trim())?.value
            ?.trimEnd('.', ',', ')', ']') ?: url.trim()
        if (target.isEmpty()) return
        if (target.toHttpUrlOrNull() == null) {
            configureState = ConfigureUiState.Error("Paste a valid http or https link.")
            return
        }
        analysisJob?.cancel()
        activeDownloadTaskId = null
        configureState = ConfigureUiState.Loading
        val imageMode = imagesOnly
        analysisJob = viewModelScope.launch {
            if (imageMode) {
                try { showImages(imageDiscovery.discover(target)) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { configureState = ConfigureUiState.Error(e.message ?: "Could not find images") }
                return@launch
            }
            // A direct image needs no Python analysis. Failed probes leave media extraction alone.
            try {
                imageDiscovery.directImage(target)?.let { showImages(it); return@launch }
            } catch (e: CancellationException) { throw e } catch (_: Exception) { }
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
                        formatMode = FormatMode.HIGHEST
                        embedSubtitles = false
                        embedThumbnail = false
                        customCommandEnabled = false
                        customArgs = ""
                        configureState = ConfigureUiState.Loaded(info)
                        // Settings > General > "Configure before download" off means the user wants
                        // a one-tap download with sensible defaults, skipping the review form -
                        // the sheet still opens to show live progress via activeDownloadTaskId.
                        if (!PreferenceUtil.getBoolean(PrefKeys.CONFIGURE_BEFORE_DOWNLOAD, true)) confirmDownload()
                    }
                    .onFailure {
                        if (it is CancellationException) throw it
                        try { showImages(imageDiscovery.discover(target)) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { configureState = ConfigureUiState.Error(it.message ?: "No downloadable media or images found") }
                    }
            }
        }
    }

    fun retryAnalyze() = startDownloadFlow()

    fun dismissConfigureSheet() {
        analysisJob?.cancel()
        selectedImageUrls = emptySet()
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
        formatMode = FormatMode.HIGHEST
        // Subtitle embedding only applies to video; switching to audio would silently carry a
        // toggle over that no longer means anything.
        if (kind == DownloadKind.AUDIO_ONLY) embedSubtitles = false
    }

    fun selectFormatMode(mode: FormatMode) {
        formatMode = mode
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
        // Highest quality deliberately ignores the Settings > Downloads default cap and always
        // leaves formatId null, which yt-dlp itself resolves to the true best available
        // (bestvideo*+bestaudio/best / bestaudio/best) - the whole point of the button.
        val formatId =
            when (formatMode) {
                FormatMode.HIGHEST -> null
                FormatMode.LOW -> lowestFormatFor(info, selectedKind)?.formatId
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

    /** The lowest-resolution/bitrate format available, for the "Low quality" button - mirrors the
     * candidate selection a progressive (video+audio) format list uses, just inverted to minByOrNull. */
    private fun lowestFormatFor(info: MediaInfo, kind: DownloadKind): MediaFormat? =
        if (kind == DownloadKind.VIDEO) {
            val candidates = info.formats.filter { it.hasVideo && it.hasAudio }.ifEmpty { info.videoFormats }
            candidates.minByOrNull { it.height ?: Int.MAX_VALUE }
        } else {
            info.audioOnlyFormats.minByOrNull { it.audioBitrateKbps ?: Double.MAX_VALUE }
        }
}

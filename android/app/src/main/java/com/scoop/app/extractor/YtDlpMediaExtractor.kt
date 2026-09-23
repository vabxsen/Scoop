package com.scoop.app.extractor

import com.scoop.app.core.media.MediaEngineReadiness
import com.scoop.app.core.model.MediaInfo
import com.scoop.app.core.model.PlaylistEntryInfo
import com.scoop.app.core.model.PlaylistInfo
import com.scoop.app.core.network.SecureUrl
import com.scoop.app.core.network.PublicHttpsProxy
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlin.math.roundToInt
import java.util.UUID
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Extracts media metadata by shelling out to the bundled yt-dlp runtime and parsing its JSON. */
class YtDlpMediaExtractor(private val mediaEngineReadiness: MediaEngineReadiness) : MediaExtractor {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun analyze(url: String): Result<MediaInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireNotNull(SecureUrl.parse(url)) { "Only public HTTPS links are allowed" }
                mediaEngineReadiness.awaitReady()
                val request =
                    YoutubeDLRequest(url).apply {
                        addOption("--dump-single-json")
                        addOption("--no-playlist")
                        addOption("--no-warnings")
                        addOption("-R", "1")
                        addOption("--socket-timeout", "10")
                    }
                val output = executeBounded(request, ANALYZE_TIMEOUT_MS)
                require(output.length <= MAX_METADATA_CHARS) { "Metadata response was too large" }
                json.decodeFromString<YtDlpVideoJson>(output).toMediaInfo(url)
            }.onFailure { if (it is CancellationException) throw it }
        }

    override suspend fun getPlaylist(url: String): Result<PlaylistInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireNotNull(SecureUrl.parse(url)) { "Only public HTTPS links are allowed" }
                mediaEngineReadiness.awaitReady()
                val request =
                    YoutubeDLRequest(url).apply {
                        addOption("--dump-single-json")
                        addOption("--flat-playlist")
                        addOption("--yes-playlist")
                        addOption("--playlist-end", MAX_PLAYLIST_ENTRIES.toString())
                        addOption("--no-warnings")
                    }
                val output = executeBounded(request, PLAYLIST_TIMEOUT_MS)
                require(output.length <= MAX_METADATA_CHARS) { "Playlist response was too large" }
                json.decodeFromString<YtDlpPlaylistJson>(output).toPlaylistInfo()
            }.onFailure { if (it is CancellationException) throw it }
    }

    private suspend fun executeBounded(request: YoutubeDLRequest, timeoutMs: Long): String = coroutineScope {
        val processId = UUID.randomUUID().toString()
        PublicHttpsProxy().use { proxy ->
            request.addOption("--proxy", proxy.url)
            val execution = async(Dispatchers.IO) { YoutubeDL.getInstance().execute(request, processId, null).out }
            try {
                withTimeout(timeoutMs) { execution.await() }
            } catch (error: TimeoutCancellationException) {
                throw IOException("Video analysis timed out. Check your connection and try again.", error)
            } finally {
                YoutubeDL.destroyProcessById(processId)
                execution.cancel()
            }
        }
    }

}

private const val MAX_PLAYLIST_ENTRIES = 200
private const val MAX_METADATA_CHARS = 2 * 1024 * 1024
private const val ANALYZE_TIMEOUT_MS = 60_000L
private const val PLAYLIST_TIMEOUT_MS = 90_000L

private fun YtDlpVideoJson.toMediaInfo(sourceUrl: String): MediaInfo {
    return MediaInfo(
        sourceUrl = listOfNotNull(originalUrl, webpageUrl, sourceUrl).firstOrNull { SecureUrl.parse(it) != null } ?: sourceUrl,
        title = title,
        thumbnailUrl = thumbnail?.takeIf { SecureUrl.parse(it) != null },
    )
}

private fun YtDlpPlaylistJson.toPlaylistInfo(): PlaylistInfo =
    PlaylistInfo(
        title = title,
        entries =
            entries.orEmpty().asSequence().take(MAX_PLAYLIST_ENTRIES).mapNotNull {
                val safeUrl = it.url?.takeIf { candidate -> SecureUrl.parse(candidate) != null } ?: return@mapNotNull null
                PlaylistEntryInfo(
                    url = safeUrl,
                    title = it.title,
                    durationSeconds = it.duration?.roundToInt(),
                    thumbnailUrl = it.thumbnail?.takeIf { candidate -> SecureUrl.parse(candidate) != null },
                )
            }.toList(),
    )

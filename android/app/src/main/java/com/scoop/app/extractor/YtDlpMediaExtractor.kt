package com.scoop.app.extractor

import android.content.Context
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Extracts media metadata by shelling out to the bundled yt-dlp runtime and parsing its JSON. */
class YtDlpMediaExtractor(
    private val context: Context,
    private val mediaEngineReadiness: MediaEngineReadiness,
) : MediaExtractor {

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
                        // -R only limits download retries, not metadata/extractor retries.
                        addOption("--extractor-retries", "1")
                        addOption("--socket-timeout", "10")
                    }
                val output = InstagramCookieFile.use(context, url) { cookieFile ->
                    // Instagram increasingly withholds reels from signed-out requests; reuse the
                    // optional in-app Instagram sign-in for its own links only.
                    cookieFile?.let { request.addOption("--cookies", it.absolutePath) }
                    executeBounded(request, ANALYZE_TIMEOUT_MS)
                }
                require(output.length <= MAX_METADATA_CHARS) { "Metadata response was too large" }
                json.decodeFromString<YtDlpVideoJson>(output).toMediaInfo(url)
            }.onFailure { if (it is CancellationException) throw it }
                .recoverCatching { error ->
                    // Offer the sign-in (or a fresh one, if the saved session expired) instead of
                    // yt-dlp's advice to pass browser cookies, which Scoop users cannot act on.
                    throw if (isInstagramSignInFailure(url, error)) InstagramSignInRequiredException(error) else error
                }
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

    private suspend fun executeBounded(request: YoutubeDLRequest, timeoutMs: Long): String {
        val processId = UUID.randomUUID().toString()
        return PublicHttpsProxy().use { proxy ->
            request.addOption("--proxy", proxy.url)
            executeWithAnalysisTimeout(timeoutMs, destroyProcess = {
                YoutubeDL.destroyProcessById(processId)
            }) {
                YoutubeDL.getInstance().execute(request, processId, null).out
            }
        }
    }

}

private const val MAX_PLAYLIST_ENTRIES = 200
private const val MAX_METADATA_CHARS = 2 * 1024 * 1024
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

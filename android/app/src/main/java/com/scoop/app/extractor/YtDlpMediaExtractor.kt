package com.scoop.app.extractor

import com.scoop.app.core.media.MediaEngineReadiness
import com.scoop.app.core.model.MediaFormat
import com.scoop.app.core.model.MediaInfo
import com.scoop.app.core.model.PlaylistEntryInfo
import com.scoop.app.core.model.PlaylistInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Forces yt-dlp's "android" YouTube player client instead of its default "web" one. YouTube's web
 * client increasingly returns "Sign in to confirm you're not a bot" for unauthenticated requests,
 * which normally requires passing cookies - the android client isn't subject to that same
 * bot-check, so this keeps YouTube working without needing the cookie-import flow the app doesn't
 * have. Applied to every yt-dlp invocation (analyze, playlist, and the actual download) since any
 * one of them can hit the same wall independently.
 */
const val YOUTUBE_PLAYER_CLIENT_ARG = "youtube:player_client=android"

/** Extracts media metadata by shelling out to the bundled yt-dlp runtime and parsing its JSON. */
class YtDlpMediaExtractor(private val mediaEngineReadiness: MediaEngineReadiness) : MediaExtractor {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun analyze(url: String): Result<MediaInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                mediaEngineReadiness.awaitReady()
                val request =
                    YoutubeDLRequest(url).apply {
                        addOption("--dump-single-json")
                        addOption("--no-playlist")
                        addOption("--no-warnings")
                        addOption("-R", "1")
                        addOption("--socket-timeout", "10")
                        addOption("--extractor-args", YOUTUBE_PLAYER_CLIENT_ARG)
                    }
                val response = YoutubeDL.getInstance().execute(request, "analyze:$url", null)
                json.decodeFromString<YtDlpVideoJson>(response.out).toMediaInfo(url)
            }
        }

    override suspend fun getPlaylist(url: String): Result<PlaylistInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                mediaEngineReadiness.awaitReady()
                val request =
                    YoutubeDLRequest(url).apply {
                        addOption("--dump-single-json")
                        addOption("--flat-playlist")
                        addOption("--yes-playlist")
                        addOption("--no-warnings")
                        addOption("--extractor-args", YOUTUBE_PLAYER_CLIENT_ARG)
                    }
                val response = YoutubeDL.getInstance().execute(request, "playlist:$url", null)
                json.decodeFromString<YtDlpPlaylistJson>(response.out).toPlaylistInfo(url)
            }
        }

}

private fun YtDlpVideoJson.toMediaInfo(sourceUrl: String): MediaInfo {
    val rawFormats = requestedFormats ?: formats ?: emptyList()
    return MediaInfo(
        id = id,
        sourceUrl = originalUrl ?: webpageUrl ?: sourceUrl,
        title = title,
        uploader = uploader ?: channel,
        durationSeconds = duration?.roundToInt(),
        uploadDate = uploadDate,
        thumbnailUrl = thumbnail,
        description = description,
        formats = rawFormats.map { it.toMediaFormat() },
    )
}

private fun YtDlpFormatJson.toMediaFormat(): MediaFormat =
    MediaFormat(
        formatId = formatId ?: "",
        container = ext,
        formatNote = formatNote,
        videoCodec = vcodec,
        audioCodec = acodec,
        width = width?.roundToInt(),
        height = height?.roundToInt(),
        fps = fps?.roundToInt(),
        audioBitrateKbps = abr,
        totalBitrateKbps = tbr,
        fileSizeBytes = (filesize ?: fileSizeApprox)?.toLong(),
    )

private fun YtDlpPlaylistJson.toPlaylistInfo(sourceUrl: String): PlaylistInfo =
    PlaylistInfo(
        sourceUrl = sourceUrl,
        title = title,
        uploader = uploader,
        entries =
            entries.orEmpty().map {
                PlaylistEntryInfo(
                    id = it.id,
                    url = it.url,
                    title = it.title,
                    uploader = it.uploader,
                    durationSeconds = it.duration?.roundToInt(),
                    thumbnailUrl = it.thumbnail,
                )
            },
    )

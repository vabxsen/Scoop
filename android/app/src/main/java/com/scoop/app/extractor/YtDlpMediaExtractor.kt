package com.scoop.app.extractor

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

/** Extracts media metadata by shelling out to the bundled yt-dlp runtime and parsing its JSON. */
class YtDlpMediaExtractor : MediaExtractor {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun analyze(url: String): Result<MediaInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request =
                    YoutubeDLRequest(url).apply {
                        addOption("--dump-single-json")
                        addOption("--no-playlist")
                        addOption("--no-warnings")
                        addOption("-R", "1")
                        addOption("--socket-timeout", "10")
                    }
                val response = YoutubeDL.getInstance().execute(request, "analyze:$url", null)
                json.decodeFromString<YtDlpVideoJson>(response.out).toMediaInfo(url)
            }
        }

    override suspend fun getPlaylist(url: String): Result<PlaylistInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request =
                    YoutubeDLRequest(url).apply {
                        addOption("--dump-single-json")
                        addOption("--flat-playlist")
                        addOption("--yes-playlist")
                        addOption("--no-warnings")
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

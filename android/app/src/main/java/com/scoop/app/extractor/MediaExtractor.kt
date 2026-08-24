package com.scoop.app.extractor

import com.scoop.app.core.model.MediaInfo
import com.scoop.app.core.model.PlaylistInfo

/**
 * Abstraction over whatever extraction engine resolves a URL into downloadable media.
 * Nothing outside this package should depend on yt-dlp-specific types.
 */
interface MediaExtractor {
    suspend fun analyze(url: String): Result<MediaInfo>

    suspend fun getFormats(url: String): Result<MediaInfo> = analyze(url)

    suspend fun getPlaylist(url: String): Result<PlaylistInfo>
}

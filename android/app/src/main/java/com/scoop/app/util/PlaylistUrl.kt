package com.scoop.app.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val YOUTUBE_HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com")
private val PLAYLIST_PATHS = setOf("/playlist", "/watch", "/")

/** True only for a "pure" YouTube playlist link (has `list=`, no `v=`) - e.g. youtube.com/playlist?list=X.
 * A `watch?v=X&list=Y` URL is left alone so linking a single video from within a playlist keeps
 * today's single-video behavior instead of silently expanding into the whole playlist. The same
 * goes for youtu.be/X?list=Y and /shorts/ or /embed/ links, which carry the video in the path.
 * The rule is YouTube's URL scheme, so other sites' `list=` parameters are never playlists here. */
fun isPlaylistUrl(url: String): Boolean {
    val parsed = url.toHttpUrlOrNull() ?: return false
    if (parsed.host.lowercase() !in YOUTUBE_HOSTS || parsed.encodedPath !in PLAYLIST_PATHS) return false
    val hasList = !parsed.queryParameter("list").isNullOrBlank()
    val hasVideoId = !parsed.queryParameter("v").isNullOrBlank()
    return hasList && !hasVideoId
}

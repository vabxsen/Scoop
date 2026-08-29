package com.scoop.app.util

import android.net.Uri

/** True only for a "pure" playlist link (has `list=`, no `v=`) - e.g. youtube.com/playlist?list=X.
 * A `watch?v=X&list=Y` URL is left alone so linking a single video from within a playlist keeps
 * today's single-video behavior instead of silently expanding into the whole playlist. */
fun isPlaylistUrl(url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val hasList = !uri.getQueryParameter("list").isNullOrBlank()
    val hasVideoId = !uri.getQueryParameter("v").isNullOrBlank()
    return hasList && !hasVideoId
}

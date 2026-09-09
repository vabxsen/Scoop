package com.scoop.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ImageCandidate(
    val url: String,
    val title: String = "Image",
    val mimeType: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long? = null,
    // Kept only in memory; some public image hosts require a referer or session cookie.
    val headers: Map<String, String> = emptyMap(),
)

data class ImageCollection(
    val sourceUrl: String,
    val title: String,
    val images: List<ImageCandidate>,
    val notice: String? = null,
)

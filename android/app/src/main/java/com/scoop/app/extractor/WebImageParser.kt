package com.scoop.app.extractor

import com.scoop.app.core.model.ImageCandidate
import com.scoop.app.core.model.ImageCollection
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup

object WebImageParser {
    const val MAX_IMAGES = 200

    fun parse(html: String, pageUrl: String): ImageCollection {
        val document = Jsoup.parse(html, pageUrl)
        val images = linkedMapOf<String, ImageCandidate>()
        fun add(raw: String, title: String = "Image") {
            if (images.size >= MAX_IMAGES || raw.isBlank()) return
            val absolute = document.baseUri().toHttpUrlOrNull()?.resolve(raw.trim()) ?: return
            val url = absolute.toString()
            images.putIfAbsent(url, ImageCandidate(url, title.ifBlank { "Image" }.take(200), headers = mapOf("Referer" to pageUrl, "User-Agent" to IMAGE_USER_AGENT)))
        }
        document.select("meta[property=og:image], meta[property=og:image:secure_url], meta[name=twitter:image], meta[name=twitter:image:src]")
            .forEach { add(it.attr("content"), document.title()) }
        document.select("img").forEach { element ->
            // Pick the largest responsive candidate rather than offering every thumbnail size.
            val srcset = element.attr("srcset").ifBlank { element.attr("data-srcset") }
            val largest = srcset.split(',').mapNotNull { part ->
                val pieces = part.trim().split(Regex("\\s+"))
                val url = pieces.firstOrNull()?.takeIf { it.isNotBlank() && !it.startsWith("data:") } ?: return@mapNotNull null
                url to (pieces.getOrNull(1)?.dropLast(1)?.toDoubleOrNull() ?: 1.0)
            }.maxByOrNull { it.second }?.first
            val source = largest ?: element.attr("data-original").ifBlank { element.attr("data-src") }.ifBlank { element.attr("src") }
            add(source, element.attr("alt"))
        }
        document.select("picture source[srcset]").forEach { element ->
            element.attr("srcset").split(',').lastOrNull()?.trim()?.substringBefore(' ')?.let { add(it) }
        }
        document.select("a[href]").forEach { element ->
            if (Regex("(?i)\\.(jpe?g|png|webp|gif|avif|heic|heif|bmp|tiff?|svg|jxl)(?:[?#]|$)").containsMatchIn(element.attr("href"))) {
                add(element.attr("href"), element.text())
            }
        }
        return ImageCollection(
            sourceUrl = pageUrl,
            title = document.title().ifBlank { pageUrl.toHttpUrlOrNull()?.host ?: "Images" },
            images = images.values.toList(),
            notice = if (images.size >= MAX_IMAGES) "Showing the first $MAX_IMAGES images from this page." else "Images found on this webpage may include thumbnails and icons.",
        )
    }
}

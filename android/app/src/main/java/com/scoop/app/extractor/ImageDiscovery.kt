package com.scoop.app.extractor

import android.graphics.BitmapFactory
import com.scoop.app.core.model.ImageCandidate
import com.scoop.app.core.model.ImageCollection
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class ImageDiscovery(client: OkHttpClient, private val gallery: GalleryImageExtractor) {
    private val http = client.newBuilder().callTimeout(20, TimeUnit.SECONDS).build()

    /** Header/signature inspection only; closes the response before downloading the image. */
    suspend fun directImage(url: String): ImageCollection? = http.readImageResponse(request(url)) { response ->
        if (!response.isSuccessful) return@readImageResponse null
        val prefix = response.peekBody(64 * 1024).bytes()
        val type = ImageFormats.detect(prefix) ?: return@readImageResponse null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(prefix, 0, prefix.size, options)
        val resolved = response.request.url
        ImageCollection(url, resolved.pathSegments.lastOrNull().orEmpty().ifBlank { "Image" }, listOf(
            ImageCandidate(resolved.toString(), resolved.pathSegments.lastOrNull().orEmpty().ifBlank { "Image" }, type,
                options.outWidth.coerceAtLeast(0), options.outHeight.coerceAtLeast(0), response.body.contentLength().takeIf { it >= 0 },
                mapOf("User-Agent" to IMAGE_USER_AGENT)),
        ))
    }

    suspend fun discover(url: String): ImageCollection {
        // Login-page icons and avatars are not the photos from an Instagram post.
        // Preserve the extractor's authentication error instead of offering those as a gallery.
        if (InstagramSession.isPost(url)) {
            val collection = gallery.discover(url)
            if (collection.images.isEmpty()) throw IOException("This Instagram post has no accessible photos. It may contain only video or be unavailable to your account.")
            return collection
        }
        var failure: Exception? = null
        try { directImage(url)?.let { return it } } catch (e: CancellationException) { throw e } catch (e: Exception) { failure = e }
        try {
            gallery.discover(url).takeIf { it.images.isNotEmpty() }?.let { return it }
        } catch (e: CancellationException) { throw e } catch (e: Exception) { failure = e }
        val page = http.readImageResponse(request(url)) { response ->
            if (!response.isSuccessful) throw IOException("This link returned HTTP ${response.code}. The page may require a login or no longer be available.")
            val body = response.body
            val type = body.contentType()
            if (type != null && type.subtype != "html" && type.subtype != "xhtml+xml" && type.type != "text") {
                throw IOException("This link is not an image or an accessible webpage.")
            }
            val html = body.byteStream().readBytesBounded(2 * 1024 * 1024).toString(type?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)
            WebImageParser.parse(html, response.request.url.toString())
        }
        if (page.images.isEmpty()) throw IOException("No accessible images were found. This page may need a login, load images with JavaScript, or be unsupported.", failure)
        return page
    }

    private fun request(url: String): Request {
        val parsed = url.toHttpUrlOrNull() ?: throw IOException("Paste a valid http or https link.")
        return Request.Builder().url(parsed).header("User-Agent", IMAGE_USER_AGENT).build()
    }
}

internal fun InputStream.readBytesBounded(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count == -1) return output.toByteArray()
        if (output.size() + count > limit) throw IOException("This page is too large to inspect safely.")
        output.write(buffer, 0, count)
    }
}

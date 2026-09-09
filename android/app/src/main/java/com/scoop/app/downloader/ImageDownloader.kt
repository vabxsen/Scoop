package com.scoop.app.downloader

import android.content.Context
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadSpeedLimit
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.extractor.IMAGE_USER_AGENT
import com.scoop.app.extractor.ImageFormats
import com.scoop.app.extractor.readImageResponse
import com.scoop.app.extractor.forImages
import com.scoop.app.util.PrefKeys
import com.scoop.app.util.PreferenceUtil
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImageDownloader(private val context: Context, client: OkHttpClient) {
    private val http = client.forImages().newBuilder()
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun download(task: DownloadTask, onProgress: (Float) -> Unit): String {
        val image = task.request.image ?: throw IOException("Image details are missing. Analyze the original link again.")
        val jobContext = currentCoroutineContext()
        val request = Request.Builder().url(image.url).header("User-Agent", IMAGE_USER_AGENT).apply {
            image.headers.filterKeys { it.lowercase() in setOf("user-agent", "referer", "origin", "cookie", "accept", "authorization") }
                .forEach { (name, value) -> header(name, value) }
        }.build()
        val temporary = DownloadPaths.tempWorkspace(context, task.id)
        try {
            return http.readImageResponse(request) { response ->
                if (!response.isSuccessful) throw IOException("Image download failed (HTTP ${response.code}). Try analyzing the link again.")
                val body = response.body
                val length = body.contentLength()
                if (length > MAX_BYTES) throw IOException("This image exceeds the 256 MB download limit.")
                val type = ImageFormats.detect(response.peekBody(4096).bytes())
                    ?: throw IOException("The server returned a webpage or unsupported file instead of an image.")
                val extension = ImageFormats.extension(type) ?: throw IOException("Unsupported image format.")
                val base = image.title.substringBeforeLast('.', image.title).replace(Regex("[\\p{Cntrl}\\\\/:*?\"<>|]"), "_").trim().trim('.').take(120).ifBlank { "Image" }
                val file = File(temporary, "$base.$extension")
                val speedLimit = DownloadSpeedLimit.entries.firstOrNull { it.name == PreferenceUtil.getString(PrefKeys.DOWNLOAD_SPEED_LIMIT, DownloadSpeedLimit.UNLIMITED.name) }
                val bytesPerSecond = when (speedLimit) {
                    DownloadSpeedLimit.KBPS_500 -> 500L * 1024
                    DownloadSpeedLimit.MBPS_1 -> 1024L * 1024
                    DownloadSpeedLimit.MBPS_2 -> 2L * 1024 * 1024
                    DownloadSpeedLimit.MBPS_5 -> 5L * 1024 * 1024
                    else -> 0L
                }
                var downloaded = 0L
                var lastPercent = -2
                val start = System.nanoTime()
                body.byteStream().use { source -> file.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        jobContext.ensureActive()
                        val count = source.read(buffer)
                        if (count == -1) break
                        downloaded += count
                        if (downloaded > MAX_BYTES) throw IOException("This image exceeds the 256 MB download limit.")
                        output.write(buffer, 0, count)
                        if (bytesPerSecond > 0) {
                            val wait = downloaded * 1000 / bytesPerSecond - (System.nanoTime() - start) / 1_000_000
                            if (wait > 0) Thread.sleep(wait.coerceAtMost(100))
                        }
                        val percent = if (length > 0) ((downloaded * 100 / length).coerceAtMost(99)).toInt() else -1
                        if (percent != lastPercent) { lastPercent = percent; onProgress(if (percent < 0) -1f else percent / 100f) }
                    }
                } }
                if (downloaded == 0L || (length >= 0 && downloaded != length)) throw IOException("The image download was interrupted. Please retry.")
                jobContext.ensureActive()
                DownloadPaths.saveToCustomFolder(context, file, file.name, type)
                    ?: DownloadPaths.publishToMediaStore(context, DownloadKind.IMAGE, file, file.name, type)
                    ?: DownloadPaths.moveWithDedup(file, DownloadPaths.outputDir(context, DownloadKind.IMAGE), file.name).absolutePath
            }
        } finally {
            DownloadPaths.clearTempWorkspace(context, task.id)
        }
    }

    companion object { private const val MAX_BYTES = 256L * 1024 * 1024 }
}

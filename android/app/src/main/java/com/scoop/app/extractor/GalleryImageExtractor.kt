package com.scoop.app.extractor

import android.content.Context
import android.os.Build
import com.scoop.app.core.media.MediaEngineReadiness
import com.scoop.app.core.model.ImageCandidate
import com.scoop.app.core.model.ImageCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Runs a separate, bundled gallery-dl process without changing yt-dlp or its packages. */
class GalleryImageExtractor(private val context: Context, private val readiness: MediaEngineReadiness) {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class GalleryResult(
        val images: List<ImageCandidate> = emptyList(),
        val error: String? = null,
        val limited: Boolean = false,
    )

    suspend fun discover(url: String): ImageCollection {
        val cookies = InstagramSession.cookiesFor(url)
        val output = try { execute(url, cookies) } catch (e: TimeoutCancellationException) {
            throw IOException("The gallery took too long to respond.", e)
        }
        val result = json.decodeFromString<GalleryResult>(output)
        if (result.error == "authentication_required" && InstagramSession.isPost(url)) throw InstagramSignInRequiredException()
        if (result.error != null) throw IOException("This site could not provide images. It may require a login or restrict downloads.")
        val title = url.toHttpUrlOrNull()?.let { "${it.host} · ${it.pathSegments.lastOrNull { part -> part.isNotBlank() }.orEmpty()}" } ?: "Images"
        return ImageCollection(url, title, result.images,
            if (result.limited) "Showing the first ${WebImageParser.MAX_IMAGES} images." else null)
    }

    // Also used by the device smoke test to verify the actual packaged runtime.
    suspend fun version(): String = execute("--version")

    private suspend fun execute(argument: String, cookies: Map<String, String> = emptyMap()): String = withTimeout(60_000L) {
        readiness.awaitReady()
        withContext(Dispatchers.IO) {
            val runtime = prepareRuntime()
            // Paths are part of the pinned youtubedl-android 0.17.3 layout; no reflection.
            val pythonHome = File(context.noBackupFilesDir, "youtubedl-android/packages/python/usr")
            val executable = File(context.applicationInfo.nativeLibraryDir, "libpython.so")
            if (!executable.isFile || !pythonHome.isDirectory) throw IOException("The image gallery runtime is unavailable on this device.")
            val errorFile = File.createTempFile("gallery-", ".log", context.cacheDir)
            try {
                suspendCancellableCoroutine { continuation ->
                    val process = ProcessBuilder(executable.absolutePath, File(runtime, "extract.py").absolutePath, argument)
                        .directory(runtime)
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) redirectError(errorFile)
                        }
                        .apply {
                            environment().apply {
                                put("PYTHONHOME", pythonHome.absolutePath)
                                put("PYTHONPATH", runtime.absolutePath)
                                put("PYTHONDONTWRITEBYTECODE", "1")
                                put("LD_LIBRARY_PATH", File(pythonHome, "lib").absolutePath)
                                put("SSL_CERT_FILE", File(pythonHome, "etc/tls/cert.pem").absolutePath)
                                put("HOME", runtime.absolutePath)
                                put("TMPDIR", context.cacheDir.absolutePath)
                            }
                        }.start()
                    // Android 7 cannot redirect stderr to a file. Drain its pipe separately
                    // so extractor diagnostics cannot block stdout or corrupt the JSON result.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        kotlin.concurrent.thread(isDaemon = true, name = "gallery-stderr") {
                            runCatching {
                                process.errorStream.use { stream ->
                                    val buffer = ByteArray(8192)
                                    while (stream.read(buffer) != -1) { /* Discard diagnostics. */ }
                                }
                            }
                        }
                    }
                    continuation.invokeOnCancellation { process.destroy() }
                    try {
                        // Pipe the session to this child only; never put cookies in arguments or files.
                        process.outputStream.bufferedWriter(Charsets.UTF_8).use { input ->
                            if (argument != "--version") input.write(json.encodeToString(cookies))
                        }
                        val output = process.inputStream.use { stream ->
                            val bytes = stream.readBytesBounded(2 * 1024 * 1024)
                            bytes.toString(Charsets.UTF_8)
                        }
                        val exit = process.waitFor()
                        if (continuation.isActive) {
                            val structuredError = runCatching { json.decodeFromString<GalleryResult>(output).error }.getOrNull()
                            continuation.resumeWith(if (exit == 0 || structuredError != null) Result.success(output)
                                else Result.failure(IOException("The site did not return an accessible image gallery.")))
                        }
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWith(Result.failure(error))
                    } finally {
                        process.destroy()
                    }
                }
            } finally {
                errorFile.delete()
            }
        }
    }

    @Synchronized
    private fun prepareRuntime(): File {
        val directory = File(context.noBackupFilesDir, "gallery-dl-1.32.11-v3")
        val marker = File(directory, ".ready")
        if (marker.isFile) return directory
        directory.mkdirs()
        ZipInputStream(context.assets.open("gallery/runtime.zip")).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = File(directory, entry.name)
                check(target.canonicalPath.startsWith(directory.canonicalPath + File.separator))
                if (!entry.isDirectory) {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
            }
        }
        context.assets.open("gallery/extract.py").use { source -> File(directory, "extract.py").outputStream().use { source.copyTo(it) } }
        marker.writeText("1.32.11")
        return directory
    }
}

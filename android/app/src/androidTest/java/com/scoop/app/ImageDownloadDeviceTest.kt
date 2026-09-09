package com.scoop.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.core.model.ImageCandidate
import com.scoop.app.downloader.DownloadPaths
import com.scoop.app.downloader.ImageDownloader
import com.scoop.app.downloader.DownloadManager
import com.scoop.app.core.database.DownloadHistoryDao
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.extractor.GalleryImageExtractor
import com.scoop.app.extractor.ImageDiscovery
import com.scoop.app.util.PrefKeys
import com.scoop.app.util.PreferenceUtil
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class ImageDownloadDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val gallery get() = GlobalContext.get().get<GalleryImageExtractor>()

    @Test fun bundledGalleryRuntimeStartsOnAndroid() = runBlocking {
        assertTrue(gallery.version().contains("1.32.11"))
    }

    @Test fun extensionlessRedirectedImageDownloadsOriginalBytesToGallery() = runBlocking {
        ImageServer().use { server ->
            val discovery = ImageDiscovery(OkHttpClient(), gallery)
            val collection = discovery.directImage(server.url("/redirect"))!!
            assertEquals("image/png", collection.images.single().mimeType)
            assertEquals(8, collection.images.single().width)
            assertNull(discovery.directImage(server.url("/fake.png")))
            val downloader = ImageDownloader(context, OkHttpClient())
            val saved = mutableListOf<String>()
            try {
                repeat(2) {
                    val image = collection.images.single()
                    val task = task(collection.sourceUrl, image)
                    val path = downloader.download(task) { }
                    saved.add(path)
                    val uri = Uri.parse(path)
                    assertEquals("content", uri.scheme)
                    assertEquals("image/png", context.contentResolver.getType(uri))
                    assertArrayEquals(server.png, context.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
                    assertFalse(DownloadPaths.tempWorkspace(context, task.id).listFiles().orEmpty().isNotEmpty())
                    DownloadPaths.clearTempWorkspace(context, task.id)
                }
                assertNotEquals(saved[0], saved[1])
            } finally { saved.forEach { context.contentResolver.delete(Uri.parse(it), null, null) } }
        }
    }

    @Test fun webpageFallbackReturnsSelectableImagesAndRejectsFakeImageDownload() = runBlocking {
        ImageServer().use { server ->
            val collection = ImageDiscovery(OkHttpClient(), gallery).discover(server.url("/page"))
            assertEquals(2, collection.images.size)
            assertTrue(collection.images.all { it.headers["Referer"] == server.url("/page") })
            val bad = task(server.url("/page"), ImageCandidate(server.url("/fake.png")))
            val failure = runCatching { ImageDownloader(context, OkHttpClient()).download(bad) {} }.exceptionOrNull()
            assertNotNull(failure)
            assertTrue(failure!!.message!!.contains("instead of an image"))
        }
    }

    @Test fun cancellationRemovesPartialImage() = runBlocking {
        ImageServer().use { server ->
            val item = task(server.url("/slow"), ImageCandidate(server.url("/slow")))
            val job = launch { ImageDownloader(context, OkHttpClient()).download(item) {} }
            delay(300)
            job.cancelAndJoin()
            delay(300)
            assertFalse(java.io.File(context.cacheDir, "downloads_tmp/${item.id}").exists())
        }
    }

    @Test fun rapidImageBatchesPersistHistoryAndRespectIncognitoWithoutServiceCrash() = runBlocking {
        val previousIncognito = PreferenceUtil.getBoolean(PrefKeys.INCOGNITO, false)
        val manager = GlobalContext.get().get<DownloadManager>()
        val history = GlobalContext.get().get<DownloadHistoryDao>()
        val created = mutableListOf<DownloadTask>()
        ActivityScenario.launch(MainActivity::class.java).use {
            ImageServer().use { server ->
                try {
                    for (incognito in listOf(false, true)) {
                        PreferenceUtil.putBoolean(PrefKeys.INCOGNITO, incognito)
                        val task = manager.enqueue(DownloadRequest(server.url("/original"), DownloadKind.IMAGE,
                            image = ImageCandidate(server.url("/original"))), "Fast image test", server.url("/original"))
                        created.add(task)
                        withTimeout(15_000) { while (manager.tasks[task] !is DownloadStatus.Completed) {
                            val status = manager.tasks[task]
                            if (status is DownloadStatus.Failed) throw AssertionError(status.message)
                            delay(50)
                        } }
                        assertEquals(!incognito, history.getAll().any { item -> item.id == task.id && item.kind == "IMAGE" })
                        // Android reports a startForegroundService/stopService race asynchronously.
                        delay(6_000)
                    }
                } finally {
                    created.forEach { manager.deleteTaskAndFile(it.id) }
                    PreferenceUtil.putBoolean(PrefKeys.INCOGNITO, previousIncognito)
                }
            }
        }
    }

    private fun task(url: String, image: ImageCandidate) = DownloadTask(UUID.randomUUID().toString(),
        DownloadRequest(url, DownloadKind.IMAGE, image = image), "Image device test", image.url)

    internal class ImageServer : AutoCloseable {
        val transientRequests = java.util.concurrent.atomic.AtomicInteger()
        val png: ByteArray = ByteArrayOutputStream().also { output ->
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.BLUE)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
        }.toByteArray()
        private val socket = ServerSocket(0)
        private val executor = Executors.newCachedThreadPool()
        init {
            executor.submit {
                while (!socket.isClosed) {
                    try { val client = socket.accept(); executor.submit { serve(client) } }
                    catch (_: Exception) { }
                }
            }
        }
        fun url(path: String) = "http://127.0.0.1:${socket.localPort}$path"
        private fun serve(client: Socket) {
            client.use {
                try {
                    val reader = it.getInputStream().bufferedReader()
                    val path = reader.readLine()?.split(' ')?.getOrNull(1) ?: return
                    while (!reader.readLine().isNullOrEmpty()) { }
                    val output = it.getOutputStream()
                    if (path == "/transient" && transientRequests.incrementAndGet() == 1) {
                        output.write("HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                        return
                    }
                    if (path == "/redirect") {
                        output.write("HTTP/1.1 302 Found\r\nLocation: /original\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                        return
                    }
                    val body = when(path) {
                        "/page" -> "<title>Test gallery</title><img src='/original' alt='First'><img src='/second.png' alt='Second'>".toByteArray()
                        "/fake.png" -> "<html>Login required</html>".toByteArray()
                        "/slow" -> png + ByteArray(1024 * 1024)
                        else -> png
                    }
                    val type = if (path == "/page") "text/html" else "application/octet-stream"
                    output.write("HTTP/1.1 200 OK\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    if (path == "/slow") {
                        for (chunk in body.asList().chunked(4096)) { output.write(chunk.toByteArray()); output.flush(); Thread.sleep(30) }
                    } else output.write(body)
                } catch (_: Exception) { }
            }
        }
        override fun close() { socket.close(); executor.shutdownNow() }
    }
}

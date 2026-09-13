package com.scoop.app.core.network

import com.scoop.app.core.model.FormatSelector
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.DownloadTask
import com.scoop.app.core.model.ImageCandidate
import com.scoop.app.downloader.toPersistedQueueEntry
import java.net.Socket
import java.net.URI
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class SecureUrlTest {
    @Test fun acceptsOnlyHttpsWebUrls() {
        assertNotNull(SecureUrl.parse("https://example.com/media?id=1"))
        listOf("http://example.com/media", "file:///data/local/file", "javascript:alert(1)", "not a url")
            .forEach { assertNull(it, SecureUrl.parse(it)) }
    }

    @Test fun rejectsLocalAndCredentialBearingDestinations() {
        listOf(
            "https://localhost/file",
            "https://127.0.0.1/file",
            "https://192.168.1.10/file",
            "https://169.254.1.2/file",
            "https://100.64.0.1/file",
            "https://203.0.113.9/file",
            "https://[::1]/file",
            "https://[fc00::1]/file",
            "https://user:secret@example.com/file",
        ).forEach { assertNull(it, SecureUrl.parse(it)) }
    }

    @Test fun appliesStrictCrossOriginReferrerAndRedactsLogs() {
        assertEquals(
            "https://source.example/",
            SecureUrl.referrerFor("https://source.example/private?a=secret#fragment", "https://cdn.example/image.jpg"),
        )
        assertEquals(
            "https://source.example/private",
            SecureUrl.referrerFor("https://source.example/private?a=secret#fragment", "https://source.example/image.jpg"),
        )
        assertEquals(
            "https://source.example/",
            SecureUrl.redactedForLog("https://user:secret@source.example/private?a=secret#fragment"),
        )
    }

    @Test fun formatSelectorsKeepAudioAndApplyTheConfiguredCap() {
        assertEquals("bestvideo*+bestaudio/best", FormatSelector.video())
        assertEquals("bestvideo[height<=720]+bestaudio/best[height<=720]", FormatSelector.video(720))
        assertEquals("worstvideo+worstaudio/worst", FormatSelector.video(low = true))
        assertEquals("worstaudio/worst", FormatSelector.audio(low = true))
    }

    @Test fun durableQueueNeverPersistsAuthenticationHeaders() {
        val task = DownloadTask(
            id = "task",
            request = DownloadRequest(
                url = "https://example.com/post",
                kind = DownloadKind.IMAGE,
                image = ImageCandidate(
                    url = "https://cdn.example.com/image.jpg",
                    headers = mapOf("Cookie" to "secret", "Authorization" to "Bearer secret", "Accept" to "image/*"),
                ),
            ),
            title = "Image",
            thumbnailUrl = "https://cdn.example.com/image.jpg?token=secret",
        )
        val persisted = task.toPersistedQueueEntry(2)
        assertEquals(true, persisted.requiresReanalysis)
        assertEquals("", persisted.task.request.url)
        assertNull(persisted.task.request.image)
        assertNull(persisted.task.thumbnailUrl)
        assertEquals(2, persisted.retryAttempt)
    }

    @Test fun subprocessProxyRejectsPrivateConnectTargets() {
        PublicHttpsProxy().use { proxy ->
            val endpoint = URI(proxy.url)
            Socket(endpoint.host, endpoint.port).use { socket ->
                socket.soTimeout = 2_000
                socket.getOutputStream().apply {
                    write("CONNECT 127.0.0.1:443 HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n".toByteArray())
                    flush()
                }
                val response = socket.getInputStream().bufferedReader().readLine()
                assertEquals("HTTP/1.1 403 Forbidden", response)
            }
        }
    }
}

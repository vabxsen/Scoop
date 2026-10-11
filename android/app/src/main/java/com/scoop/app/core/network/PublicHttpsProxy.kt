package com.scoop.app.core.network

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Loopback CONNECT proxy for bundled Python tools. It resolves once through [PublicNetworkDns]
 * and connects to that exact approved address, so subprocess redirects cannot bypass OkHttp's
 * public-network boundary or win a validate-then-resolve DNS race.
 */
class PublicHttpsProxy : AutoCloseable {
    private val server = ServerSocket(0, 16, java.net.InetAddress.getByName("127.0.0.1"))
    private val workers = Executors.newCachedThreadPool()
    private val sockets = ConcurrentHashMap.newKeySet<Socket>()
    @Volatile private var closed = false

    val url: String = "http://127.0.0.1:${server.localPort}"

    init {
        workers.execute {
            while (!closed) {
                try {
                    val client = server.accept()
                    sockets.add(client)
                    workers.execute { handle(client) }
                } catch (_: IOException) {
                    if (!closed) continue
                }
            }
        }
    }

    private fun handle(client: Socket) {
        var upstream: Socket? = null
        try {
            client.soTimeout = IO_TIMEOUT_MS
            val input = BufferedInputStream(client.getInputStream())
            val header = readHeader(input)
            val requestLine = header.lineSequence().firstOrNull().orEmpty().split(' ')
            if (requestLine.size < 3 || requestLine[0] != "CONNECT") throw IOException("HTTPS CONNECT required")
            val destination = URI("https://${requestLine[1]}")
            val host = destination.host ?: throw IOException("Invalid CONNECT host")
            val port = destination.port.takeIf { it in 1..65535 } ?: 443
            if (SecureUrl.parse("https://${formatHost(host)}:$port/") == null) throw IOException("Blocked destination")
            val address = PublicNetworkDns.lookup(host).first()
            upstream = Socket().also {
                sockets.add(it)
                it.connect(InetSocketAddress(address, port), CONNECT_TIMEOUT_MS)
                it.soTimeout = IO_TIMEOUT_MS
            }
            client.getOutputStream().apply {
                write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray(Charsets.US_ASCII))
                flush()
            }
            val remote = requireNotNull(upstream)
            // A download is one-directional for long stretches (the client only reads), so the
            // read timeout must not end the tunnel while the other direction is still moving.
            val lastActivity = AtomicLong(System.nanoTime())
            val reverse = workers.submit { relay(remote.getInputStream(), client, lastActivity) }
            relay(input, remote, lastActivity)
            runCatching { reverse.get() }
        } catch (_: Exception) {
            runCatching {
                client.getOutputStream().apply {
                    write("HTTP/1.1 403 Forbidden\r\nConnection: close\r\nContent-Length: 0\r\n\r\n".toByteArray(Charsets.US_ASCII))
                    flush()
                }
            }
        } finally {
            upstream?.let { sockets.remove(it); runCatching { it.close() } }
            sockets.remove(client)
            runCatching { client.close() }
        }
    }

    /** Copies [from] into [target] until EOF, an error, or both directions idle for [IO_TIMEOUT_MS],
     * then half-closes [target] so the peer sees the end of this direction. */
    private fun relay(from: InputStream, target: Socket, lastActivity: AtomicLong) {
        try {
            val output = target.getOutputStream()
            val buffer = ByteArray(RELAY_BUFFER_BYTES)
            while (true) {
                val count =
                    try {
                        from.read(buffer)
                    } catch (_: SocketTimeoutException) {
                        if (System.nanoTime() - lastActivity.get() >= IO_TIMEOUT_NANOS) break else continue
                    }
                if (count == -1) break
                lastActivity.set(System.nanoTime())
                output.write(buffer, 0, count)
            }
        } catch (_: IOException) {
        } finally {
            runCatching { target.shutdownOutput() }
        }
    }

    private fun readHeader(input: BufferedInputStream): String {
        val bytes = ArrayList<Byte>()
        var matched = 0
        while (bytes.size < MAX_HEADER_BYTES) {
            val value = input.read()
            if (value == -1) throw IOException("Incomplete proxy request")
            bytes.add(value.toByte())
            matched = when {
                matched == 0 && value == '\r'.code -> 1
                matched == 1 && value == '\n'.code -> 2
                matched == 2 && value == '\r'.code -> 3
                matched == 3 && value == '\n'.code -> return bytes.toByteArray().toString(Charsets.US_ASCII)
                value == '\r'.code -> 1
                else -> 0
            }
        }
        throw IOException("Proxy request header too large")
    }

    override fun close() {
        closed = true
        runCatching { server.close() }
        sockets.toList().forEach { runCatching { it.close() } }
        workers.shutdownNow()
    }

    private fun formatHost(host: String): String = if (':' in host && !host.startsWith("[")) "[$host]" else host

    companion object {
        private const val MAX_HEADER_BYTES = 16 * 1024
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val IO_TIMEOUT_MS = 30_000
        private val IO_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(IO_TIMEOUT_MS.toLong())
        private const val RELAY_BUFFER_BYTES = 16 * 1024
    }
}

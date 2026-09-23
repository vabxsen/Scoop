package com.scoop.app.core.media

import android.content.Context
import android.util.AtomicFile
import com.scoop.app.R
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Installs only the APK's pinned runtime. No executable downloads or self-updates. */
internal object BundledYtDlp {
    const val VERSION = "2026.08.19"
    const val SHA256 = "1fa6733c37ea6fb51c99ad8fe785e7b7e5f3246c9b980230329d4fb72ed8d4d6"

    @Synchronized
    fun install(context: Context) {
        // This is the documented layout of the pinned youtubedl-android wrapper.
        // Its init() only copies yt-dlp if absent, so app upgrades must replace stale copies here.
        install(File(context.noBackupFilesDir, "youtubedl-android/yt-dlp/yt-dlp")) {
            context.resources.openRawResource(R.raw.ytdlp)
        }
    }

    internal fun install(destination: File, bundled: () -> InputStream) {
        val target = AtomicFile(destination)
        val installedHash = runCatching { target.openRead().use(::sha256) }.getOrNull()
        if (installedHash == SHA256) return
        val output = target.startWrite()
        try {
            bundled().use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
                check(hex(digest.digest()) == SHA256) { "Bundled video engine checksum mismatch" }
            }
            target.finishWrite(output)
        } catch (error: Throwable) {
            target.failWrite(output)
            throw error
        }
    }

    private fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        return hex(digest.digest())
    }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
}

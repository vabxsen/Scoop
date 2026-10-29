package com.scoop.app.extractor

import android.content.Context
import java.io.File

/**
 * Hands the optional Instagram session to one yt-dlp run. yt-dlp only reads cookies from a file,
 * so the session is written to app-private, no-backup storage just for the run and deleted in a
 * `finally` - on success, failure and cancellation alike. Non-Instagram links never get a file.
 */
internal object InstagramCookieFile {
    private const val DIRECTORY = "ytdlp-session"

    suspend fun <T> use(context: Context, url: String, block: suspend (cookieFile: File?) -> T): T {
        val contents = instagramNetscapeCookies(InstagramSession.cookiesFor(url))
        val file = contents?.let { write(context, it) }
        try {
            return block(file)
        } finally {
            file?.delete()
        }
    }

    /** Removes any file left behind by a process that died mid-run. Call once at startup. */
    fun sweep(context: Context) {
        File(context.noBackupFilesDir, DIRECTORY).deleteRecursively()
    }

    private fun write(context: Context, contents: String): File {
        val directory = File(context.noBackupFilesDir, DIRECTORY).apply { mkdirs() }
        return File.createTempFile("instagram", ".txt", directory).apply { writeText(contents) }
    }
}

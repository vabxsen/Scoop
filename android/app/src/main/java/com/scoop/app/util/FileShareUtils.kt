package com.scoop.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

/** [filePath] is either a real filesystem path or a MediaStore content:// URI (for downloads
 * published to the shared Movies/Music collection - see DownloadPaths.publishToMediaStore). */
object FileShareUtils {
    private fun contentUriFor(context: Context, filePath: String): Uri =
        if (filePath.startsWith("content://")) {
            Uri.parse(filePath)
        } else {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", File(filePath))
        }

    private fun mimeTypeFor(context: Context, filePath: String): String =
        if (filePath.startsWith("content://")) {
            context.contentResolver.getType(Uri.parse(filePath)) ?: "*/*"
        } else {
            val extension = filePath.substringAfterLast('.', "").lowercase()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        }

    /** Ready-to-launch ACTION_VIEW intent for [filePath] - e.g. to wrap in a PendingIntent for a notification. */
    fun openFileIntent(context: Context, filePath: String): Intent {
        val uri = contentUriFor(context, filePath)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeTypeFor(context, filePath))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openFile(context: Context, filePath: String) {
        context.startActivity(openFileIntent(context, filePath))
    }

    fun installApk(context: Context, filePath: String) {
        val uri = contentUriFor(context, filePath)
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }

    fun shareFile(context: Context, filePath: String) {
        val uri = contentUriFor(context, filePath)
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = mimeTypeFor(context, filePath)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(Intent.createChooser(intent, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    /** Size in bytes of the file/media item behind [filePath], or null if it can't be determined. */
    fun sizeBytes(context: Context, filePath: String): Long? =
        if (filePath.startsWith("content://")) {
            runCatching {
                context.contentResolver.query(Uri.parse(filePath), arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else null
                }
            }.getOrNull()
        } else {
            File(filePath).takeIf { it.exists() }?.length()
        }
}

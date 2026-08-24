package com.scoop.app.downloader

import android.content.Context
import android.os.Environment
import com.scoop.app.core.model.DownloadKind
import java.io.File

/** Where finished downloads live, shared by the download engine and the Settings "save location" row. */
object DownloadPaths {
    fun outputDir(context: Context, kind: DownloadKind): File =
        (when (kind) {
            DownloadKind.VIDEO -> context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            DownloadKind.AUDIO_ONLY -> context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        } ?: context.filesDir).apply { mkdirs() }

    /** Human-readable form of [outputDir] for display only (e.g. Settings), not a real filesystem path. */
    fun displayLabel(kind: DownloadKind): String =
        when (kind) {
            DownloadKind.VIDEO -> "Movies/Scoop"
            DownloadKind.AUDIO_ONLY -> "Music/Scoop"
        }

    private fun tempRoot(context: Context): File = File(context.cacheDir, "downloads_tmp")

    fun tempWorkspace(context: Context, taskId: String): File =
        File(tempRoot(context), taskId).apply { mkdirs() }

    fun clearTempWorkspace(context: Context, taskId: String) {
        File(tempRoot(context), taskId).deleteRecursively()
    }

    /** Removes any leftover per-task temp workspaces from a previous process that died mid-download. */
    fun sweepStaleTempWorkspaces(context: Context) {
        tempRoot(context).listFiles()?.forEach { it.deleteRecursively() }
    }

    /** Moves [source] into [targetDir] as [desiredName], appending " (n)" on a name collision. */
    fun moveWithDedup(source: File, targetDir: File, desiredName: String): File {
        targetDir.mkdirs()
        val ext = desiredName.substringAfterLast('.', "")
        val base = if (ext.isEmpty()) desiredName else desiredName.removeSuffix(".$ext")
        var candidate = File(targetDir, desiredName)
        var index = 1
        while (candidate.exists()) {
            val suffixedName = if (ext.isEmpty()) "$base ($index)" else "$base ($index).$ext"
            candidate = File(targetDir, suffixedName)
            index++
        }
        source.copyTo(candidate, overwrite = false)
        source.delete()
        return candidate
    }
}

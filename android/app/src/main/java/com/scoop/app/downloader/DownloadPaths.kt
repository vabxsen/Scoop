package com.scoop.app.downloader

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.util.PrefKeys
import com.scoop.app.util.PreferenceUtil
import java.io.File
import java.io.IOException

/** Where finished downloads live, shared by the download engine and the Settings "save location" row. */
object DownloadPaths {
    /** The user's chosen save-everything-here folder (SAF tree), if they picked one in Settings ->
     * Storage. Null means "use the default Movies/Scoop and Music/Scoop locations". */
    fun customFolderUri(context: Context): Uri? =
        PreferenceUtil.getString(PrefKeys.CUSTOM_SAVE_FOLDER_URI, "").takeIf { it.isNotEmpty() }?.let(Uri::parse)?.takeIf { uri ->
            // A tree granted to a since-uninstalled/reinstalled app, or revoked by the user in
            // system Settings, would otherwise silently fail every download until re-picked.
            context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
        }

    /** Persists [treeUri] as the custom save folder, taking a permanent grant so it survives reboots. */
    fun setCustomFolder(context: Context, treeUri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        clearCustomFolder(context, releasePermission = false)
        PreferenceUtil.putString(PrefKeys.CUSTOM_SAVE_FOLDER_URI, treeUri.toString())
    }

    /** Reverts to the default Movies/Scoop and Music/Scoop locations. */
    fun clearCustomFolder(context: Context, releasePermission: Boolean = true) {
        if (releasePermission) {
            customFolderUri(context)?.let { uri ->
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
            }
        }
        PreferenceUtil.putString(PrefKeys.CUSTOM_SAVE_FOLDER_URI, "")
    }

    /** A short, human-readable name for the custom folder (its own display name, not a full path -
     * SAF doesn't expose one), for showing next to the "Change" row in Settings. */
    fun customFolderLabel(context: Context, treeUri: Uri): String? = DocumentFile.fromTreeUri(context, treeUri)?.name

    /**
     * Copies [source] into the user's chosen custom folder as [desiredName], appending " (n)" on a
     * name collision. Returns the new document's content:// URI, or null if no custom folder is
     * set or the write fails (caller falls back to [publishToMediaStore]/[outputDir]).
     */
    fun saveToCustomFolder(context: Context, source: File, desiredName: String): String? {
        val treeUri = customFolderUri(context) ?: return null
        val folder = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val ext = desiredName.substringAfterLast('.', "")
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        val base = if (ext.isEmpty()) desiredName else desiredName.removeSuffix(".$ext")

        var candidateName = desiredName
        var index = 1
        while (folder.findFile(candidateName) != null) {
            candidateName = if (ext.isEmpty()) "$base ($index)" else "$base ($index).$ext"
            index++
        }

        return try {
            val newFile = folder.createFile(mimeType, candidateName) ?: return null
            context.contentResolver.openOutputStream(newFile.uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
                ?: throw IOException("Could not open output stream for ${newFile.uri}")
            source.delete()
            newFile.uri.toString()
        } catch (e: Exception) {
            null
        }
    }

    /** Legacy per-app fallback - private to this app, invisible to Gallery/Files and other apps.
     * Only used pre-Android 10 (see [publishToMediaStore]) or if a MediaStore publish fails. */
    fun outputDir(context: Context, kind: DownloadKind): File =
        (when (kind) {
            DownloadKind.VIDEO -> context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            DownloadKind.AUDIO_ONLY -> context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        } ?: context.filesDir).apply { mkdirs() }

    /** Human-readable form of the save location for display only (e.g. Settings), not a real filesystem path. */
    fun displayLabel(kind: DownloadKind): String =
        when (kind) {
            DownloadKind.VIDEO -> "Movies/Scoop"
            DownloadKind.AUDIO_ONLY -> "Music/Scoop"
        }

    private fun relativePath(kind: DownloadKind): String =
        when (kind) {
            DownloadKind.VIDEO -> Environment.DIRECTORY_MOVIES + "/Scoop"
            DownloadKind.AUDIO_ONLY -> Environment.DIRECTORY_MUSIC + "/Scoop"
        }

    /**
     * Publishes [source] into the device's shared Movies/Music collection via MediaStore, so it
     * shows up in the Gallery/Files apps immediately - unlike [outputDir], which is app-private
     * storage other apps (and the system media scanner) can't see. Deletes [source] and returns
     * the new item's content:// URI on success, leaving [source] untouched on failure so the
     * caller can fall back to [outputDir].
     *
     * Only available from Android 10 (scoped storage introduced MediaStore.RELATIVE_PATH); older
     * versions return null and always use the legacy app-private location instead of adding a
     * runtime WRITE_EXTERNAL_STORAGE permission flow for a vanishingly small population.
     */
    fun publishToMediaStore(context: Context, kind: DownloadKind, source: File, desiredName: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver
        val collection =
            when (kind) {
                DownloadKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                DownloadKind.AUDIO_ONLY -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        val ext = desiredName.substringAfterLast('.', "")
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, desiredName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath(kind))
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        val itemUri = runCatching { resolver.insert(collection, values) }.getOrNull() ?: return null

        return try {
            resolver.openOutputStream(itemUri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
                ?: throw IOException("Could not open output stream for $itemUri")
            resolver.update(itemUri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            source.delete()
            itemUri.toString()
        } catch (e: Exception) {
            resolver.delete(itemUri, null, null)
            null
        }
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

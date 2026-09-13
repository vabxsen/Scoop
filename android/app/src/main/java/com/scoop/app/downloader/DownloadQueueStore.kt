package com.scoop.app.downloader

import android.content.Context
import android.util.AtomicFile
import com.scoop.app.core.model.DownloadTask
import java.io.File
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PersistedQueueEntry(
    val task: DownloadTask,
    val retryAttempt: Int = 0,
    val requiresReanalysis: Boolean = false,
)

internal fun DownloadTask.toPersistedQueueEntry(retryAttempt: Int): PersistedQueueEntry {
    val headers = request.image?.headers.orEmpty()
    val sourceHasSecret = request.url.toHttpUrlOrNull()?.queryParameterNames.orEmpty().any { name ->
        name.contains(Regex("(?i)(token|signature|credential|password|passwd|auth|api[_-]?key|x-amz-|expires)"))
    }
    val sensitive = request.customArgs != null || sourceHasSecret ||
        headers.keys.any { it.equals("Cookie", true) || it.equals("Authorization", true) } ||
            request.image?.url?.toHttpUrlOrNull()?.query != null
    val safeImage = request.image?.copy(
        headers = headers.filterKeys { !it.equals("Cookie", true) && !it.equals("Authorization", true) },
    )
    val requiresReanalysis = sensitive
    return PersistedQueueEntry(
        task = copy(
            request = request.copy(
                url = request.url.takeUnless { requiresReanalysis }.orEmpty(),
                customArgs = null,
                image = if (requiresReanalysis) null else safeImage,
            ),
            thumbnailUrl = null,
        ),
        retryAttempt = retryAttempt,
        requiresReanalysis = requiresReanalysis,
    )
}

/** Small, crash-safe queue journal. Sensitive request headers are never written to disk. */
class DownloadQueueStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "download-queue.json"))
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun read(): List<PersistedQueueEntry> =
        runCatching {
            file.openRead().bufferedReader().use { reader ->
                json.decodeFromString<List<PersistedQueueEntry>>(reader.readText())
            }
        }.getOrDefault(emptyList())

    @Synchronized
    fun write(entries: List<PersistedQueueEntry>) {
        if (entries.isEmpty()) {
            file.delete()
            return
        }
        val output = file.startWrite()
        try {
            output.write(json.encodeToString(entries).toByteArray(Charsets.UTF_8))
            output.flush()
            file.finishWrite(output)
        } catch (error: Throwable) {
            file.failWrite(output)
            throw error
        }
    }
}

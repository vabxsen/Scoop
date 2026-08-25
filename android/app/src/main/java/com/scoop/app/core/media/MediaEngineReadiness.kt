package com.scoop.app.core.media

import android.content.Context
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MediaEngineReadiness"

/**
 * YoutubeDL/FFmpeg/Aria2c.init() extract bundled native binaries to disk and must complete before
 * any YoutubeDL.getInstance() call, or the library throws "instance not initialized". Startup
 * kicks this off on a background coroutine so it doesn't block the UI - extractor/download code
 * must await [awaitReady] rather than assume init already finished by the time it runs, or a
 * download started right after app launch can race it.
 */
class MediaEngineReadiness(private val context: Context) {
    private val ready = CompletableDeferred<Unit>()

    fun startInitializing(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                YoutubeDL.init(context)
                FFmpeg.init(context)
                Aria2c.init(context)
                ready.complete(Unit)
            } catch (t: Throwable) {
                // Extraction/download calls will surface a clear error once they await readiness.
                ready.completeExceptionally(t)
                return@launch
            }

            runCatching { YoutubeDL.getInstance().updateYoutubeDL(context) }
                .onSuccess { Log.i(TAG, "yt-dlp update check: $it") }
                .onFailure { Log.w(TAG, "yt-dlp self-update failed, continuing with the bundled version", it) }
        }
    }

    suspend fun awaitReady() = ready.await()
}

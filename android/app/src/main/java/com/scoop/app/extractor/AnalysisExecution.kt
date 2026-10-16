package com.scoop.app.extractor

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull

// Includes network requests and local JavaScript challenge solving on slower ARM devices.
// A socket timeout alone cannot bound the latter; keep a separate, cancellable deadline.
internal const val ANALYZE_TIMEOUT_MS = 180_000L

internal suspend fun <T> executeWithAnalysisTimeout(
    timeoutMs: Long,
    destroyProcess: () -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    execute: suspend () -> T,
): T = supervisorScope {
    // Killing a blocking wrapper can throw an ordinary exception on its IO thread. Do not
    // let that teardown failure replace the caller's cancellation (or our timeout).
    val execution = async(dispatcher) { execute() }
    try {
        // Only our own deadline becomes an IOException. A caller's timeout/cancellation must
        // propagate unchanged, including when the configure sheet is closed or replaced.
        val completed = withTimeoutOrNull(timeoutMs) { Result.success(execution.await()) }
            ?: throw IOException(
                "Video analysis did not finish within ${timeoutMs / 1_000} seconds. " +
                    "The connection or video platform checks may be slow. Please try again.",
            )
        completed.getOrThrow()
    } finally {
        destroyProcess()
        execution.cancel()
    }
}

package com.scoop.app.extractor

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisExecutionTest {
    @Test fun slowSuccessfulAnalysisSurvivesTheFormerSixtySecondCutoff() = runTest {
        var destroyed = 0
        val output = executeWithAnalysisTimeout(
            ANALYZE_TIMEOUT_MS, { destroyed++ }, StandardTestDispatcher(testScheduler),
        ) {
            delay(75_000)
            "video metadata"
        }
        assertEquals("video metadata", output)
        assertEquals(75_000L, currentTime)
        assertEquals(1, destroyed)
    }

    @Test fun hungAnalysisIsBoundedAndCleanedUp() = runTest {
        var destroyed = 0
        var stopped = false
        val failure = runCatching {
            executeWithAnalysisTimeout(
                ANALYZE_TIMEOUT_MS, { destroyed++ }, StandardTestDispatcher(testScheduler),
            ) {
                try { awaitCancellation() } finally { stopped = true }
            }
        }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertTrue(failure!!.message!!.contains("180 seconds"))
        assertEquals(ANALYZE_TIMEOUT_MS, currentTime)
        assertEquals(1, destroyed)
        assertTrue(stopped)
    }

    @Test fun closingTheSheetCancelsImmediately() = runTest {
        var destroyed = 0
        val analysis = async {
            executeWithAnalysisTimeout(
                ANALYZE_TIMEOUT_MS, { destroyed++ }, StandardTestDispatcher(testScheduler),
            ) { awaitCancellation() }
        }
        runCurrent()
        advanceTimeBy(1_000)
        analysis.cancel()
        val failure = runCatching { analysis.await() }.exceptionOrNull()
        runCurrent()
        assertTrue(failure is CancellationException)
        assertEquals(1_000L, currentTime)
        assertEquals(1, destroyed)
    }

    @Test fun callerDeadlineRemainsCancellationRatherThanMediaFailure() = runTest {
        var destroyed = 0
        val failure = runCatching {
            withTimeout(1_000) {
                executeWithAnalysisTimeout(
                    ANALYZE_TIMEOUT_MS, { destroyed++ }, StandardTestDispatcher(testScheduler),
                ) { awaitCancellation() }
            }
        }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        assertEquals(1_000L, currentTime)
        assertEquals(1, destroyed)
    }

    @Test fun extractorFailureIsPreserved() = runTest {
        var destroyed = 0
        val rootCause = IllegalArgumentException("Original extractor detail")
        val original = IOException("Requested video is unavailable", rootCause)
        val failure = runCatching {
            executeWithAnalysisTimeout(
                ANALYZE_TIMEOUT_MS, { destroyed++ }, StandardTestDispatcher(testScheduler),
            ) { throw original }
        }.exceptionOrNull()
        // Coroutine debug/stacktrace recovery (also enabled by JVM -ea) may copy the
        // exception at await(), retaining the original as its cause. Check the exact
        // failure semantics and original chain rather than only the wrapper's identity.
        assertEquals(original.javaClass, failure?.javaClass)
        assertEquals(original.message, failure?.message)
        val preservedOriginal = if (failure === original) failure else failure?.cause
        assertSame(original, preservedOriginal)
        assertSame(rootCause, preservedOriginal?.cause)
        assertEquals(0L, currentTime)
        assertEquals(1, destroyed)
    }

    @Test fun nullableSuccessIsNotATimeout() = runTest {
        val output = executeWithAnalysisTimeout<String?>(
            ANALYZE_TIMEOUT_MS, {}, StandardTestDispatcher(testScheduler),
        ) { null }
        assertNull(output)
    }

    @Test fun blockingWrapperTeardownDoesNotReplaceCallerCancellation() = runBlocking {
        val started = CountDownLatch(1)
        val destroyed = CountDownLatch(1)
        val analysis = async(Dispatchers.Default) {
            executeWithAnalysisTimeout(ANALYZE_TIMEOUT_MS, { destroyed.countDown() }) {
                started.countDown()
                check(destroyed.await(5, TimeUnit.SECONDS)) { "Cleanup was never called" }
                // YoutubeDL's CanceledException is an ordinary Exception, not coroutine cancellation.
                throw IOException("Subprocess exited after destroy")
            }
        }
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS))
            analysis.cancel()
            val failure = runCatching { analysis.await() }.exceptionOrNull()
            assertTrue(failure is CancellationException)
        } finally {
            destroyed.countDown()
            analysis.cancel()
            analysis.join()
        }
    }

    @Test fun blockingWrapperTeardownDoesNotReplaceAnalysisTimeout() = runBlocking {
        val destroyed = CountDownLatch(1)
        val failure = runCatching {
            executeWithAnalysisTimeout(100, { destroyed.countDown() }) {
                check(destroyed.await(5, TimeUnit.SECONDS)) { "Cleanup was never called" }
                throw IOException("Subprocess exited after destroy")
            }
        }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertTrue(failure!!.message!!.startsWith("Video analysis did not finish"))
        assertEquals(0L, destroyed.count)
    }
}

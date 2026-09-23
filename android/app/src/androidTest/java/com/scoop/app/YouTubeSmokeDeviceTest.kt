package com.scoop.app

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.scoop.app.core.model.DownloadKind
import com.scoop.app.core.model.DownloadRequest
import com.scoop.app.core.model.DownloadStatus
import com.scoop.app.downloader.DownloadManager
import com.scoop.app.extractor.MediaExtractor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Opt-in live tests: -e youtubeSmoke true. Downloads real low-quality video/audio, then removes them. */
@RunWith(AndroidJUnit4::class)
class YouTubeSmokeDeviceTest {
    @Test fun optionalClientDiagnostic() = runBlocking {
        val client = InstrumentationRegistry.getArguments().getString("youtubeClient")
        assumeTrue(client != null)
        GlobalContext.get().get<com.scoop.app.core.media.MediaEngineReadiness>().awaitReady()
        com.scoop.app.core.network.PublicHttpsProxy().use { proxy ->
            val request = com.yausername.youtubedl_android.YoutubeDLRequest(
                "https://www.youtube.com/watch?v=hiMPy5769Xc",
            ).apply {
                addOption("--simulate")
                addOption("--no-playlist")
                addOption("--socket-timeout", "10")
                addOption("--retries", "0")
                addOption("--extractor-retries", "0")
                addOption("--print", "title")
                addOption("--extractor-args", "youtube:player_client=$client")
                addOption("--proxy", proxy.url)
            }
            val result = com.yausername.youtubedl_android.YoutubeDL.getInstance().execute(request)
            assertTrue(result.out.isNotBlank())
        }
    }

    private fun requireLiveTest() = assumeTrue(
        InstrumentationRegistry.getArguments().getString("youtubeSmoke") == "true",
    )

    @Test fun reporterLinksAnalyzeAsMedia() = runBlocking {
        requireLiveTest()
        val extractor = GlobalContext.get().get<MediaExtractor>()
        for (id in listOf("hiMPy5769Xc", "Z8OgO5pHwxI", "8cMzvVg3y10")) {
            val info = extractor.analyze("https://www.youtube.com/watch?v=$id").getOrThrow()
            assertTrue(info.title.isNotBlank())
            assertTrue(info.sourceUrl.contains(id))
        }
    }

    @Test fun videoAndAudioDownloadThroughRealQueue() = runBlocking {
        requireLiveTest()
        val manager = GlobalContext.get().get<DownloadManager>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        ActivityScenario.launch(MainActivity::class.java).use {
            for (kind in listOf(DownloadKind.VIDEO, DownloadKind.AUDIO_ONLY)) {
                val task = manager.enqueue(DownloadRequest(
                    "https://www.youtube.com/watch?v=hiMPy5769Xc", kind,
                    formatId = if (kind == DownloadKind.VIDEO) "worst[ext=mp4]/worst" else "worstaudio/worst",
                    audioContainer = if (kind == DownloadKind.AUDIO_ONLY) "mp3" else null,
                ), "YouTube smoke test", null)
                try {
                    val completed = withTimeout(300_000) {
                        while (true) {
                            when (val state = manager.tasks[task]) {
                                is DownloadStatus.Failed -> throw AssertionError(state.message)
                                is DownloadStatus.Completed -> return@withTimeout state
                                else -> delay(200)
                            }
                        }
                        @Suppress("UNREACHABLE_CODE")
                        error("unreachable")
                    }
                    val path = requireNotNull(completed.filePath)
                    MediaMetadataRetriever().use { metadata ->
                        metadata.setDataSource(context, Uri.parse(path))
                        val key = if (kind == DownloadKind.VIDEO) MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO
                            else MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
                        assertEquals("yes", metadata.extractMetadata(key))
                        assertTrue(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong() > 0)
                    }
                } finally { manager.deleteTaskAndFile(task.id) }
            }
        }
    }
}

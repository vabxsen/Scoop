package com.scoop.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scoop.app.core.media.BundledYtDlp
import com.scoop.app.core.media.MediaEngineReadiness
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class BundledYtDlpDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun packagedRuntimeStartsAndReportsPinnedVersion() = runBlocking {
        GlobalContext.get().get<MediaEngineReadiness>().awaitReady()
        val response = YoutubeDL.getInstance().execute(YoutubeDLRequest(emptyList<String>()).apply {
            addOption("--version")
        })
        assertEquals(BundledYtDlp.VERSION, response.out.trim())
    }

    @Test fun staleRuntimeIsReplacedAndInvalidReplacementRollsBack() {
        val directory = File(context.cacheDir, "runtime-test-${System.nanoTime()}").apply { mkdirs() }
        val target = File(directory, "yt-dlp")
        try {
            target.writeText("old extractor")
            try {
                BundledYtDlp.install(target) { "corrupt resource".byteInputStream() }
                fail("Invalid resource should fail checksum validation")
            } catch (_: IllegalStateException) { }
            assertEquals("old extractor", target.readText())
            BundledYtDlp.install(target) { context.resources.openRawResource(R.raw.ytdlp) }
            assertTrue(target.length() > 1_000_000)
            BundledYtDlp.install(target) { error("Current runtime must not be rewritten") }
        } finally { directory.deleteRecursively() }
    }
}

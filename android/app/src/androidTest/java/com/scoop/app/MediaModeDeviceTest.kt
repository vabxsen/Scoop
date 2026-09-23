package com.scoop.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.scoop.app.core.model.ImageCandidate
import com.scoop.app.core.model.ImageCollection
import com.scoop.app.core.model.MediaInfo
import com.scoop.app.core.model.PlaylistInfo
import com.scoop.app.downloader.DownloadManager
import com.scoop.app.extractor.ImageSource
import com.scoop.app.extractor.MediaExtractor
import com.scoop.app.ui.screen.home.ConfigureUiState
import com.scoop.app.ui.screen.home.HomeViewModel
import com.scoop.app.util.PrefKeys
import com.scoop.app.util.PreferenceUtil
import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Offline regressions: a failed video must never become a thumbnail download. */
@RunWith(AndroidJUnit4::class)
class MediaModeDeviceTest {
    private val link = "https://www.youtube.com/watch?v=hiMPy5769Xc"
    private class Images : ImageSource {
        var calls = 0
        var direct = false
        val collection = ImageCollection("https://example.com/", "Test", listOf(
            ImageCandidate("https://example.com/photo.jpg", "Photo", "image/jpeg"),
        ))
        override suspend fun directImage(url: String) = if (direct) collection else null
        override suspend fun discover(url: String): ImageCollection { calls++; return collection }
    }

    @Test fun mediaFailureDoesNotDiscoverOrEnqueueThumbnails() = onMain {
        for (configureFirst in listOf(true, false)) {
            PreferenceUtil.putBoolean(PrefKeys.CONFIGURE_BEFORE_DOWNLOAD, configureFirst)
            val images = Images()
            val manager = GlobalContext.get().get<DownloadManager>()
            val before = manager.tasks.size
            val vm = viewModel(images, IOException("Requested format is not available"))
            vm.onUrlChange(link)
            vm.startDownloadFlow()
            assertTrue(vm.configureState is ConfigureUiState.Error)
            assertTrue((vm.configureState as ConfigureUiState.Error).message.contains("Requested format"))
            assertEquals(0, images.calls)
            assertEquals(before, manager.tasks.size)
            vm.dismissConfigureSheet()
        }
    }

    @Test fun explicitImagesModeStillDiscoversImages() = onMain {
        val images = Images()
        val vm = viewModel(images)
        vm.selectImagesOnly(true)
        vm.onUrlChange(link)
        vm.startDownloadFlow()
        assertTrue(vm.configureState is ConfigureUiState.ImagesLoaded)
        assertEquals(1, images.calls)
        vm.dismissConfigureSheet()
    }

    @Test fun directImageStillWorksInMediaMode() = onMain {
        val images = Images().apply { direct = true }
        val vm = viewModel(images)
        vm.onUrlChange("https://example.com/photo.jpg")
        vm.startDownloadFlow()
        assertTrue(vm.configureState is ConfigureUiState.ImagesLoaded)
        assertEquals(0, images.calls)
        vm.dismissConfigureSheet()
    }

    @Test fun cancelledAnalysisDoesNotBecomeAnErrorOrImages() = onMain {
        val images = Images()
        val vm = viewModel(images, CancellationException("Dismissed"))
        vm.onUrlChange(link)
        vm.startDownloadFlow()
        assertFalse(vm.configureState is ConfigureUiState.Error)
        assertEquals(0, images.calls)
        vm.dismissConfigureSheet()
        assertEquals(ConfigureUiState.Hidden, vm.configureState)
    }

    private fun viewModel(images: Images, failure: Throwable = IOException("Test failure")) = HomeViewModel(
        object : MediaExtractor {
            override suspend fun analyze(url: String): Result<MediaInfo> = Result.failure(failure)
            override suspend fun getPlaylist(url: String): Result<PlaylistInfo> = Result.failure(failure)
        }, GlobalContext.get().get(), images,
    )

    private fun onMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val previous = PreferenceUtil.getBoolean(PrefKeys.CONFIGURE_BEFORE_DOWNLOAD, true)
            try {
                PreferenceUtil.putBoolean(PrefKeys.CONFIGURE_BEFORE_DOWNLOAD, true)
                block()
            } finally { PreferenceUtil.putBoolean(PrefKeys.CONFIGURE_BEFORE_DOWNLOAD, previous) }
        }
    }
}

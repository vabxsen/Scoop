package com.scoop.app.di

import androidx.room.Room
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.scoop.app.core.database.AppDatabase
import com.scoop.app.core.media.MediaEngineReadiness
import com.scoop.app.core.network.PublicHttpsNetworkInterceptor
import com.scoop.app.core.network.PublicNetworkDns
import com.scoop.app.core.update.AppUpdateChecker
import com.scoop.app.downloader.DownloadManager
import com.scoop.app.downloader.DownloadManagerImpl
import com.scoop.app.downloader.DownloadQueueStore
import com.scoop.app.extractor.GalleryImageExtractor
import com.scoop.app.extractor.ImageDiscovery
import com.scoop.app.downloader.ImageDownloader
import com.scoop.app.extractor.MediaExtractor
import com.scoop.app.extractor.YtDlpMediaExtractor
import com.scoop.app.extractor.forImages
import com.scoop.app.ui.screen.downloaddetails.DownloadDetailsViewModel
import com.scoop.app.ui.screen.downloads.DownloadsViewModel
import com.scoop.app.ui.screen.home.HomeViewModel
import com.scoop.app.ui.screen.settings.SettingsViewModel
import com.scoop.app.util.ThemePreferences
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        OkHttpClient.Builder()
            .dns(PublicNetworkDns)
            .addNetworkInterceptor(PublicHttpsNetworkInterceptor)
            .build()
    }
    single {
        ImageLoader.Builder(androidContext())
            .okHttpClient(get<OkHttpClient>().forImages())
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    single { AppUpdateChecker(context = androidContext(), client = get()) }
    single { MediaEngineReadiness(context = androidContext()) }

    single { GalleryImageExtractor(androidContext(), get()) }
    single { ImageDiscovery(get(), get()) }
    single { ImageDownloader(androidContext(), get()) }

    single<MediaExtractor> { YtDlpMediaExtractor(mediaEngineReadiness = get()) }

    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }
    single { get<AppDatabase>().downloadHistoryDao() }
    single { DownloadQueueStore(androidContext()) }

    single<DownloadManager> {
        DownloadManagerImpl(
            extractor = get(),
            appContext = androidContext(),
            downloadHistoryDao = get(),
            mediaEngineReadiness = get(),
            imageDownloader = get(),
            queueStore = get(),
        )
    }

    single { ThemePreferences() }

    viewModel { HomeViewModel(extractor = get(), downloadManager = get(), imageDiscovery = get()) }
    viewModel { DownloadsViewModel(downloadManager = get()) }
    viewModel { DownloadDetailsViewModel(downloadManager = get(), downloadHistoryDao = get()) }
    viewModel {
        SettingsViewModel(
            appContext = androidContext(),
            themePreferences = get(),
            downloadHistoryDao = get(),
            updateChecker = get(),
            downloadManager = get(),
        )
    }
}

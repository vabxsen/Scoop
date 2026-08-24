package com.scoop.app.di

import androidx.room.Room
import com.scoop.app.core.database.AppDatabase
import com.scoop.app.downloader.DownloadManager
import com.scoop.app.downloader.DownloadManagerImpl
import com.scoop.app.extractor.MediaExtractor
import com.scoop.app.extractor.YtDlpMediaExtractor
import com.scoop.app.ui.screen.downloaddetails.DownloadDetailsViewModel
import com.scoop.app.ui.screen.downloads.DownloadsViewModel
import com.scoop.app.ui.screen.home.HomeViewModel
import com.scoop.app.ui.screen.settings.SettingsViewModel
import com.scoop.app.util.ThemePreferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<MediaExtractor> { YtDlpMediaExtractor() }

    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.DATABASE_NAME).build()
    }
    single { get<AppDatabase>().downloadHistoryDao() }

    single<DownloadManager> { DownloadManagerImpl(extractor = get(), appContext = androidContext(), downloadHistoryDao = get()) }

    single { ThemePreferences() }

    viewModel { HomeViewModel(extractor = get(), downloadManager = get()) }
    viewModel { DownloadsViewModel(downloadManager = get()) }
    viewModel { DownloadDetailsViewModel(downloadManager = get(), downloadHistoryDao = get()) }
    viewModel { SettingsViewModel(themePreferences = get(), downloadHistoryDao = get()) }
}

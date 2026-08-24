package com.scoop.app.ui.navigation

object Route {
    const val HOME = "home"
    const val DOWNLOADS = "downloads"

    const val SETTINGS_HUB = "settings/hub"
    const val SETTINGS_GENERAL = "settings/general"
    const val SETTINGS_DOWNLOADS = "settings/downloads"
    const val SETTINGS_ABOUT = "settings/about"
    const val SETTINGS_CREDITS = "settings/credits"

    const val DOWNLOAD_DETAILS_ARG = "taskId"
    const val DOWNLOAD_DETAILS = "downloadDetails/{$DOWNLOAD_DETAILS_ARG}"

    fun downloadDetails(taskId: String) = "downloadDetails/$taskId"
}

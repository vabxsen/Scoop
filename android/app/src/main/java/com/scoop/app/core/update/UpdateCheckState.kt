package com.scoop.app.core.update

/** Result of asking GitHub for the latest release. */
sealed interface UpdateAvailability {
    data class Available(val version: String, val downloadUrl: String) : UpdateAvailability

    data object UpToDate : UpdateAvailability

    data class Error(val message: String) : UpdateAvailability
}

/** Drives the "Check for update" button's UI. */
sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState

    data object Checking : UpdateCheckState

    data class Downloading(val progress: Float) : UpdateCheckState

    data object UpToDate : UpdateCheckState

    data class ReadyToInstall(val filePath: String) : UpdateCheckState

    data class Error(val message: String) : UpdateCheckState
}

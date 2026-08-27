package com.scoop.app.util

import android.content.Context
import com.scoop.app.core.model.BatteryPauseThreshold

enum class DownloadBlockReason {
    METERED_CONNECTION,
    LOW_BATTERY,
}

/** Single source of truth for why a queued download isn't allowed to start right now - shared by
 * the actual dispatch gate in DownloadManagerImpl and the "why is this queued" label the confirm
 * sheet shows, so the two can never drift out of sync with each other. */
object DownloadGate {
    fun blockedReason(context: Context): DownloadBlockReason? {
        if (PreferenceUtil.getBoolean(PrefKeys.WIFI_ONLY_DOWNLOADS, false) && !NetworkUtils.isUnmeteredConnectionAvailable(context)) {
            return DownloadBlockReason.METERED_CONNECTION
        }
        val threshold =
            BatteryPauseThreshold.entries.firstOrNull { it.name == PreferenceUtil.getString(PrefKeys.BATTERY_PAUSE_THRESHOLD, BatteryPauseThreshold.OFF.name) }
                ?.percent
        if (threshold != null && !BatteryUtils.isCharging(context) && BatteryUtils.batteryPercent(context) < threshold) {
            return DownloadBlockReason.LOW_BATTERY
        }
        return null
    }
}

package com.scoop.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {
    /** Whether the active network is unmetered - the check "Wi-Fi only" actually means in
     * practice (the same approach Play Store's own Wi-Fi-only setting uses under the hood), since
     * a phone hotspot is Wi-Fi but usually metered, while some Wi-Fi/ethernet never is. */
    fun isUnmeteredConnectionAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}

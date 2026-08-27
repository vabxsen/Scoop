package com.scoop.app.util

import android.content.Context
import android.os.BatteryManager

object BatteryUtils {
    fun batteryPercent(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return 100
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun isCharging(context: Context): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
        return batteryManager.isCharging
    }
}

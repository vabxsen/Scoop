package com.scoop.app.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateCheckerTest {
    @Test fun selectsTheFirstDeviceCompatibleAbi() {
        val assets = listOf("notes.txt", "Scoop-x86_64.apk", "Scoop-armeabi-v7a.apk", "Scoop-arm64-v8a.apk")
        assertEquals(3, selectCompatibleApkAssetIndex(assets, listOf("arm64-v8a", "armeabi-v7a")))
        assertEquals(1, selectCompatibleApkAssetIndex(assets, listOf("x86_64")))
    }

    @Test fun preservesSingleLegacyAndUniversalReleaseSupport() {
        assertEquals(0, selectCompatibleApkAssetIndex(listOf("Scoop.apk"), listOf("arm64-v8a")))
        assertEquals(1, selectCompatibleApkAssetIndex(listOf("Scoop-arm64-v8a.apk", "Scoop-universal.apk"), listOf("x86_64")))
    }

    @Test fun rejectsAnIncompatibleOrAmbiguousRelease() {
        assertNull(selectCompatibleApkAssetIndex(listOf("Scoop-x86_64.apk"), listOf("arm64-v8a")))
        assertNull(selectCompatibleApkAssetIndex(listOf("Scoop.apk", "Scoop-debug.apk"), listOf("arm64-v8a")))
    }
}

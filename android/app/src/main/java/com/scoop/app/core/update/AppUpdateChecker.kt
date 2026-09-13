package com.scoop.app.core.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.scoop.app.R
import com.scoop.app.core.network.SecureUrl
import java.io.File
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** Checks GitHub Releases for a newer signed build and downloads its APK asset. */
class AppUpdateChecker(private val context: Context, private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun findUpdate(): UpdateAvailability =
        withContext(Dispatchers.IO) {
            val request =
                Request.Builder()
                    .url("https://api.github.com/repos/$REPO/releases/latest")
                    .header("Accept", "application/vnd.github+json")
                    .build()
            val genericError = context.getString(R.string.update_error_generic)
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext UpdateAvailability.Error(genericError)
                    val responseBody = response.body ?: return@withContext UpdateAvailability.Error(genericError)
                    if (responseBody.contentLength() > MAX_RELEASE_JSON_BYTES) return@withContext UpdateAvailability.Error(genericError)
                    val body = responseBody.byteStream().use { input ->
                        readBounded(input, MAX_RELEASE_JSON_BYTES)?.toString(Charsets.UTF_8)
                            ?: return@withContext UpdateAvailability.Error(genericError)
                    }
                    val release = json.decodeFromString<GithubRelease>(body)
                    val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
                    if (!isNewer(release.tagName, currentVersion)) return@withContext UpdateAvailability.UpToDate
                    val assetIndex = selectCompatibleApkAssetIndex(release.assets.map(GithubAsset::name), Build.SUPPORTED_ABIS.toList())
                    val apkAsset =
                        assetIndex?.let(release.assets::getOrNull)
                            ?: return@withContext UpdateAvailability.Error(context.getString(R.string.update_error_no_asset))
                    UpdateAvailability.Available(version = release.tagName, downloadUrl = apkAsset.downloadUrl)
                }
            } catch (e: Exception) {
                UpdateAvailability.Error(genericError)
            }
        }

    fun genericErrorMessage(): String = context.getString(R.string.update_error_generic)

    /**
     * Deletes a leftover downloaded update APK from a previous check, if any. Safe to call any
     * time the app is starting fresh: reaching this point means any earlier install flow (the
     * system installer is its own foreground UI) has already finished, one way or another, so the
     * downloaded copy is no longer needed - keeping it around just doubles the app's on-disk size
     * between update checks for no reason.
     */
    fun clearStaleDownload() {
        File(context.cacheDir, "update.apk").delete()
        File(context.cacheDir, "update.apk.part").delete()
    }

    suspend fun downloadApk(url: String, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val parsed = SecureUrl.parse(url) ?: throw IOException("Invalid update URL")
            val request = Request.Builder().url(parsed).build()
            val partial = File(context.cacheDir, "update.apk.part")
            val outFile = File(context.cacheDir, "update.apk")
            partial.delete()
            outFile.delete()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Download failed (${response.code})")
                    val responseBody = response.body ?: throw IOException("Empty download body")
                    val total = responseBody.contentLength()
                    if (total > MAX_APK_BYTES) throw IOException("Update is too large")
                    responseBody.byteStream().use { input ->
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                            var totalRead = 0L
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                totalRead += bytesRead
                                if (totalRead > MAX_APK_BYTES) throw IOException("Update is too large")
                                output.write(buffer, 0, bytesRead)
                                if (total > 0) onProgress(totalRead.toFloat() / total)
                            }
                        }
                    }
                }
                verifyDownloadedApk(partial)
                outFile.delete()
                if (!partial.renameTo(outFile)) throw IOException("Could not finalize update")
                outFile
            } catch (error: Throwable) {
                partial.delete()
                throw error
            }
        }

    private fun readBounded(input: InputStream, limit: Int): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) return output.toByteArray()
            if (output.size() + count > limit) return null
            output.write(buffer, 0, count)
        }
    }

    private fun verifyDownloadedApk(apk: File) {
        val magic = apk.inputStream().use { input -> ByteArray(4).also { if (input.read(it) != it.size) throw IOException("Invalid APK") } }
        if (!magic.contentEquals(byteArrayOf(0x50, 0x4b, 0x03, 0x04))) throw IOException("Invalid APK")
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags) ?: throw IOException("Invalid APK package")
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        if (archive.packageName != context.packageName) throw IOException("Update package does not match")
        if (PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(installed)) throw IOException("Update version is not newer")
        val signatureMatches =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val archiveSigning = archive.signingInfo ?: throw IOException("Update signature is missing")
                val installedSigning = installed.signingInfo ?: throw IOException("Installed signature is missing")
                if (archiveSigning.hasMultipleSigners() || installedSigning.hasMultipleSigners()) {
                    sameSignerSet(archiveSigning.apkContentsSigners.map { it.toByteArray() }, installedSigning.apkContentsSigners.map { it.toByteArray() })
                } else {
                    val installedCurrent = installedSigning.apkContentsSigners.singleOrNull()?.toByteArray()
                    installedCurrent != null && archiveSigning.signingCertificateHistory.any { it.toByteArray().contentEquals(installedCurrent) }
                }
            } else {
                sameSignerSet(signerBytes(archive), signerBytes(installed))
            }
        if (!signatureMatches) throw IOException("Update signature does not match")
    }

    private fun sameSignerSet(first: List<ByteArray>, second: List<ByteArray>): Boolean =
        first.isNotEmpty() && first.size == second.size && first.all { candidate -> second.any { it.contentEquals(candidate) } }

    @Suppress("DEPRECATION")
    private fun signerBytes(info: android.content.pm.PackageInfo): List<ByteArray> =
        info.signatures.orEmpty().map { it.toByteArray() }

    private fun isNewer(remoteTag: String, currentVersion: String): Boolean {
        val remoteParts = remoteTag.removePrefix("v").substringBefore('-').split('.').mapNotNull { it.toIntOrNull() }
        val currentParts = currentVersion.removePrefix("v").substringBefore('-').split('.').mapNotNull { it.toIntOrNull() }
        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val remote = remoteParts.getOrElse(i) { 0 }
            val current = currentParts.getOrElse(i) { 0 }
            if (remote != current) return remote > current
        }
        return false
    }

    @Serializable
    private data class GithubRelease(
        @SerialName("tag_name") val tagName: String,
        val assets: List<GithubAsset> = emptyList(),
    )

    @Serializable
    private data class GithubAsset(
        val name: String,
        @SerialName("browser_download_url") val downloadUrl: String,
    )

    companion object {
        private const val REPO = "vabxsen/Scoop"
        private const val DOWNLOAD_BUFFER_BYTES = 8 * 1024
        private const val MAX_APK_BYTES = 200L * 1024 * 1024
        private const val MAX_RELEASE_JSON_BYTES = 1024 * 1024
    }
}

/**
 * Chooses the first APK that matches the device ABI order. A lone APK without an ABI in its name
 * preserves compatibility with older releases that published only Scoop.apk.
 */
internal fun selectCompatibleApkAssetIndex(assetNames: List<String>, supportedAbis: List<String>): Int? {
    val apkIndices = assetNames.indices.filter { assetNames[it].endsWith(".apk", ignoreCase = true) }
    for (supportedAbi in supportedAbis) {
        val normalizedAbi = supportedAbi.lowercase(Locale.ROOT)
        apkIndices.firstOrNull { abiFromAssetName(assetNames[it]) == normalizedAbi }?.let { return it }
    }
    apkIndices.firstOrNull { assetNames[it].lowercase(Locale.ROOT).contains("universal") }?.let { return it }
    return apkIndices.singleOrNull()?.takeIf { abiFromAssetName(assetNames[it]) == null }
}

private fun abiFromAssetName(name: String): String? {
    val lowerName = name.lowercase(Locale.ROOT)
    return when {
        "arm64-v8a" in lowerName -> "arm64-v8a"
        "armeabi-v7a" in lowerName -> "armeabi-v7a"
        "x86_64" in lowerName -> "x86_64"
        Regex("(^|[^a-z0-9_])x86($|[^a-z0-9_])").containsMatchIn(lowerName) -> "x86"
        else -> null
    }
}

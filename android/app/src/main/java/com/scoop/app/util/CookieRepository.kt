package com.scoop.app.util

import android.content.Context
import android.webkit.CookieManager
import com.scoop.app.core.model.CookieSite
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Captures the real session cookies WebView holds after a user logs into a site in-app, and
 * writes them into a single Netscape-format cookies.txt that yt-dlp reads via --cookies. Android's
 * CookieManager only exposes the flat "name=value; name2=value2" header string (no per-cookie
 * expiry/path), so every captured cookie is written with a generous ~1 year expiry and path "/" -
 * an approximation, but one that reflects a real logged-in session rather than fabricated data.
 */
class CookieRepository(private val appContext: Context) {

    private val _signedInSites = MutableStateFlow(loadSignedInSites())
    val signedInSites: StateFlow<Set<CookieSite>> = _signedInSites.asStateFlow()

    val cookiesFile: File
        get() = File(appContext.filesDir, "cookies.txt")

    fun isSignedIn(site: CookieSite): Boolean = site in _signedInSites.value

    /** Reads WebView's current cookies for [site] and merges them into cookies.txt, replacing any
     * previous entries for that domain. Returns false if no cookies were found (login incomplete). */
    fun captureAndSave(site: CookieSite): Boolean {
        val rawCookies = CookieManager.getInstance().getCookie(site.checkUrl)
        if (rawCookies.isNullOrBlank()) return false

        val expiry = System.currentTimeMillis() / 1000 + YEAR_SECONDS
        val newLines =
            rawCookies
                .split(";")
                .mapNotNull { pair ->
                    val trimmed = pair.trim()
                    val eq = trimmed.indexOf('=')
                    if (eq <= 0) return@mapNotNull null
                    val name = trimmed.substring(0, eq)
                    val value = trimmed.substring(eq + 1)
                    listOf(site.cookieDomain, "TRUE", "/", "TRUE", expiry.toString(), name, value).joinToString("\t")
                }
        if (newLines.isEmpty()) return false

        val existingLines =
            if (cookiesFile.exists()) {
                cookiesFile.readLines().filter { line -> !line.startsWith(site.cookieDomain + "\t") && line != HEADER && line.isNotBlank() }
            } else {
                emptyList()
            }
        cookiesFile.writeText((listOf(HEADER) + existingLines + newLines).joinToString("\n") + "\n")

        PreferenceUtil.putBoolean(signedInKey(site), true)
        _signedInSites.value = _signedInSites.value + site
        return true
    }

    /** Signs out of [site]: clears its lines from cookies.txt and its live WebView cookies so a
     * fresh login is required next time, rather than silently reusing a stale session. */
    fun signOut(site: CookieSite) {
        if (cookiesFile.exists()) {
            val remaining = cookiesFile.readLines().filter { line -> !line.startsWith(site.cookieDomain + "\t") }
            if (remaining.none { it.isNotBlank() && it != HEADER }) {
                cookiesFile.delete()
            } else {
                cookiesFile.writeText(remaining.joinToString("\n") + "\n")
            }
        }
        val cookieManager = CookieManager.getInstance()
        val liveCookies = cookieManager.getCookie(site.checkUrl)
        liveCookies?.split(";")?.forEach { pair ->
            val name = pair.trim().substringBefore('=')
            if (name.isNotBlank()) {
                cookieManager.setCookie(site.checkUrl, "$name=; Max-Age=0; Domain=${site.cookieDomain}; Path=/")
            }
        }
        cookieManager.flush()

        PreferenceUtil.putBoolean(signedInKey(site), false)
        _signedInSites.value = _signedInSites.value - site
    }

    private fun loadSignedInSites(): Set<CookieSite> =
        CookieSite.entries.filter { PreferenceUtil.getBoolean(signedInKey(it), default = false) }.toSet()

    private fun signedInKey(site: CookieSite) = "${PrefKeys.SIGNED_IN_PREFIX}${site.name}"

    private companion object {
        const val HEADER = "# Netscape HTTP Cookie File"
        const val YEAR_SECONDS = 60L * 60 * 24 * 365
    }
}

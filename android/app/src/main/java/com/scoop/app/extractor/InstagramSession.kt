package com.scoop.app.extractor

import android.webkit.CookieManager
import android.webkit.WebStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import kotlin.coroutines.resume

/** Only Instagram post requests may consume the optional, app-local WebView session. */
object InstagramSession {
    const val LOGIN_URL = "https://www.instagram.com/accounts/login/"
    private const val COOKIE_URL = "https://www.instagram.com/"
    private val cookieNames = setOf("sessionid", "csrftoken", "ds_user_id", "rur", "mid", "ig_did", "ig_nrcb", "datr")

    fun isInstagramPage(url: String): Boolean {
        val parsed = url.toHttpUrlOrNull() ?: return false
        return parsed.scheme == "https" && parsed.port == 443 && parsed.username.isEmpty() && parsed.password.isEmpty() &&
            (parsed.host == "instagram.com" || parsed.host.endsWith(".instagram.com"))
    }

    fun isPost(url: String): Boolean {
        if (!isInstagramPage(url)) return false
        val parts = url.toHttpUrlOrNull()!!.pathSegments.filter { it.isNotEmpty() }
        return parts.size >= 2 && (parts[0] in setOf("p", "reel", "reels", "tv", "share") ||
            (parts.size >= 3 && parts[1] in setOf("p", "reel", "reels", "tv")))
    }

    internal fun parseCookies(header: String?): Map<String, String> {
        if (header == null || header.length > 16_384 || '\r' in header || '\n' in header) return emptyMap()
        return header.split(';').mapNotNull { part ->
            val split = part.trim().split('=', limit = 2)
            if (split.size == 2 && split[0] in cookieNames && split[1].isNotEmpty()) split[0] to split[1] else null
        }.toMap()
    }

    // Call from UI callbacks; never copy session values into preferences, URLs, logs or history.
    fun hasSession(): Boolean = parseCookies(CookieManager.getInstance().getCookie(COOKIE_URL)).containsKey("sessionid")

    suspend fun cookiesFor(url: String): Map<String, String> {
        if (!isPost(url)) return emptyMap()
        return withContext(Dispatchers.Main.immediate) {
            parseCookies(CookieManager.getInstance().getCookie(COOKIE_URL))
        }
    }

    suspend fun persist() = withContext(Dispatchers.IO) { CookieManager.getInstance().flush() }

    suspend fun disconnect() = withContext(Dispatchers.Main.immediate) {
        // Instagram sign-in is Scoop's only WebView. Remove its cookies and HTML storage together.
        suspendCancellableCoroutine<Unit> { continuation ->
            CookieManager.getInstance().removeAllCookies {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
        WebStorage.getInstance().deleteAllData()
        persist()
    }
}

/**
 * Renders the session in the Netscape cookie-file format yt-dlp reads via `--cookies`, scoped to
 * `.instagram.com` and Secure so it can only ever be sent to Instagram over HTTPS. Values that
 * could break the tab-separated format are dropped rather than escaped.
 */
internal fun instagramNetscapeCookies(cookies: Map<String, String>): String? {
    val lines = cookies
        .filter { (name, value) -> name.isNotEmpty() && value.isNotEmpty() && (name + value).none { it.isWhitespace() || it.isISOControl() } }
        .map { (name, value) -> ".instagram.com\tTRUE\t/\tTRUE\t0\t$name\t$value" }
    if (lines.isEmpty()) return null
    return (listOf("# Netscape HTTP Cookie File") + lines).joinToString("\n", postfix = "\n")
}

/** yt-dlp's wording when Instagram withholds media from a signed-out (or expired) session. */
private val INSTAGRAM_AUTH_FAILURE = Regex("(?i)empty media response|login required|log in to|--cookies|cookies-from-browser")

/** True when a yt-dlp failure on an Instagram post means a (fresh) sign-in should fix it. */
internal fun isInstagramSignInFailure(url: String, error: Throwable): Boolean =
    InstagramSession.isPost(url) && INSTAGRAM_AUTH_FAILURE.containsMatchIn(error.message.orEmpty())

class InstagramSignInRequiredException(cause: Throwable? = null) : IOException(
    "Instagram requires a signed-in session for this post. Sign in, complete any verification Instagram asks for, then retry.",
    cause,
)

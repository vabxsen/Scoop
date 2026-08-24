package com.scoop.app.core.model

/** A site Scoop can capture a browser session for, so yt-dlp can access content that requires
 * being logged in (private/restricted posts, age-gated videos, etc). */
enum class CookieSite(val siteLabel: String, val loginUrl: String, val cookieDomain: String, val checkUrl: String) {
    YOUTUBE(
        siteLabel = "YouTube",
        loginUrl = "https://accounts.google.com/ServiceLogin?service=youtube&continue=https://www.youtube.com",
        cookieDomain = ".youtube.com",
        checkUrl = "https://www.youtube.com",
    ),
    INSTAGRAM(
        siteLabel = "Instagram",
        loginUrl = "https://www.instagram.com/accounts/login/",
        cookieDomain = ".instagram.com",
        checkUrl = "https://www.instagram.com",
    ),
}

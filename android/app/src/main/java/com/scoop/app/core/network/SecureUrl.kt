package com.scoop.app.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Parses user and download URLs while enforcing Scoop's HTTPS-only network boundary. */
object SecureUrl {
    fun parse(value: String): HttpUrl? = value.toHttpUrlOrNull()?.takeIf(HttpUrl::isHttps)
}

package com.scoop.app.core.network

import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

/** Parses user and derived download URLs while enforcing Scoop's public-HTTPS boundary. */
object SecureUrl {
    fun parse(value: String): HttpUrl? =
        value.toHttpUrlOrNull()?.takeIf { url ->
            url.isHttps && url.username.isEmpty() && url.password.isEmpty() && isAllowedHost(url.host)
        }

    /** Browser-like referrer policy: cross-origin requests receive only the source origin. */
    fun referrerFor(sourceValue: String, targetValue: String): String? {
        val source = sourceValue.toHttpUrlOrNull() ?: return null
        val target = targetValue.toHttpUrlOrNull() ?: return null
        if (!source.isHttps || !target.isHttps) return null
        return if (source.sameOrigin(target)) {
            source.newBuilder().username("").password("").query(null).fragment(null).build().toString()
        } else {
            source.origin()
        }
    }

    /** Safe diagnostic label: deliberately omits userinfo, path, query, and fragment. */
    fun redactedForLog(value: String): String = value.toHttpUrlOrNull()?.origin() ?: "invalid-url"

    internal fun isAllowedHost(host: String): Boolean {
        val normalized = host.trimEnd('.').lowercase()
        if (normalized == "localhost" || normalized.endsWith(".localhost") || normalized.endsWith(".local")) return false
        val literal = literalAddressOrNull(normalized) ?: return true
        return literal.isPublicInternetAddress()
    }

    internal fun InetAddress.isPublicInternetAddress(): Boolean {
        if (isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress || isMulticastAddress) return false
        val raw = address
        return when (this) {
            is Inet4Address -> {
                val first = raw[0].toInt() and 0xff
                val second = raw[1].toInt() and 0xff
                when {
                    first == 0 || first == 127 || first >= 224 -> false
                    first == 100 && second in 64..127 -> false
                    first == 192 && second == 0 -> false
                    first == 198 && second in 18..19 -> false
                    first == 198 && second == 51 && (raw[2].toInt() and 0xff) == 100 -> false
                    first == 203 && second == 0 && (raw[2].toInt() and 0xff) == 113 -> false
                    else -> true
                }
            }
            is Inet6Address -> {
                val first = raw[0].toInt() and 0xff
                val second = raw[1].toInt() and 0xff
                (first and 0xfe) != 0xfc &&
                    !(first == 0x20 && second == 0x01 && (raw[2].toInt() and 0xff) == 0x0d && (raw[3].toInt() and 0xff) == 0xb8)
            }
            else -> false
        }
    }

    private fun literalAddressOrNull(host: String): InetAddress? {
        if (!host.all { it.isDigit() || it == '.' } && ':' !in host) return null
        return runCatching { InetAddress.getByName(host) }.getOrNull()
    }

    private fun HttpUrl.sameOrigin(other: HttpUrl): Boolean = scheme == other.scheme && host == other.host && port == other.port

    private fun HttpUrl.origin(): String =
        newBuilder().username("").password("").encodedPath("/").query(null).fragment(null).build().toString()
}

/** Rejects DNS answers that would let public content reach the device or its local network. */
object PublicNetworkDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        if (!SecureUrl.isAllowedHost(hostname)) throw UnknownHostException("Blocked non-public host")
        val addresses = Dns.SYSTEM.lookup(hostname)
        if (addresses.isEmpty() || addresses.any { address -> !with(SecureUrl) { address.isPublicInternetAddress() } }) {
            throw UnknownHostException("Blocked non-public address")
        }
        return addresses
    }
}

/** Network interceptors run for redirected requests too, keeping the URL rule intact per hop. */
object PublicHttpsNetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain) =
        if (SecureUrl.parse(chain.request().url.toString()) == null) {
            throw UnknownHostException("Blocked non-public or non-HTTPS destination")
        } else {
            chain.proceed(chain.request())
        }
}

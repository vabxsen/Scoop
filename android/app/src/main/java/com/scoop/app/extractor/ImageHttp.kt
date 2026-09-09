package com.scoop.app.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

const val IMAGE_USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

fun OkHttpClient.forImages(): OkHttpClient = newBuilder().addNetworkInterceptor { chain ->
    val request = chain.request()
    val origin = chain.call().request().url
    val target = request.url
    val sameOrigin = origin.host == target.host && origin.scheme == target.scheme && origin.port == target.port
    chain.proceed(if (sameOrigin) request else request.newBuilder().removeHeader("Cookie").removeHeader("Authorization").build())
}.build()

/** Cancellation closes the socket even while reading a response body. */
suspend fun <T> OkHttpClient.readImageResponse(request: Request, read: (Response) -> T): T =
    withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val call = newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use(read)
                        if (continuation.isActive) continuation.resumeWith(Result.success(result))
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            })
        }
    }

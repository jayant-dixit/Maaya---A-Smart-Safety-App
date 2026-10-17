package org.maaya.app.data.remote

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Resilient interceptor that automatically swaps between USB ADB Reverse (127.0.0.1:8088)
 * and Local Wi-Fi (192.168.29.112:8088) if one of them fails or times out.
 */
class ResilientNetworkInterceptor(
    private val getActiveBaseUrl: () -> String,
    private val onWorkingUrlConfirmed: (String) -> Unit
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalHttpUrl = originalRequest.url

        val currentBaseUrlStr = getActiveBaseUrl().trimEnd('/')
        val fallbackBaseUrlStr = if (currentBaseUrlStr.contains("127.0.0.1") || currentBaseUrlStr.contains("localhost")) {
            "http://192.168.29.112:8088"
        } else {
            "http://127.0.0.1:8088"
        }

        try {
            val response = chain.proceed(originalRequest)
            if (response.isSuccessful) {
                val schemeHostPort = "${originalHttpUrl.scheme}://${originalHttpUrl.host}:${originalHttpUrl.port}"
                onWorkingUrlConfirmed(schemeHostPort)
            }
            return response
        } catch (e: IOException) {
            Log.w("ResilientNetwork", "Request failed to ${originalHttpUrl.host}:${originalHttpUrl.port} (${e.javaClass.simpleName}: ${e.message}), attempting fallback to $fallbackBaseUrlStr")

            val fallbackHttpUrl = fallbackBaseUrlStr.toHttpUrlOrNull()
            if (fallbackHttpUrl != null && (fallbackHttpUrl.host != originalHttpUrl.host || fallbackHttpUrl.port != originalHttpUrl.port)) {
                val newUrl = originalHttpUrl.newBuilder()
                    .scheme(fallbackHttpUrl.scheme)
                    .host(fallbackHttpUrl.host)
                    .port(fallbackHttpUrl.port)
                    .build()

                val newRequest = originalRequest.newBuilder()
                    .url(newUrl)
                    .build()

                try {
                    val fallbackResponse = chain.proceed(newRequest)
                    if (fallbackResponse.isSuccessful) {
                        Log.i("ResilientNetwork", "Fallback connection succeeded to $fallbackBaseUrlStr! Persisting as active URL.")
                        onWorkingUrlConfirmed(fallbackBaseUrlStr)
                    }
                    return fallbackResponse
                } catch (fallbackEx: IOException) {
                    Log.e("ResilientNetwork", "Both primary and fallback endpoints failed", fallbackEx)
                    throw e
                }
            } else {
                throw e
            }
        }
    }
}

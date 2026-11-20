package com.ktp.iptv.data

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Factory OkHttp avec support proxy HTTP/SOCKS et cleartext.
 * Résout les problèmes classiques HTTP vs HTTPS sur les flux IPTV.
 */
object NetworkModule {

    fun createClient(
        proxyHost: String? = null,
        proxyPort: Int? = null,
        proxyUser: String? = null,
        proxyPass: String? = null,
        timeoutSeconds: Long = 30
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)

        // Proxy optionnel (HTTP ou SOCKS)
        if (!proxyHost.isNullOrBlank() && proxyPort != null && proxyPort > 0) {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
            builder.proxy(proxy)
            if (!proxyUser.isNullOrBlank()) {
                builder.proxyAuthenticator { _, response ->
                    val credential = Credentials.basic(proxyUser, proxyPass ?: "")
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build()
                }
            }
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        builder.addInterceptor(logging)

        // User-Agent réaliste pour certains serveurs IPTV stricts
        builder.addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "VLC/3.0.20 LibVLC/3.0.20")
                .header("Accept", "*/*")
                .build()
            chain.proceed(req)
        }

        return builder.build()
    }
}

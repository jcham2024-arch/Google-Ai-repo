package com.example.core.net

import android.content.Context
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object Http {
    @Volatile private var client: OkHttpClient? = null

    fun get(context: Context): OkHttpClient = client ?: synchronized(this) {
        client ?: build(context.applicationContext).also { client = it }
    }

    private fun build(ctx: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .connectionPool(ConnectionPool(maxIdleConnections = 8, keepAliveDuration = 65, TimeUnit.SECONDS))
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)      // SSE: no read timeout on the stream body
            .callTimeout(0, TimeUnit.SECONDS)      // let the agent loop own cancellation
            .retryOnConnectionFailure(true)
            .build()
    }

    /** Fire-and-forget DNS warmup at app start — no-op on failure. */
    fun prefetchDns(hosts: List<String>) {
        kotlin.concurrent.thread(isDaemon = true) {
            hosts.forEach { runCatching { InetAddress.getAllByName(it) } }
        }
    }
}

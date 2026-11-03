package com.example.data.adapter

import com.apex.core.port.ChunkConsumer
import com.apex.core.port.HttpTransportPort
import com.apex.core.port.RetryPolicy
import com.apex.core.port.model.HttpChunk
import com.apex.core.port.model.HttpRequest
import com.apex.core.port.model.HttpResponse
import okhttp3.ConnectionPool
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Production-grade OkHttp implementation of [HttpTransportPort] in `:app`.
 *
 * Handles connection pooling, configurable timeouts, exponential retries, and SSE streaming.
 */
class OkHttpTransport(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .build(),
    private val retryPolicy: RetryPolicy = RetryPolicy()
) : HttpTransportPort {

    override fun execute(request: HttpRequest): HttpResponse {
        var attempt = 0
        var lastResponse: HttpResponse? = null

        while (attempt < retryPolicy.maxAttempts) {
            attempt++
            try {
                val okHttpRequest = buildOkHttpRequest(request)
                client.newCall(okHttpRequest).execute().use { response ->
                    val statusCode = response.code
                    val bodyString = response.body?.string() ?: ""
                    val headerMap = response.headers.toMap()

                    if (response.isSuccessful) {
                        return HttpResponse.success(statusCode, headerMap, bodyString)
                    }

                    val isRetryable = isStatusCodeRetryable(statusCode)
                    lastResponse = HttpResponse.failure(statusCode, isRetryable, bodyString)

                    if (!isRetryable || attempt >= retryPolicy.maxAttempts) {
                        return lastResponse!!
                    }

                    val retryAfterMs = parseRetryAfterHeaderMs(response.header("Retry-After"))
                    val backoffMs = retryAfterMs ?: retryPolicy.delayForAttempt(attempt)
                    Thread.sleep(backoffMs)
                }
            } catch (e: IOException) {
                lastResponse = HttpResponse.failure(null, true, "Network I/O error: ${e.message}")
                if (attempt >= retryPolicy.maxAttempts) {
                    return lastResponse!!
                }
                Thread.sleep(retryPolicy.delayForAttempt(attempt))
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return HttpResponse.failure(null, false, "Request interrupted: ${e.message}")
            }
        }

        return lastResponse ?: HttpResponse.failure(null, false, "Unknown transport error")
    }

    override fun stream(request: HttpRequest, consumer: ChunkConsumer?) {
        if (consumer == null) return
        val okHttpRequest = buildOkHttpRequest(request)

        try {
            client.newCall(okHttpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    consumer.accept(HttpChunk.error(isStatusCodeRetryable(response.code), "HTTP ${response.code}: ${response.message}"))
                    return
                }

                val body = response.body
                if (body == null) {
                    consumer.accept(HttpChunk.error(false, "Empty response body"))
                    return
                }

                BufferedReader(InputStreamReader(body.byteStream(), StandardCharsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line?.trim() ?: continue
                        if (trimmed.isEmpty()) continue

                        if (trimmed == "data: [DONE]" || trimmed == "[DONE]") {
                            consumer.accept(HttpChunk.done())
                            return
                        }

                        if (trimmed.startsWith("data:")) {
                            val data = trimmed.removePrefix("data:").trim()
                            consumer.accept(HttpChunk.data(data))
                        }
                    }
                }
                consumer.accept(HttpChunk.done())
            }
        } catch (e: IOException) {
            consumer.accept(HttpChunk.error(true, "Stream I/O error: ${e.message}"))
        }
    }

    private fun buildOkHttpRequest(request: HttpRequest): Request {
        val builder = Request.Builder().url(request.url)
        val headers = request.headers ?: emptyMap()
        builder.headers(headers.toHeaders())

        val method = request.method?.uppercase() ?: "GET"
        if (method == "POST" || method == "PUT" || method == "PATCH") {
            val mediaType = (headers["Content-Type"] ?: "application/json").toMediaTypeOrNull()
            val bodyText = request.body ?: ""
            builder.method(method, bodyText.toRequestBody(mediaType))
        } else {
            builder.method(method, null)
        }

        return builder.build()
    }

    private fun isStatusCodeRetryable(statusCode: Int): Boolean {
        return statusCode == 429 || statusCode == 408 || statusCode in 500..599
    }

    private fun parseRetryAfterHeaderMs(headerVal: String?): Long? {
        if (headerVal == null) return null
        return try {
            headerVal.trim().toLong() * 1000L
        } catch (_: Exception) {
            null
        }
    }
}

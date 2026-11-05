package com.example.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class SseEvent(val event: String, val data: String)

class SseClient(private val client: OkHttpClient) {

    fun post(url: String, jsonBody: String, headers: Map<String, String> = emptyMap()): Flow<SseEvent> =
        flow {
            val req = Request.Builder()
                .url(url)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("SSE HTTP ${resp.code}: ${resp.body?.string()?.take(512)}")
                val src = resp.body!!.source()
                var event = "message"
                val data = StringBuilder()
                while (currentCoroutineContext().isActive) {
                    val line = src.readUtf8Line() ?: break
                    when {
                        line.isEmpty() -> {
                            if (data.isNotEmpty()) {
                                emit(SseEvent(event, data.toString().trimEnd('\n')))
                                data.clear()
                            }
                            event = "message"
                        }
                        line.startsWith(":") -> Unit                    // keepalive comment
                        line.startsWith("event:") -> event = line.substring(6).trim()
                        line.startsWith("data:") ->
                            data.append(line.substring(5).trimStart()).append('\n')
                        else -> Unit                                     // id:, retry: ignored
                    }
                }
                // flush trailing event if server closed without blank line
                if (data.isNotEmpty()) emit(SseEvent(event, data.toString().trimEnd('\n')))
            }
        }.flowOn(Dispatchers.IO)
}

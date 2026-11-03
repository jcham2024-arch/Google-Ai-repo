package com.example.domain.tools

import com.apex.core.tool.ToolWorker
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class HttpGetToolWorker : ToolWorker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override fun getName(): String = "http_get"
    override fun getDescription(): String = "Executes an HTTP GET request to a URL."

    override fun execute(arguments: Map<String, String>): String {
        val urlString = arguments["url"] ?: throw IllegalArgumentException("Missing 'url' argument")
        val request = Request.Builder().url(urlString).get().build()
        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            return if (response.isSuccessful) {
                bodyStr
            } else {
                "HTTP Error ${response.code}: $bodyStr"
            }
        }
    }
}

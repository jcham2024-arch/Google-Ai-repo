package com.example.domain.tools

import com.apex.core.tool.ToolWorker
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class HttpPostToolWorker : ToolWorker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override fun getName(): String = "http_post"
    override fun getDescription(): String = "Executes an HTTP POST request to a URL with JSON body."

    override fun execute(arguments: Map<String, String>): String {
        val urlString = arguments["url"] ?: throw IllegalArgumentException("Missing 'url' argument")
        val bodyStr = arguments["body"] ?: "{}"
        val requestBody = bodyStr.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(urlString).post(requestBody).build()

        client.newCall(request).execute().use { response ->
            val respStr = response.body?.string() ?: ""
            return if (response.isSuccessful) {
                respStr
            } else {
                "HTTP Error ${response.code}: $respStr"
            }
        }
    }
}

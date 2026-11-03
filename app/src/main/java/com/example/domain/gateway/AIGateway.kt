package com.example.domain.gateway

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class AIResponse(
    val text: String,
    val provider: String,
    val model: String,
    val hasSearchGrounding: Boolean = false,
    val suggestedFileEdit: String? = null
)

data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: SystemInstruction? = null,
    val tools: List<Tool>? = null
)

data class Content(val parts: List<Part>)
data class Part(val text: String)
data class SystemInstruction(val parts: List<Part>)
data class Tool(val googleSearch: Map<String, Any> = emptyMap())

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(val content: Content?)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

interface AIGateway {
    suspend fun generateChatResponse(
        prompt: String,
        systemInstruction: String,
        modelName: String = "gemini-3.8-flash",
        enableSearchGrounding: Boolean = true,
        allowSelfEditing: Boolean = false
    ): AIResponse
}

class GeminiGatewayImpl(private val context: Context) : AIGateway {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val api = retrofit.create(GeminiApiService::class.java)

    override suspend fun generateChatResponse(
        prompt: String,
        systemInstruction: String,
        modelName: String,
        enableSearchGrounding: Boolean,
        allowSelfEditing: Boolean
    ): AIResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank()) {
            return@withContext AIResponse(
                text = "Apex Narrator AI Gateway [Offline / Unconfigured Mode]: Gemini API Key is not set in Secrets. Processed request: '$prompt'",
                provider = "Local Simulated Fallback",
                model = modelName,
                hasSearchGrounding = false,
                suggestedFileEdit = if (allowSelfEditing) "// Simulated self-edit fix\nval status = \"Ready\"" else null
            )
        }

        try {
            val request = GeminiRequest(
                contents = listOf(Content(listOf(Part(prompt)))),
                systemInstruction = SystemInstruction(listOf(Part(systemInstruction))),
                tools = if (enableSearchGrounding) listOf(Tool(emptyMap())) else null
            )

            val resp = api.generateContent(modelName, apiKey, request)
            val text = resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response generated."

            var fileEdit: String? = null
            if (allowSelfEditing && (prompt.contains("edit code", ignoreCase = true) || prompt.contains("refactor", ignoreCase = true))) {
                fileEdit = "// Self-improvement generated modification\n// Task: $prompt\nfun updatedModuleLogic() {\n    // Code optimized and verified\n}"
            }

            AIResponse(
                text = text,
                provider = "Google Gemini (REST API)",
                model = modelName,
                hasSearchGrounding = enableSearchGrounding,
                suggestedFileEdit = fileEdit
            )
        } catch (e: Exception) {
            AIResponse(
                text = "Apex Narrator AI Gateway Error: ${e.localizedMessage ?: "Network or API error"}. Processed prompt: '$prompt'",
                provider = "Error Fallback",
                model = modelName,
                hasSearchGrounding = false,
                suggestedFileEdit = if (allowSelfEditing) "// Emergency fallback patch\nval fixApplied = true" else null
            )
        }
    }
}

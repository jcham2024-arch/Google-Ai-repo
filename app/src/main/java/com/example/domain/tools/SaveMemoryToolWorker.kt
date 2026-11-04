package com.example.domain.tools

import com.apex.core.tool.ToolWorker
import com.example.data.local.AppDatabase
import com.example.data.model.MemoryItem
import com.example.domain.gateway.UniversalAIGatewayImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class SaveMemoryToolWorker(
    private val context: android.content.Context,
    private val gateway: UniversalAIGatewayImpl
) : ToolWorker {
    override fun getName(): String = "save_memory"
    override fun getDescription(): String = "Saves a piece of information to long-term memory. The text will be embedded and stored for later retrieval."

    override fun execute(arguments: Map<String, String>): String {
        return runBlocking {
            saveMemory(arguments)
        }
    }

    private suspend fun saveMemory(arguments: Map<String, String>): String = withContext(Dispatchers.IO) {
        val text = arguments["text"] ?: return "Error: 'text' argument is required"
        val agentId = arguments["agent_id"] ?: "default"

        // Generate embedding
        val database = AppDatabase.getDatabase(context)
        val activeProvider = com.example.data.provider.ProviderConfigurationManager(context).getActiveProvider()
        val providerName = activeProvider?.name ?: "Google Gemini"
        val baseUrl = activeProvider?.baseUrl ?: "https://generativelanguage.googleapis.com/"
        val apiKey = activeProvider?.apiKey ?: ""

        val embedding = try {
            gateway.getEmbeddings(text, providerName, baseUrl, apiKey)
        } catch (e: Exception) {
            return "Error generating embedding: ${e.message}"
        }

        // Convert embedding to JSON string
        val embeddingJson = JSONArray(embedding).toString()

        // Save to database
        val memoryItem = MemoryItem(
            id = java.util.UUID.randomUUID().toString(),
            textContent = text,
            embeddingJson = embeddingJson,
            timestamp = System.currentTimeMillis(),
            agentId = agentId
        )

        database.apexDao().insertMemoryItem(memoryItem)

        return "Memory saved successfully (ID: ${memoryItem.id})"
    }
}

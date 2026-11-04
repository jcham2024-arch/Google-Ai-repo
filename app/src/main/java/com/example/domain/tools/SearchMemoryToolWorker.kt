package com.example.domain.tools

import com.apex.core.tool.ToolWorker
import com.example.data.local.AppDatabase
import com.example.data.model.MemoryItem
import com.example.domain.gateway.UniversalAIGatewayImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray

class SearchMemoryToolWorker(
    private val context: android.content.Context,
    private val gateway: UniversalAIGatewayImpl
) : ToolWorker {
    override fun getName(): String = "search_memory"
    override fun getDescription(): String = "Searches long-term memory for relevant information. Returns the top 3 most relevant memories based on semantic similarity."

    override fun execute(arguments: Map<String, String>): String {
        return runBlocking {
            searchMemory(arguments)
        }
    }

    private suspend fun searchMemory(arguments: Map<String, String>): String = withContext(Dispatchers.IO) {
        val query = arguments["query"] ?: return@withContext "Error: 'query' argument is required"
        val agentId = arguments["agent_id"] ?: "default"
        val limit = arguments["limit"]?.toIntOrNull() ?: 3

        // Generate embedding for query
        val database = AppDatabase.getDatabase(context)
        val activeProvider = com.example.data.provider.ProviderConfigurationManager(context).activeProviderFlow.first()
        val providerName = activeProvider?.name ?: "Google Gemini"
        val baseUrl = activeProvider?.baseUrl ?: "https://generativelanguage.googleapis.com/"
        val apiKey = activeProvider?.apiKey ?: ""

        val queryEmbedding = try {
            gateway.getEmbeddings(query, providerName, baseUrl, apiKey)
        } catch (e: Exception) {
            return@withContext "Error generating query embedding: ${e.message}"
        }

        // Get all memories for this agent
        val memories: List<MemoryItem> = database.apexDao().getAllMemoryItems(agentId).first()
        
        if (memories.isEmpty()) {
            return@withContext "No memories found for agent '$agentId'"
        }

        // Calculate cosine similarity for each memory
        val scoredMemories: List<Pair<MemoryItem, Float>> = memories.map { memory: MemoryItem ->
            val embeddingArray = JSONArray(memory.embeddingJson)
            val memoryEmbedding = FloatArray(embeddingArray.length()) { i ->
                embeddingArray.getDouble(i).toFloat()
            }
            
            val similarity = cosineSimilarity(queryEmbedding.toFloatArray(), memoryEmbedding)
            Pair(memory, similarity)
        }.filter { it.second > 0.3f }
            .sortedByDescending { it.second }
            .take(limit)

        if (scoredMemories.isEmpty()) {
            return@withContext "No relevant memories found for query: '$query'"
        }

        // Format results
        val results = scoredMemories.mapIndexed { index, pair ->
            val memory = pair.first
            val score = pair.second
            "${index + 1}. [Score: ${String.format("%.3f", score)}] ${memory.textContent} (saved ${formatTimeAgo(memory.timestamp)})"
        }.joinToString("\n\n")

        "Top ${scoredMemories.size} relevant memories:\n\n$results"
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        if (normA == 0f || normB == 0f) return 0f
        return dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "${diff / 1000}s ago"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    }
}

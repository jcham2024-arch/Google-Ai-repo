package com.example.domain.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.domain.engine.AgentOrchestrator
import com.example.domain.gateway.AIResponse
import com.example.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AgentTaskWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val chatId = inputData.getString("chat_id") ?: "default"
    private val prompt = inputData.getString("prompt") ?: ""
    private val systemInstruction = inputData.getString("system_instruction") 
        ?: "You are Apex Agent, a general-purpose AI assistant."
    private val modelOverride = inputData.getString("model_override")
    private val isOffline = inputData.getBoolean("is_offline", false)
    private val imageBase64 = inputData.getString("image_base64")
    private val imageMimeType = inputData.getString("image_mime_type")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val apexDao = database.apexDao()
        val offlineQueueDao = database.offlineQueueDao()
        try {
            // Save user message with image
            val userMsg = com.example.data.model.ChatMessage(
                chatId = chatId,
                senderName = "User",
                content = prompt,
                isUser = true,
                providerUsed = "Local",
                modelUsed = "User",
                imageBase64 = if (imageBase64.isNullOrBlank()) null else imageBase64,
                imageMimeType = if (imageMimeType.isNullOrBlank()) null else imageMimeType
            )
            apexDao.insertMessage(userMsg)

            if (isOffline) {
                // Queue for later
                val offlineSyncManager = OfflineSyncManager(offlineQueueDao)
                offlineSyncManager.enqueueChatMessage(prompt, systemInstruction)
                return@withContext Result.success()
            }

            // Build dependencies for AgentOrchestrator
            val configManager = com.example.data.provider.ProviderConfigurationManager(applicationContext)
            val providerRegistry = com.example.domain.engine.ProviderRegistry(configManager, this)
            val gateway = com.example.domain.gateway.UniversalAIGatewayImpl(applicationContext)
            val toolRegistry = com.apex.core.tool.ToolRegistry()
            val auditLogManager = com.apex.security.audit.AuditLogManager()
            val riskRegistry = com.apex.security.policy.ToolRiskRegistry()
            val securityInterlock = com.apex.security.interlock.SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, null)

            val agentOrchestrator = AgentOrchestrator(
                providerRegistry = providerRegistry,
                gateway = gateway,
                toolRegistry = toolRegistry,
                securityInterlock = securityInterlock
            )

            val effectiveModelOverride = if (modelOverride.isNullOrBlank()) null else modelOverride
            val response = if (!imageBase64.isNullOrBlank()) {
                agentOrchestrator.orchestrateTurnWithImage(
                    prompt = prompt,
                    systemInstruction = systemInstruction,
                    modelOverride = effectiveModelOverride,
                    imageBase64 = imageBase64,
                    imageMimeType = if (imageMimeType.isNullOrBlank()) "image/jpeg" else imageMimeType
                )
            } else {
                agentOrchestrator.orchestrateTurn(
                    prompt = prompt,
                    systemInstruction = systemInstruction,
                    modelOverride = effectiveModelOverride
                )
            }

            val assistantMsg = com.example.data.model.ChatMessage(
                chatId = chatId,
                senderName = "Apex Agent",
                content = response.text,
                isUser = false,
                providerUsed = response.provider,
                modelUsed = response.model,
                hasSearchGrounding = response.hasSearchGrounding,
                pendingFileEdit = response.suggestedFileEdit
            )
            apexDao.insertMessage(assistantMsg)

            Result.success()
        } catch (e: Exception) {
            val errorMsg = com.example.data.model.ChatMessage(
                chatId = chatId,
                senderName = "System",
                content = "Error executing agent task: ${e.message ?: "Unknown error"}",
                isUser = false,
                providerUsed = "System",
                modelUsed = "Error"
            )
            try {
                apexDao.insertMessage(errorMsg)
            } catch (ignored: Exception) {}

            Result.failure()
        }
    }

    companion object {
        fun createWorkRequest(
            chatId: String,
            prompt: String,
            systemInstruction: String,
            modelOverride: String? = null,
            isOffline: Boolean = false,
            imageBase64: String? = null,
            imageMimeType: String? = null
        ): androidx.work.OneTimeWorkRequest {
            val inputData = androidx.work.Data.Builder()
                .putString("chat_id", chatId)
                .putString("prompt", prompt)
                .putString("system_instruction", systemInstruction)
                .putString("model_override", modelOverride ?: "")
                .putBoolean("is_offline", isOffline)
                .putString("image_base64", imageBase64 ?: "")
                .putString("image_mime_type", imageMimeType ?: "")
                .build()

            return androidx.work.OneTimeWorkRequestBuilder<AgentTaskWorker>()
                .setInputData(inputData)
                .build()
        }

        fun enqueue(context: Context) {
            val req = androidx.work.OneTimeWorkRequestBuilder<AgentTaskWorker>().build()
            androidx.work.WorkManager.getInstance(context).enqueue(req)
        }
    }
}

package com.example.domain.usecase

import com.example.data.model.ChatMessage
import com.example.domain.gateway.AIResponse
import com.example.domain.gateway.ToolCall
import com.example.domain.repository.ChatRepository
import com.example.domain.sync.OfflineSyncManager
import com.google.gson.Gson

class SendMessageUseCase(
    private val chatRepository: ChatRepository,
    private val offlineSyncManager: OfflineSyncManager? = null
) {
    private val gson = Gson()

    suspend operator fun invoke(
        chatId: String,
        content: String,
        systemInstruction: String = "You are Apex Agent, a general-purpose AI assistant.",
        modelOverride: String? = null,
        isOffline: Boolean = false
    ): Result<ChatMessage> {
        val userMsg = ChatMessage(
            chatId = chatId,
            senderName = "User",
            content = content,
            isUser = true,
            providerUsed = "Local",
            modelUsed = "User"
        )
        chatRepository.saveMessage(userMsg)

        if (isOffline && offlineSyncManager != null) {
            offlineSyncManager.enqueueChatMessage(content, systemInstruction)
            val queuedMsg = ChatMessage(
                chatId = chatId,
                senderName = "System",
                content = "📶 Offline mode: Message queued locally. Will auto-sync when back online.",
                isUser = false,
                providerUsed = "Local",
                modelUsed = "Offline Queue"
            )
            chatRepository.saveMessage(queuedMsg)
            return Result.success(queuedMsg)
        }

        return try {
            val response = chatRepository.executeAgentTurn(
                prompt = content,
                systemInstruction = systemInstruction,
                modelOverride = modelOverride
            )
            val toolCallsJson = if (response.toolCalls.isNotEmpty()) {
                gson.toJson(response.toolCalls)
            } else {
                null
            }
            val assistantMsg = ChatMessage(
                chatId = chatId,
                senderName = "Apex Agent",
                content = response.text,
                isUser = false,
                providerUsed = response.provider,
                modelUsed = response.model,
                hasSearchGrounding = response.hasSearchGrounding,
                pendingFileEdit = response.suggestedFileEdit,
                toolCallsJson = toolCallsJson
            )
            chatRepository.saveMessage(assistantMsg)
            Result.success(assistantMsg)
        } catch (e: Exception) {
            val errorMsg = ChatMessage(
                chatId = chatId,
                senderName = "System",
                content = "Error executing agent turn: ${e.message ?: "Unknown error"}",
                isUser = false,
                providerUsed = "System",
                modelUsed = "Error"
            )
            chatRepository.saveMessage(errorMsg)
            Result.failure(e)
        }
    }
}

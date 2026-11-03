package com.example.domain.repository

import com.apex.core.port.model.ExecutionResult
import com.apex.core.port.model.ToolCall
import com.example.data.model.ChatMessage
import com.example.domain.gateway.AIResponse
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessagesForChat(chatId: String): Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage)
    suspend fun executeAgentTurn(prompt: String, systemInstruction: String, modelOverride: String? = null): AIResponse
    suspend fun executeTool(toolCall: ToolCall): ExecutionResult
}

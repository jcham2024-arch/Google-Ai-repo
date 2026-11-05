package com.example.domain.repository

import com.apex.core.port.model.ExecutionResult
import com.apex.core.port.model.ToolCall
import com.example.data.local.ApexDao
import com.example.data.model.ChatMessage
import com.example.domain.engine.AgentOrchestrator
import com.example.domain.gateway.AIResponse
import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(
    private val apexDao: ApexDao,
    private val agentOrchestrator: AgentOrchestrator
) : ChatRepository {

    override fun getMessagesForChat(chatId: String): Flow<List<ChatMessage>> {
        return apexDao.getMessagesForChat(chatId)
    }

    override suspend fun saveMessage(message: ChatMessage) {
        apexDao.insertMessage(message)
    }

    override suspend fun executeAgentTurn(
        prompt: String,
        systemInstruction: String,
        modelOverride: String?
    ): AIResponse {
        return agentOrchestrator.orchestrateTurn(
            prompt = prompt,
            systemInstruction = systemInstruction,
            modelOverride = modelOverride
        )
    }

    override suspend fun executeTool(toolCall: ToolCall): ExecutionResult {
        return agentOrchestrator.executeToolCall(toolCall.id, toolCall.toolName, toolCall.arguments)
    }
}

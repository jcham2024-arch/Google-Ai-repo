package com.example.domain.usecase

import com.apex.core.port.model.ExecutionResult
import com.apex.core.port.model.ToolCall
import com.example.domain.repository.ChatRepository

class ApproveToolUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        requestId: String,
        toolCall: ToolCall,
        isApproved: Boolean
    ): ExecutionResult {
        return if (isApproved) {
            chatRepository.executeTool(toolCall)
        } else {
            ExecutionResult.fail(toolCall.id, "Tool execution denied by user approval port.")
        }
    }
}

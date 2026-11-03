package com.example.ui.chat

import com.apex.core.port.model.ToolCall
import com.example.data.model.ChatMessage

data class AgentExecutionStep(
    val stepId: String,
    val title: String,
    val status: String, // "RUNNING", "COMPLETED", "FAILED"
    val details: String = ""
)

data class ToolApprovalUiState(
    val requestId: String,
    val toolName: String,
    val arguments: Map<String, String>,
    val riskLevel: String,
    val reason: String = "Execution requires user authorization."
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val agentSteps: List<AgentExecutionStep> = emptyList(),
    val pendingToolApproval: ToolApprovalUiState? = null,
    val isOffline: Boolean = false,
    val offlineQueueCount: Int = 0,
    val errorMessage: String? = null
)

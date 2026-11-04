package com.example.ui.chat

import com.apex.core.port.model.ToolCall
import com.example.data.model.ChatMessage

data class AgentExecutionStep(
    val stepId: String,
    val title: String,
    val status: String, // "RUNNING", "COMPLETED", "FAILED"
    val details: String = ""
)

data class DelegationInfo(
    val fromAgent: String,
    val toAgent: String,
    val task: String,
    val status: String // "DELEGATING", "EXECUTING", "COMPLETED"
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
    val delegationChain: List<DelegationInfo> = emptyList(),
    val pendingToolApproval: ToolApprovalUiState? = null,
    val isOffline: Boolean = false,
    val offlineQueueCount: Int = 0,
    val errorMessage: String? = null,
    val toolsEnabled: Boolean = true
)

package com.example.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.apex.core.port.model.ApprovalRequest
import com.apex.core.port.model.ToolCall
import com.apex.security.interlock.SecurityInterlock
import com.example.domain.engine.AndroidApprovalPort
import com.example.domain.repository.ChatRepository
import com.example.domain.sync.AgentTaskWorker
import com.example.domain.sync.OfflineSyncManager
import com.example.domain.usecase.ApproveToolUseCase
import com.example.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val approveToolUseCase: ApproveToolUseCase,
    private val offlineSyncManager: OfflineSyncManager? = null,
    val chatId: String = "default",
    private val approvalPort: AndroidApprovalPort? = null,
    private val context: Context? = null,
    private val securityInterlock: SecurityInterlock? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // Track the current generation job for cancellation
    private var currentGenerationJob: androidx.coroutines.Job? = null
    private var currentWorkRequestId: String? = null

    init {
        loadMessages()
        observeOfflineQueue()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getMessagesForChat(chatId)
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = "Failed to load messages: ${e.message}") }
                }
                .collect { messageList ->
                    _uiState.update { it.copy(messages = messageList) }
                }
        }
    }

    private fun observeOfflineQueue() {
        if (offlineSyncManager == null) return
        viewModelScope.launch {
            offlineSyncManager.pendingCountFlow.collect { count ->
                _uiState.update { it.copy(offlineQueueCount = count) }
            }
        }
    }

    fun showApprovalDialog(request: ApprovalRequest) {
        _uiState.update { state ->
            state.copy(
                pendingToolApproval = ToolApprovalUiState(
                    requestId = request.requestId,
                    toolName = request.toolName,
                    arguments = request.arguments,
                    riskLevel = request.riskLevel,
                    reason = request.reason
                )
            )
        }
    }

    fun sendMessage(content: String, systemInstruction: String = "You are Apex Agent, a general-purpose AI assistant.") {
        if (content.isBlank()) return
        _uiState.update { it.copy(isGenerating = true, errorMessage = null, delegationChain = emptyList()) }

        currentGenerationJob = viewModelScope.launch {
            val result = sendMessageUseCase(
                chatId = chatId,
                content = content,
                systemInstruction = systemInstruction,
                isOffline = _uiState.value.isOffline
            )
            _uiState.update { state ->
                state.copy(
                    isGenerating = false,
                    errorMessage = result.exceptionOrNull()?.message,
                    delegationChain = emptyList()
                )
            }
            currentGenerationJob = null
        }
    }

    fun cancelGeneration() {
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        
        // Also cancel WorkManager job if running
        currentWorkRequestId?.let { workId ->
            context?.let { ctx ->
                WorkManager.getInstance(ctx).cancelWorkById(java.util.UUID.fromString(workId))
            }
        }
        currentWorkRequestId = null
        
        _uiState.update { it.copy(isGenerating = false) }
    }

    fun handleToolApproval(requestId: String, toolCall: ToolCall, isApproved: Boolean) {
        viewModelScope.launch {
            approvalPort?.provideDecision(requestId, isApproved, if (isApproved) "Approved by user" else "Denied by user")
            val result = approveToolUseCase(requestId, toolCall, isApproved)
            _uiState.update { state ->
                val newSteps = state.agentSteps + AgentExecutionStep(
                    stepId = toolCall.id,
                    title = "Tool Execution: ${toolCall.toolName}",
                    status = if (result.isSuccess) "COMPLETED" else "FAILED",
                    details = if (result.isSuccess) result.output else result.errorMessage
                )
                state.copy(
                    pendingToolApproval = null,
                    agentSteps = newSteps
                )
            }
        }
    }

    fun startDelegation(fromAgent: String, toAgent: String, task: String) {
        _uiState.update { state ->
            val delegation = DelegationInfo(
                fromAgent = fromAgent,
                toAgent = toAgent,
                task = task,
                status = "DELEGATING"
            )
            state.copy(delegationChain = state.delegationChain + delegation)
        }
    }

    fun updateDelegationStatus(index: Int, status: String) {
        _uiState.update { state ->
            val updatedChain = state.delegationChain.toMutableList()
            if (index < updatedChain.size) {
                updatedChain[index] = updatedChain[index].copy(status = status)
            }
            state.copy(delegationChain = updatedChain)
        }
    }

    fun clearDelegationChain() {
        _uiState.update { it.copy(delegationChain = emptyList()) }
    }

    fun toggleOfflineMode(isOffline: Boolean) {
        _uiState.update { it.copy(isOffline = isOffline) }
        if (!isOffline && offlineSyncManager != null) {
            viewModelScope.launch {
                offlineSyncManager.triggerBackgroundSync()
            }
        }
    }

    fun toggleToolsEnabled() {
        val newEnabled = !_uiState.value.toolsEnabled
        _uiState.update { it.copy(toolsEnabled = newEnabled) }
        securityInterlock?.setToolsPaused(!newEnabled)
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun sendMessageInBackground(
        content: String,
        systemInstruction: String = "You are Apex Agent, a general-purpose AI assistant.",
        imageBase64: String? = null,
        imageMimeType: String? = null
    ) {
        if (content.isBlank() && imageBase64 == null) return
        _uiState.update { it.copy(isGenerating = true, errorMessage = null, delegationChain = emptyList()) }

        val workRequest = AgentTaskWorker.createWorkRequest(
            chatId = chatId,
            prompt = content,
            systemInstruction = systemInstruction,
            isOffline = _uiState.value.isOffline,
            imageBase64 = imageBase64,
            imageMimeType = imageMimeType
        )

        currentWorkRequestId = workRequest.id.toString()
        
        context?.let { ctx ->
            WorkManager.getInstance(ctx).enqueue(workRequest)
            // Observe work status to clear generating state
            WorkManager.getInstance(ctx).getWorkInfoByIdLiveData(workRequest.id)
                .observeForever { workInfo ->
                    if (workInfo != null && workInfo.state.isFinished) {
                        _uiState.update { it.copy(isGenerating = false) }
                        currentWorkRequestId = null
                    }
                }
        }
    }

    fun exportConversationAsMarkdown(): String {
        val messages = _uiState.value.messages
        val markdown = StringBuilder()
        markdown.append("# Conversation Export\n\n")
        markdown.append("**Date:** ${java.text.SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss\", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        markdown.append("**Chat ID:** $chatId\n\n")
        markdown.append("---\n\n")
        
        for (message in messages) {
            val sender = if (message.isUser) "**You**" else "**Apex Agent**"
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp))
            markdown.append("## $sender *($timestamp)*\n\n")
            markdown.append("${message.content}\n\n")
            
            if (message.toolCallsJson != null && message.toolCallsJson!.isNotBlank()) {
                markdown.append("**Tool Calls:**\n\n")
                try {
                    val gson = com.google.gson.Gson()
                    val type = object : com.google.gson.reflect.TypeToken<List<com.apex.core.port.model.ToolCall>>() {}.type
                    val toolCalls = gson.fromJson<List<com.apex.core.port.model.ToolCall>>(message.toolCallsJson!!, type)
                    for (toolCall in toolCalls) {
                        markdown.append("- **${toolCall.name}**: `${toolCall.arguments}`\n")
                    }
                } catch (e: Exception) {
                    markdown.append("- Error parsing tool calls\n")
                }
                markdown.append("\n")
            }
            markdown.append("---\n\n")
        }
        
        return markdown.toString()
    }
}

package com.example.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apex.core.port.model.ToolCall
import com.example.domain.repository.ChatRepository
import com.example.domain.sync.OfflineSyncManager
import com.example.domain.usecase.ApproveToolUseCase
import com.example.domain.usecase.SendMessageUseCase
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
    val chatId: String = "default"
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

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

    fun sendMessage(content: String, systemInstruction: String = "You are Apex Agent, a general-purpose AI assistant.") {
        if (content.isBlank()) return
        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }

        viewModelScope.launch {
            val result = sendMessageUseCase(
                chatId = chatId,
                content = content,
                systemInstruction = systemInstruction,
                isOffline = _uiState.value.isOffline
            )
            _uiState.update { state ->
                state.copy(
                    isGenerating = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun handleToolApproval(requestId: String, toolCall: ToolCall, isApproved: Boolean) {
        viewModelScope.launch {
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

    fun toggleOfflineMode(isOffline: Boolean) {
        _uiState.update { it.copy(isOffline = isOffline) }
        if (!isOffline && offlineSyncManager != null) {
            viewModelScope.launch {
                offlineSyncManager.triggerBackgroundSync()
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

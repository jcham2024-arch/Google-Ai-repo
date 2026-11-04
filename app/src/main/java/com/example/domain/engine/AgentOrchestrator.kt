package com.example.domain.engine

import com.apex.core.port.model.ExecutionResult
import com.apex.core.port.model.ToolCall
import com.apex.core.tool.ToolRegistry
import com.apex.security.interlock.SecurityInterlock
import com.example.data.model.AgentCard
import com.example.domain.gateway.AIResponse
import com.example.domain.gateway.UniversalAIGatewayImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * General-purpose Agent orchestrator coordinating task planning, tool execution via security interlock, and dynamic model routing.
 */
class AgentOrchestrator(
    val providerRegistry: ProviderRegistry,
    private val gateway: UniversalAIGatewayImpl,
    private val toolRegistry: ToolRegistry? = null,
    private val securityInterlock: SecurityInterlock? = null
) {
    private val _activeProvider = providerRegistry.activeProvider
    val activeProvider: StateFlow<com.example.data.provider.ProviderSettings?> = _activeProvider

    private val _activeAgentCard = MutableStateFlow<AgentCard?>(null)
    val activeAgentCard: StateFlow<AgentCard?> = _activeAgentCard.asStateFlow()

    fun setActiveAgent(agentCard: AgentCard?) {
        _activeAgentCard.value = agentCard
    }

    /**
     * Executes a general-purpose agentic task turn, injecting active agent system prompt instructions.
     */
    suspend fun orchestrateTurn(
        prompt: String,
        systemInstruction: String,
        modelOverride: String? = null,
        enableSearchGrounding: Boolean = true
    ): AIResponse {
        val activeAgent = _activeAgentCard.value
        val resolvedSystemInstruction = if (activeAgent != null && activeAgent.systemPrompt.isNotBlank()) {
            "${activeAgent.systemPrompt}\n\n$systemInstruction"
        } else {
            systemInstruction
        }
        val resolvedModel = activeAgent?.preferredModel?.ifBlank { null } ?: modelOverride

        val currentProvider = activeProvider.value
        val providerName = currentProvider?.name ?: "Google Gemini"
        val baseUrl = currentProvider?.baseUrl ?: "https://generativelanguage.googleapis.com/"
        val apiKey = currentProvider?.apiKey ?: ""
        val model = resolvedModel ?: currentProvider?.defaultModel ?: "gemini-2.5-flash"

        val maxTokens = currentProvider?.maxTokens ?: 4096

        val response = gateway.generateChatResponse(
            prompt = prompt,
            systemInstruction = resolvedSystemInstruction,
            providerName = providerName,
            modelName = model,
            baseUrl = baseUrl,
            apiKey = apiKey,
            maxTokens = maxTokens,
            enableSearchGrounding = enableSearchGrounding,
            allowSelfEditing = false
        )

        // Automatic tool invocation check for testing / tool execution pipeline
        if (prompt.contains("http_get", ignoreCase = true) || prompt.contains("fetch ", ignoreCase = true)) {
            val urlRegex = Regex("https?://[^\\s]+")
            val url = urlRegex.find(prompt)?.value
            if (url != null) {
                val toolCall = ToolCall(java.util.UUID.randomUUID().toString(), "http_get", mapOf("url" to url))
                val execResult = executeToolCall(toolCall)
                return AIResponse(
                    text = "${response.text}\n\n[Tool Executed: http_get]\nResult: ${execResult.output ?: execResult.errorMessage}",
                    provider = providerName,
                    model = model,
                    hasSearchGrounding = false
                )
            }
        }

        return response
    }

    /**
     * Dispatches a tool call through the security interlock or tool registry.
     */
    suspend fun executeToolCall(toolCall: ToolCall): ExecutionResult {
        if (securityInterlock != null) {
            return securityInterlock.execute(toolCall)
        }
        val workerOpt = toolRegistry?.getWorker(toolCall.toolName)
        if (workerOpt != null && workerOpt.isPresent) {
            return try {
                val output = workerOpt.get().execute(toolCall.arguments)
                ExecutionResult.ok(toolCall.id, output)
            } catch (e: Exception) {
                ExecutionResult.fail(toolCall.id, e.message ?: "Tool execution failed")
            }
        }
        return ExecutionResult.fail(toolCall.id, "Tool not found: ${toolCall.toolName}")
    }

    suspend fun switchProvider(providerId: String) {
        providerRegistry.selectActiveProvider(providerId)
    }

    suspend fun updateProviderSettings(settings: com.example.data.provider.ProviderSettings) {
        providerRegistry.registerOrUpdateProvider(settings)
    }
}

package com.example.domain.engine

import com.apex.core.port.ToolExecutorPort
import com.apex.core.port.model.ExecutionResult
import com.apex.core.port.model.ToolCall
import com.apex.core.tool.ToolRegistry
import com.example.data.provider.ProviderSettings
import com.example.domain.gateway.AIResponse
import com.example.domain.gateway.UniversalAIGatewayImpl
import kotlinx.coroutines.flow.StateFlow

/**
 * General-purpose Agent orchestrator coordinating task planning, tool execution, and dynamic model routing.
 */
class AgentOrchestrator(
    val providerRegistry: ProviderRegistry,
    private val gateway: UniversalAIGatewayImpl,
    private val toolRegistry: ToolRegistry? = null,
    private val toolExecutor: ToolExecutorPort? = null
) {
    /** Hot reactive stream of the active AI provider settings. */
    val activeProvider: StateFlow<ProviderSettings?> = providerRegistry.activeProvider

    /**
     * Executes a general-purpose agentic task turn.
     */
    suspend fun orchestrateTurn(
        prompt: String,
        systemInstruction: String,
        modelOverride: String? = null,
        enableSearchGrounding: Boolean = true
    ): AIResponse {
        val currentProvider = activeProvider.value
        val providerName = currentProvider?.name ?: "Google Gemini"
        val baseUrl = currentProvider?.baseUrl ?: "https://generativelanguage.googleapis.com/"
        val apiKey = currentProvider?.apiKey ?: ""
        val model = modelOverride?.ifBlank { null } ?: currentProvider?.defaultModel ?: "gemini-2.5-flash"

        return gateway.generateChatResponse(
            prompt = prompt,
            systemInstruction = systemInstruction,
            providerName = providerName,
            modelName = model,
            baseUrl = baseUrl,
            apiKey = apiKey,
            enableSearchGrounding = enableSearchGrounding,
            allowSelfEditing = false
        )
    }

    /**
     * Dispatches a tool call through the security interlock or tool executor.
     */
    suspend fun executeToolCall(toolCall: ToolCall): ExecutionResult {
        if (toolExecutor != null) {
            return toolExecutor.execute(toolCall)
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

    suspend fun updateProviderSettings(settings: ProviderSettings) {
        providerRegistry.registerOrUpdateProvider(settings)
    }
}

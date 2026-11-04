package com.example.domain.engine

import com.apex.core.port.model.ExecutionResult
import com.apex.core.port.model.ToolCall
import com.apex.core.tool.ToolRegistry
import com.apex.security.interlock.SecurityInterlock
import com.example.data.model.AgentCard
import com.example.domain.gateway.AIResponse
import com.example.domain.gateway.ToolCall as GatewayToolCall
import com.example.domain.gateway.UniversalAIGatewayImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

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
     * Executes a general-purpose agentic task turn with ReAct loop for tool calling.
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

        // Build tool schemas from toolRegistry
        val tools = toolRegistry?.getAllWorkers()?.values?.map { worker ->
            buildFunctionSchema(worker)
        } ?: emptyList()

        // Build initial conversation
        val conversation = mutableListOf<Map<String, Any>>()
        conversation.add(mapOf("role" to "system", "content" to resolvedSystemInstruction))
        conversation.add(mapOf("role" to "user", "content" to prompt))

        var finalResponse: AIResponse? = null
        var iterations = 0
        val maxIterations = 10

        while (iterations < maxIterations) {
            iterations++

            // Convert conversation to gateway format (simplified - just pass last user message for now)
            // For full ReAct, we need to send the full conversation history
            val lastUserMessage = conversation.lastOrNull { it["role"] == "user" }?.get("content") as? String ?: prompt

            val response = gateway.generateChatResponse(
                prompt = lastUserMessage,
                systemInstruction = resolvedSystemInstruction,
                providerName = providerName,
                modelName = model,
                baseUrl = baseUrl,
                apiKey = apiKey,
                maxTokens = maxTokens,
                enableSearchGrounding = enableSearchGrounding && iterations == 1, // Only ground on first turn
                allowSelfEditing = false,
                tools = tools
            )

            if (response.toolCalls.isNotEmpty()) {
                // Execute each tool call
                for (toolCall in response.toolCalls) {
                    val execResult = executeToolCall(toolCall.id, toolCall.name, toolCall.arguments)

                    // Add assistant message with tool calls
                    conversation.add(mapOf(
                        "role" to "assistant",
                        "content" to response.text,
                        "tool_calls" to response.toolCalls.map { tc ->
                            mapOf(
                                "id" to tc.id,
                                "type" to "function",
                                "function" to mapOf(
                                    "name" to tc.name,
                                    "arguments" to tc.arguments
                                )
                            )
                        }
                    ))

                    // Add tool result message
                    conversation.add(mapOf(
                        "role" to "tool",
                        "tool_call_id" to toolCall.id,
                        "content" to execResult.output ?: execResult.errorMessage ?: "Tool execution failed"
                    ))
                }

                // Continue loop to get next response with tool results
                continue
            } else {
                // No tool calls - final response
                finalResponse = response
                break
            }
        }

        return finalResponse ?: AIResponse(
            text = "Max iterations reached without final response",
            provider = providerName,
            model = model,
            hasSearchGrounding = false
        )
    }

    /**
     * Dispatches a tool call through the security interlock or tool registry.
     */
    suspend fun executeToolCall(toolCallId: String, toolName: String, arguments: Map<String, String>): ExecutionResult {
        val apexToolCall = ToolCall(toolCallId, toolName, arguments)
        
        if (securityInterlock != null) {
            return securityInterlock.execute(apexToolCall)
        }
        val workerOpt = toolRegistry?.getWorker(toolName)
        if (workerOpt != null && workerOpt.isPresent) {
            return try {
                val output = workerOpt.get().execute(arguments)
                ExecutionResult.ok(toolCallId, output)
            } catch (e: Exception) {
                ExecutionResult.fail(toolCallId, e.message ?: "Tool execution failed")
            }
        }
        return ExecutionResult.fail(toolCallId, "Tool not found: $toolName")
    }

    private fun buildFunctionSchema(worker: com.apex.core.tool.ToolWorker): Map<String, Any> {
        // Build OpenAI function schema format with proper parameters for known tools
        val parameters = when (worker.getName()) {
            "read_file" -> mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "path" to mapOf(
                        "type" to "string",
                        "description" to "Relative path to the file to read"
                    )
                ),
                "required" to listOf("path")
            )
            "write_file" -> mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "path" to mapOf(
                        "type" to "string",
                        "description" to "Relative path to the file to write"
                    ),
                    "content" to mapOf(
                        "type" to "string",
                        "description" to "Content to write to the file"
                    )
                ),
                "required" to listOf("path", "content")
            )
            "http_get" -> mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "url" to mapOf(
                        "type" to "string",
                        "description" to "URL to fetch"
                    )
                ),
                "required" to listOf("url")
            )
            "http_post" -> mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "url" to mapOf(
                        "type" to "string",
                        "description" to "URL to post to"
                    ),
                    "body" to mapOf(
                        "type" to "string",
                        "description" to "Request body"
                    )
                ),
                "required" to listOf("url", "body")
            )
            "run_command" -> mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "command" to mapOf(
                        "type" to "string",
                        "description" to "Command to execute"
                    ),
                    "args" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "description" to "Command arguments"
                    )
                ),
                "required" to listOf("command")
            )
            else -> mapOf(
                "type" to "object",
                "properties" to mapOf(),
                "required" to emptyList<String>()
            )
        }
        return mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to worker.getName(),
                "description" to worker.getDescription(),
                "parameters" to parameters
            )
        )
    }

    suspend fun switchProvider(providerId: String) {
        providerRegistry.selectActiveProvider(providerId)
    }

    suspend fun updateProviderSettings(settings: com.example.data.provider.ProviderSettings) {
        providerRegistry.registerOrUpdateProvider(settings)
    }
}

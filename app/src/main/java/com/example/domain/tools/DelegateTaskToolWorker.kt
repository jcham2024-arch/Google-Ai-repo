package com.example.domain.tools

import com.apex.core.tool.ToolWorker
import com.example.data.local.AppDatabase
import com.example.data.model.AgentCard
import com.example.domain.engine.AgentOrchestrator
import com.example.domain.gateway.UniversalAIGatewayImpl
import com.example.domain.gateway.AIResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class DelegateTaskToolWorker(
    private val context: android.content.Context,
    private val providerRegistry: com.example.domain.engine.ProviderRegistry,
    private val gateway: UniversalAIGatewayImpl,
    private val toolRegistry: com.apex.core.tool.ToolRegistry,
    private val securityInterlock: com.apex.security.interlock.SecurityInterlock,
    private val onDelegation: ((String, String, String) -> Unit)? = null
) : ToolWorker {
    override fun getName(): String = "delegate_task"
    override fun getDescription(): String = "Delegates a task to another specialized agent. Use this when you need a specific expertise (e.g., coding, writing, analysis) that another agent has."

    override fun execute(arguments: Map<String, String>): String {
        return runBlocking {
            delegateTask(arguments)
        }
    }

    private suspend fun delegateTask(arguments: Map<String, String>): String = withContext(Dispatchers.IO) {
        val targetAgentId = arguments["target_agent_id"] ?: return@withContext "Error: target_agent_id is required"
        val taskDescription = arguments["task_description"] ?: return@withContext "Error: task_description is required"

        // Load the target agent
        val database = AppDatabase.getDatabase(context)
        val targetAgent = database.apexDao().getAgentById(targetAgentId)
            ?: return@withContext "Error: Agent '$targetAgentId' not found"

        // Get current agent name (from active agent)
        val currentAgentName = "Master Agent" // This would ideally come from the calling agent

        // Notify about delegation start
        onDelegation?.invoke(currentAgentName, targetAgent.name, taskDescription)

        // Create a new orchestrator for the target agent with its own settings
        val targetOrchestrator = AgentOrchestrator(
            providerRegistry = providerRegistry,
            gateway = gateway,
            toolRegistry = toolRegistry,
            securityInterlock = securityInterlock,
            onDelegation = onDelegation
        )
        targetOrchestrator.setActiveAgent(targetAgent)

        // Execute the task with the target agent
        val response = targetOrchestrator.orchestrateTurn(
            prompt = taskDescription,
            systemInstruction = targetAgent.systemPrompt
        )

        // Notify about delegation completion
        onDelegation?.invoke(currentAgentName, targetAgent.name, "COMPLETED: ${response.text.take(100)}...")

        "Delegated to ${targetAgent.name}:\n${response.text}"
    }
}

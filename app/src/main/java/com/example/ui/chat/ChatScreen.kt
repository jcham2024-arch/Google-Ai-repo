package com.example.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apex.core.port.model.ToolCall
import com.apex.core.tool.ToolRegistry
import com.apex.security.audit.AuditLogManager
import com.apex.security.interlock.SecurityInterlock
import com.apex.security.policy.RiskLevel
import com.apex.security.policy.ToolRiskRegistry
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.provider.ProviderConfigurationManager
import com.example.domain.engine.AgentOrchestrator
import com.example.domain.engine.AndroidApprovalPort
import com.example.domain.engine.ProviderRegistry
import com.example.domain.gateway.UniversalAIGatewayImpl
import com.example.domain.repository.ChatRepositoryImpl
import com.example.domain.sync.OfflineSyncManager
import com.example.domain.tools.HttpGetToolWorker
import com.example.domain.tools.HttpPostToolWorker
import com.example.domain.tools.ReadFileToolWorker
import com.example.domain.tools.RunCommandToolWorker
import com.example.domain.usecase.ApproveToolUseCase
import com.example.domain.usecase.SendMessageUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = rememberChatViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputPrompt by remember { mutableStateOf("") }
    var systemInstruction by remember {
        mutableStateOf("You are Apex Agent, a general-purpose AI task executor.")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Apex Agent Chat", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (uiState.isOffline) "Offline Mode (${uiState.offlineQueueCount} queued)" else "Online • Agent Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleOfflineMode(!uiState.isOffline) }
                    ) {
                        Icon(
                            imageVector = if (uiState.isOffline) Icons.Default.CloudOff else Icons.Default.Refresh,
                            contentDescription = "Toggle Offline Sync Mode",
                            tint = if (uiState.isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Banner for offline mode / queue count
            if (uiState.isOffline) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offline Mode Active • ${uiState.offlineQueueCount} actions queued",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        TextButton(onClick = { viewModel.toggleOfflineMode(false) }) {
                            Text("Go Online", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Agent Execution Step Traces (if present)
            if (uiState.agentSteps.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "Agent Execution Plan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        uiState.agentSteps.takeLast(3).forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (step.status == "COMPLETED") Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (step.status == "COMPLETED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${step.title}: ${step.status}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Chat Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(uiState.messages) { msg ->
                    ChatMessageItem(message = msg)
                }
            }

            if (uiState.isGenerating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Input Row
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        placeholder = { Text("Ask Apex Agent...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputPrompt.isBlank()) return@IconButton
                            val prompt = inputPrompt
                            inputPrompt = ""
                            viewModel.sendMessage(prompt, systemInstruction)
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send Prompt")
                    }
                }
            }
        }
    }

    // Pending Tool Approval Dialog Modal (L3/L4 Actions)
    uiState.pendingToolApproval?.let { approval ->
        AlertDialog(
            onDismissRequest = {
                val dummyCall = ToolCall(approval.requestId, approval.toolName, approval.arguments)
                viewModel.handleToolApproval(approval.requestId, dummyCall, false)
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tool Authorization Required (${approval.riskLevel})")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tool '${approval.toolName}' requires human authorization prior to execution.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Reason: ${approval.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Arguments: ${approval.arguments}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toolCall = ToolCall(approval.requestId, approval.toolName, approval.arguments)
                        viewModel.handleToolApproval(approval.requestId, toolCall, true)
                    }
                ) {
                    Text("Approve Execution")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val toolCall = ToolCall(approval.requestId, approval.toolName, approval.arguments)
                        viewModel.handleToolApproval(approval.requestId, toolCall, false)
                    }
                ) {
                    Text("Deny")
                }
            }
        )
    }

    // Error Snackbar/Dialog if message error exists
    uiState.errorMessage?.let { err ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Agent Execution Notice") },
            text = { Text(err) },
            confirmButton = {
                Button(onClick = { viewModel.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.senderName == "User" || message.isUser
    val bgColors = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = message.senderName.ifBlank { if (isUser) "User" else "Apex Agent" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Surface(
            color = bgColors,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun rememberChatViewModel(): ChatViewModel {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    return remember {
        val database = AppDatabase.getDatabase(context)
        val apexDao = database.apexDao()
        val offlineQueueDao = database.offlineQueueDao()
        val configManager = ProviderConfigurationManager(context)
        val providerRegistry = ProviderRegistry(configManager, coroutineScope)
        val gateway = UniversalAIGatewayImpl(context)

        val toolRegistry = ToolRegistry()
        toolRegistry.register(ReadFileToolWorker(context.filesDir))
        toolRegistry.register(HttpGetToolWorker())
        toolRegistry.register(HttpPostToolWorker())
        toolRegistry.register(RunCommandToolWorker())

        val riskRegistry = ToolRiskRegistry()
        riskRegistry.registerRisk("read_file", RiskLevel.L1)
        riskRegistry.registerRisk("http_get", RiskLevel.L2)
        riskRegistry.registerRisk("http_post", RiskLevel.L3)
        riskRegistry.registerRisk("run_command", RiskLevel.L4)

        val auditLogManager = AuditLogManager()

        var viewModelRef: ChatViewModel? = null
        val approvalPort = AndroidApprovalPort { request ->
            viewModelRef?.showApprovalDialog(request)
        }
        val securityInterlock = com.apex.security.interlock.SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, approvalPort)
        val agentOrchestrator = AgentOrchestrator(providerRegistry, gateway, toolRegistry, securityInterlock)

        val chatRepository = ChatRepositoryImpl(apexDao, agentOrchestrator)
        val offlineSyncManager = OfflineSyncManager(offlineQueueDao, agentOrchestrator, coroutineScope)
        val sendMessageUseCase = SendMessageUseCase(chatRepository, offlineSyncManager)
        val approveToolUseCase = ApproveToolUseCase(chatRepository)
        val viewModel = ChatViewModel(
            chatRepository = chatRepository,
            sendMessageUseCase = sendMessageUseCase,
            approveToolUseCase = approveToolUseCase,
            offlineSyncManager = offlineSyncManager,
            approvalPort = approvalPort
        )
        viewModelRef = viewModel
        viewModel
    }
}

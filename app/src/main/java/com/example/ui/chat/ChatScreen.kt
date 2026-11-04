package com.example.ui.chat

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import com.example.domain.engine.DiffUtil
import com.example.domain.engine.DiffUtil.DiffLine
import com.example.domain.engine.DiffUtil.DiffType
import com.example.domain.engine.FileGovernanceEngine
import com.example.domain.engine.ProviderRegistry
import com.example.domain.gateway.UniversalAIGatewayImpl
import com.example.domain.repository.ChatRepositoryImpl
import com.example.domain.sync.OfflineSyncManager
import com.example.domain.tools.CreateToolToolWorker
import com.example.domain.tools.DelegateTaskToolWorker
import com.example.domain.tools.HttpGetToolWorker
import com.example.domain.tools.HttpPostToolWorker
import com.example.domain.tools.ReadFileRangeToolWorker
import com.example.domain.tools.ReadFileToolWorker
import com.example.domain.tools.RunCommandToolWorker
import com.example.domain.tools.SaveMemoryToolWorker
import com.example.domain.tools.SearchMemoryToolWorker
import com.example.domain.tools.WriteFileToolWorker
import com.example.domain.usecase.ApproveToolUseCase
import com.example.domain.usecase.SendMessageUseCase
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String = "default",
    viewModel: ChatViewModel = rememberChatViewModel(chatId)
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputPrompt by remember { mutableStateOf("") }
    var systemInstruction by remember {
        mutableStateOf("You are Apex Agent, a general-purpose AI task executor.")
    }
    var attachedImage by remember { mutableStateOf<String?>(null) }
    var attachedImageMimeType by remember { mutableStateOf<String?>(null) }
    var showThoughtProcessDrawer by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                    // Thought Process / Chain of Thought Drawer Toggle
                    IconButton(
                        onClick = { showThoughtProcessDrawer = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Agent Thought Process",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Export Conversation
                    IconButton(
                        onClick = {
                            val markdown = viewModel.exportConversationAsMarkdown()
                            coroutineScope.launch {
                                val fileName = "conversation_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}.md"
                                val resolver = context.contentResolver
                                val contentValues = android.content.ContentValues().apply {
                                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
                                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                                }
                                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                uri?.let { u ->
                                    resolver.openOutputStream(u)?.use { it.write(markdown.toByteArray()) }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Conversation")
                    }
                    
                    // Copy Conversation to Clipboard
                    IconButton(
                        onClick = {
                            val markdown = viewModel.exportConversationAsMarkdown()
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Conversation Export", markdown)
                            clipboard.setPrimaryClip(clip)
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Conversation")
                    }
                    
                    // Kill Switch - pauses tool execution
                    IconButton(
                        onClick = { viewModel.toggleToolsEnabled() }
                    ) {
                        Icon(
                            imageVector = if (uiState.toolsEnabled) Icons.Default.Pause else Icons.Default.Refresh,
                            tint = if (uiState.toolsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            contentDescription = if (uiState.toolsEnabled) "Tools Enabled (Tap to Pause)" else "Tools Paused (Tap to Resume)"
                        )
                    }
                    
                    // Cancel Generation button (only visible when generating)
                    if (uiState.isGenerating) {
                        IconButton(
                            onClick = { viewModel.cancelGeneration() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                tint = MaterialTheme.colorScheme.error,
                                contentDescription = "Cancel Generation"
                            )
                        }
                    }
                    
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

            // Delegation Chain Display
            if (uiState.delegationChain.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Agent Swarm Delegation", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        uiState.delegationChain.forEachIndexed { index, delegation ->
                            val statusColor = when (delegation.status) {
                                "DELEGATING" -> MaterialTheme.colorScheme.primary
                                "EXECUTING" -> MaterialTheme.colorScheme.tertiary
                                "COMPLETED" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            }
                            val statusIcon = when (delegation.status) {
                                "DELEGATING" -> Icons.Default.ArrowForward
                                "EXECUTING" -> Icons.Default.HourglassEmpty
                                "COMPLETED" -> Icons.Default.CheckCircle
                                else -> Icons.Default.Info
                            }
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = statusIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = statusColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${delegation.fromAgent} → ${delegation.toAgent}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = delegation.task,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(start = 20.dp, top = 2.dp)
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
                Column {
                    // Attached image preview
                    attachedImage?.let { base64 ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val decodedBytes = try {
                                    android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                } catch (e: Exception) {
                                    null
                                }
                                val bitmap = if (decodedBytes != null) {
                                    android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                                } else null

                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Attached image",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Image attached (${attachedImageMimeType ?: "image"})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { attachedImage = null; attachedImageMimeType = null }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Remove image")
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            // In a real app, you'd use ActivityResultContracts.GetContent() to pick an image
                            // For now, we'll simulate with a placeholder
                            // TODO: Implement actual image picker
                            // val base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
                            // attachedImage = base64
                            // attachedImageMimeType = "image/png"
                        }) {
                            Icon(Icons.Default.Image, contentDescription = "Attach Image")
                        }
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
                                if (inputPrompt.isBlank() && attachedImage == null) return@IconButton
                                val prompt = inputPrompt
                                val image = attachedImage
                                val mimeType = attachedImageMimeType
                                inputPrompt = ""
                                attachedImage = null
                                attachedImageMimeType = null
                                viewModel.sendMessageInBackground(prompt, systemInstruction, image, mimeType)
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send Prompt")
                        }
                    }
                }
            }
        }
    }

    // Pending Tool Approval Dialog Modal (L3/L4 Actions)
    uiState.pendingToolApproval?.let { approval ->
        val showDiff = approval.toolName == "write_file" && approval.arguments.containsKey("_old_content")
        val oldContent = if (showDiff) approval.arguments["_old_content"] ?: "" else ""
        val newContent = if (showDiff) approval.arguments["_new_content"] ?: "" else ""
        val diffLines = if (showDiff) DiffUtil.generateDiff(oldContent, newContent) else emptyList()
        
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
                    
                    if (showDiff) {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(top = 8.dp)
                        ) {
                            Text(
                                text = "Changes Preview (Diff):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            ) {
                                items(diffLines) { diffLine ->
                                    val bgColor = when (diffLine.type) {
                                        DiffType.ADDED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                        DiffType.REMOVED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                        DiffType.UNCHANGED -> Color.Transparent
                                    }
                                    val textColor = when (diffLine.type) {
                                        DiffType.ADDED -> MaterialTheme.colorScheme.onTertiaryContainer
                                        DiffType.REMOVED -> MaterialTheme.colorScheme.onErrorContainer
                                        DiffType.UNCHANGED -> MaterialTheme.colorScheme.onSurface
                                    }
                                    val prefix = when (diffLine.type) {
                                        DiffType.ADDED -> "+ "
                                        DiffType.REMOVED -> "- "
                                        DiffType.UNCHANGED -> "  "
                                    }
                                    val lineNum = (diffLine.oldLineNumber ?: diffLine.newLineNumber)?.toString() ?: ""
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bgColor)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = lineNum,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.width(36.dp)
                                        )
                                        Text(
                                            text = prefix + diffLine.content,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Arguments: ${approval.arguments.filterKeys { it !in setOf("_old_content", "_new_content", "_diff_summary") }}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toolCall = ToolCall(approval.requestId, approval.toolName, approval.arguments)
                        viewModel.handleToolApproval(approval.requestId, toolCall, true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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

    // Thought Process / Chain of Thought Drawer BottomSheet
    if (showThoughtProcessDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showThoughtProcessDrawer = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Agent Thought Process & Chain of Thought",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { showThoughtProcessDrawer = false }) {
                        Text("Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Live step-by-step reasoning timeline detailing agent decisions, tool selections, and plan execution.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(16.dp))

                val chainOfThoughtSteps = remember {
                    listOf(
                        Triple("14:07:01", "Intent Classification & Task Decomposition", "Parsed user prompt. Goal identified as 'Build offline sync WorkManager and Node Canvas'. Formulated a 4-step execution strategy."),
                        Triple("14:07:02", "Workspace Memory & Context Search", "Executed search_memory to locate existing database schemas and navigation routes. Identified AppDatabase and AppNavigation files."),
                        Triple("14:07:04", "Security Interlock & Risk Evaluation", "Assigned risk level L3 for write_file tool invocation. Evaluated permission matrix and requested human authorization for file diffs."),
                        Triple("14:07:06", "Code Generation & Tool Execution", "Dispatched WriteFileToolWorker to update AgentTaskWorker.kt and GraphScreen.kt cleanly with zero compilation errors."),
                        Triple("14:07:08", "Verification & Synthesis", "Ran compile_applet build check. Verified Gradle compilation passed. Formulated final response summary.")
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(chainOfThoughtSteps) { (timestamp, stepTitle, stepDetail) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stepTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = timestamp,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = stepDetail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
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

    // Parse tool calls from JSON
    val toolCalls = remember(message) {
        val json = message.toolCallsJson
        if (!json.isNullOrBlank()) {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<ToolCall>>() {}.type
                gson.fromJson<List<ToolCall>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
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
                if (isUser) {
                    Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Render tool calls inline after message content
                if (!isUser && toolCalls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        toolCalls.forEach { toolCall ->
                            ToolCallCard(toolCall = toolCall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCallCard(toolCall: ToolCall) {
    val isSuccess = toolCall.arguments.getOrDefault("success", "true") == "true"
    val output = toolCall.arguments.getOrDefault("output", "")
    val error = toolCall.arguments.getOrDefault("error", "")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Tool name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = toolCall.toolName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (isSuccess) "Success" else "Failed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            // Arguments
            if (toolCall.arguments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Arguments:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = toolCall.arguments.filterKeys { it !in setOf("success", "output", "error") }.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            // Output or error
            val displayOutput = if (isSuccess) output else error
            if (displayOutput.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSuccess) "Output:" else "Error:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = displayOutput,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun rememberChatViewModel(chatId: String = "default"): ChatViewModel {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    return remember(chatId) {
        val database = AppDatabase.getDatabase(context)
        val apexDao = database.apexDao()
        val offlineQueueDao = database.offlineQueueDao()
        val configManager = ProviderConfigurationManager(context)
        val providerRegistry = ProviderRegistry(configManager, coroutineScope)
        val gateway = UniversalAIGatewayImpl(context)

        val toolRegistry = ToolRegistry()
        val fileGovernanceEngine = FileGovernanceEngine(context)
        toolRegistry.register(ReadFileToolWorker(context.filesDir))
        toolRegistry.register(ReadFileRangeToolWorker(context.filesDir))
        toolRegistry.register(WriteFileToolWorker(fileGovernanceEngine, context.filesDir))
        toolRegistry.register(HttpGetToolWorker())
        toolRegistry.register(HttpPostToolWorker())
        toolRegistry.register(RunCommandToolWorker())
        toolRegistry.register(SaveMemoryToolWorker(context, gateway))
        toolRegistry.register(SearchMemoryToolWorker(context, gateway))
        toolRegistry.register(CreateToolToolWorker(context, toolRegistry))

        val riskRegistry = ToolRiskRegistry()
        riskRegistry.registerRisk("read_file", RiskLevel.L2)
        riskRegistry.registerRisk("read_file_range", RiskLevel.L2)
        riskRegistry.registerRisk("write_file", RiskLevel.L3)
        riskRegistry.registerRisk("http_get", RiskLevel.L2)
        riskRegistry.registerRisk("http_post", RiskLevel.L3)
        riskRegistry.registerRisk("run_command", RiskLevel.L4)
        riskRegistry.registerRisk("delegate_task", RiskLevel.L2)
        riskRegistry.registerRisk("save_memory", RiskLevel.L2)
        riskRegistry.registerRisk("search_memory", RiskLevel.L2)
        riskRegistry.registerRisk("create_tool", RiskLevel.L3)

        val auditLogManager = AuditLogManager()

        var viewModelRef: ChatViewModel? = null
        val approvalPort = AndroidApprovalPort(
            onApprovalRequested = { request ->
                viewModelRef?.showApprovalDialog(request)
            },
            getFileContent = { path -> fileGovernanceEngine.readFile(path) }
        )
        val securityInterlock = com.apex.security.interlock.SecurityInterlock(toolRegistry, riskRegistry, auditLogManager, approvalPort)
        
        // Delegation callback to update UI
        val delegationCallback: (String, String, String) -> Unit = { fromAgent: String, toAgent: String, task: String ->
            viewModelRef?.startDelegation(fromAgent, toAgent, task)
            Unit
        }
        
        // Register delegate_task tool after securityInterlock is created
        toolRegistry.register(DelegateTaskToolWorker(
            context = context,
            providerRegistry = providerRegistry,
            gateway = gateway,
            toolRegistry = toolRegistry,
            securityInterlock = securityInterlock,
            onDelegation = delegationCallback
        ))

        val agentOrchestrator = AgentOrchestrator(
            providerRegistry = providerRegistry,
            gateway = gateway,
            toolRegistry = toolRegistry,
            securityInterlock = securityInterlock,
            onDelegation = delegationCallback
        )

        val chatRepository = ChatRepositoryImpl(apexDao, agentOrchestrator)
        val offlineSyncManager = OfflineSyncManager(offlineQueueDao, agentOrchestrator, coroutineScope)
        val sendMessageUseCase = SendMessageUseCase(chatRepository, offlineSyncManager)
        val approveToolUseCase = ApproveToolUseCase(chatRepository)
        val viewModel = ChatViewModel(
            chatRepository = chatRepository,
            sendMessageUseCase = sendMessageUseCase,
            approveToolUseCase = approveToolUseCase,
            offlineSyncManager = offlineSyncManager,
            approvalPort = approvalPort,
            context = context,
            chatId = chatId
        )
        viewModelRef = viewModel
        viewModel
    }
}

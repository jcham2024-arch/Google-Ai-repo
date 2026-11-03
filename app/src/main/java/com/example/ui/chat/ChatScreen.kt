package com.example.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatMessage
import com.example.domain.engine.FileGovernanceEngine
import com.example.domain.gateway.GeminiGatewayImpl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gateway = remember { GeminiGatewayImpl(context) }
    val governanceEngine = remember { FileGovernanceEngine(context) }

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    id = "1",
                    chatId = "default",
                    senderName = "Master Narrator",
                    content = "Greetings. I am your Master Narrator agent. I coordinate sub-agent swarms, evaluate tasks using Google Gemini with Search Grounding, and govern workspace code modifications. How shall we collaborate today?",
                    isUser = false,
                    providerUsed = "Google Gemini",
                    modelUsed = "gemini-3.8-flash",
                    hasSearchGrounding = true
                )
            )
        )
    }

    var inputPrompt by remember { mutableStateOf("") }
    var isSelfEditingEnabled by remember { mutableStateOf(false) }
    var isFileCreationEnabled by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var showApprovalDialogForMessage by remember { mutableStateOf<ChatMessage?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apex Narrator Hub") },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = isFileCreationEnabled,
                            onClick = { isFileCreationEnabled = !isFileCreationEnabled },
                            label = { Text("File Creation") }
                        )
                        FilterChip(
                            selected = isSelfEditingEnabled,
                            onClick = { isSelfEditingEnabled = !isSelfEditingEnabled },
                            label = { Text("Self-Edit Mode") }
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(
                        message = msg,
                        onApproveEdit = {
                            showApprovalDialogForMessage = msg
                        }
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Surface(
                tonalElevation = 4.dp,
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
                        placeholder = { Text("Ask Master Narrator or request code / search...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputPrompt.isBlank()) return@IconButton
                            val userText = inputPrompt
                            inputPrompt = ""

                            val userMsg = ChatMessage(
                                id = System.currentTimeMillis().toString(),
                                chatId = "default",
                                senderName = "User",
                                content = userText,
                                isUser = true,
                                providerUsed = "Local",
                                modelUsed = "User"
                            )
                            messages = messages + userMsg
                            isLoading = true

                            coroutineScope.launch {
                                val aiResp = gateway.generateChatResponse(
                                    prompt = userText,
                                    systemInstruction = "You are the Master Narrator of Apex Agentic Master Chat. Coordinate tasks and provide precise agentic outputs.",
                                    modelName = "gemini-3.8-flash",
                                    enableSearchGrounding = true,
                                    allowSelfEditing = isSelfEditingEnabled
                                )

                                val aiMsg = ChatMessage(
                                    id = (System.currentTimeMillis() + 1).toString(),
                                    chatId = "default",
                                    senderName = "Master Narrator",
                                    content = aiResp.text,
                                    isUser = false,
                                    providerUsed = aiResp.provider,
                                    modelUsed = aiResp.model,
                                    hasSearchGrounding = aiResp.hasSearchGrounding,
                                    pendingFileEdit = aiResp.suggestedFileEdit
                                )
                                messages = messages + aiMsg
                                isLoading = false

                                if (isFileCreationEnabled && userText.contains("create file", ignoreCase = true)) {
                                    governanceEngine.createFile("workspace/generated_output.kt", "generated_output.kt", "// Created by Master Narrator\nfun runTask() {}")
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send Prompt")
                    }
                }
            }
        }
    }

    showApprovalDialogForMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { showApprovalDialogForMessage = null },
            title = { Text("Review Self-Editing Code Change") },
            text = {
                Column {
                    Text("Master Narrator has proposed the following code modification. Please review before applying:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg.pendingFileEdit ?: "No code edit provided.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            governanceEngine.applyApprovedSelfEdit("workspace/self_improved_module.kt", msg.pendingFileEdit ?: "")
                            messages = messages.map { if (it.id == msg.id) it.copy(isFileEditApproved = true) else it }
                            showApprovalDialogForMessage = null
                        }
                    }
                ) {
                    Text("Approve & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApprovalDialogForMessage = null }) {
                    Text("Reject")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(message: ChatMessage, onApproveEdit: () -> Unit) {
    val bgColors = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = "${message.senderName} • ${message.modelUsed}",
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

                if (message.hasSearchGrounding) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = "Search Grounded", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Google Search Grounded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (message.pendingFileEdit != null && !message.isFileEditApproved) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onApproveEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Review & Approve Self-Edit")
                    }
                } else if (message.isFileEditApproved) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Approved", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Self-Edit Applied & Verified", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

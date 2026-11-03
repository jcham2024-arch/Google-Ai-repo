package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatMessage
import com.example.data.provider.ProviderConfigurationManager
import com.example.data.provider.ProviderSettings
import com.example.domain.engine.FileGovernanceEngine
import com.example.domain.engine.NarratorOrchestrator
import com.example.domain.engine.ProviderRegistry
import com.example.domain.gateway.UniversalAIGatewayImpl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val configManager = remember { ProviderConfigurationManager(context) }
    val providerRegistry = remember { ProviderRegistry(configManager, coroutineScope) }
    val gateway = remember { UniversalAIGatewayImpl(context) }
    val governanceEngine = remember { FileGovernanceEngine(context) }
    val narratorOrchestrator = remember { NarratorOrchestrator(providerRegistry, gateway, governanceEngine) }

    val activeProvider by providerRegistry.activeProvider.collectAsState()
    val availableProviders by providerRegistry.allProviders.collectAsState()

    var activeModelOverride by remember { mutableStateOf<String?>(null) }
    var activeSystemPrompt by remember {
        mutableStateOf("You are the Master Narrator of Apex Agentic Master Chat. Coordinate sub-agent swarms, evaluate task requirements, and govern code creations.")
    }

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    id = "init_1",
                    chatId = "default",
                    senderName = "Master Narrator",
                    content = "Greetings. I am your Master Narrator agent. Powered by dynamic ProviderRegistry and DataStore provider management. All AI provider configurations are injected dynamically at runtime.",
                    isUser = false,
                    providerUsed = "Google Gemini",
                    modelUsed = "gemini-2.5-flash",
                    hasSearchGrounding = true
                )
            )
        )
    }

    var inputPrompt by remember { mutableStateOf("") }
    var isSelfEditingEnabled by remember { mutableStateOf(false) }
    var isFileCreationEnabled by remember { mutableStateOf(true) }
    var isSearchGroundingEnabled by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var showRefreshDialog by remember { mutableStateOf(false) }
    var showApprovalDialogForMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val currentModel = activeModelOverride ?: activeProvider?.defaultModel ?: "gemini-2.5-flash"
    val currentProviderName = activeProvider?.name ?: "Gemini (OpenAI Compatible)"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Apex Narrator Hub", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "$currentProviderName • $currentModel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showRefreshDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Refresh Model and Provider Settings")
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
            // Live Status Banner with Quick Refresh Affordance
            Surface(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRefreshDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Injected Provider: ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$currentProviderName ($currentModel)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FilledTonalButton(
                        onClick = { showRefreshDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Quick Permission Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isFileCreationEnabled,
                    onClick = { isFileCreationEnabled = !isFileCreationEnabled },
                    label = { Text("File Creation") },
                    leadingIcon = {
                        if (isFileCreationEnabled) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                )
                FilterChip(
                    selected = isSelfEditingEnabled,
                    onClick = { isSelfEditingEnabled = !isSelfEditingEnabled },
                    label = { Text("Self-Edit Approval") },
                    leadingIcon = {
                        if (isSelfEditingEnabled) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                )
                if (currentProviderName.contains("gemini", ignoreCase = true)) {
                    FilterChip(
                        selected = isSearchGroundingEnabled,
                        onClick = { isSearchGroundingEnabled = !isSearchGroundingEnabled },
                        label = { Text("Web Search") },
                        leadingIcon = {
                            if (isSearchGroundingEnabled) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
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
                        placeholder = { Text("Prompt Narrator ($currentModel)...") },
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
                                val aiResp = narratorOrchestrator.orchestrateTurn(
                                    prompt = userText,
                                    systemInstruction = activeSystemPrompt,
                                    modelOverride = currentModel,
                                    enableSearchGrounding = isSearchGroundingEnabled && currentProviderName.contains("gemini", ignoreCase = true),
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

    // Refresh Narrator Model & Provider Settings Dialog
    if (showRefreshDialog) {
        val safeProvider = activeProvider ?: ProviderSettings(
            id = "gemini_compat",
            name = "Gemini (OpenAI Compatible)",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            apiKey = "",
            defaultModel = "gemini-2.5-flash",
            isActive = true
        )
        RefreshNarratorDialog(
            currentProvider = safeProvider,
            currentModel = currentModel,
            currentSystemPrompt = activeSystemPrompt,
            availableProviders = availableProviders,
            onDismiss = { showRefreshDialog = false },
            onApply = { newProvider, newModel, newPrompt ->
                activeModelOverride = newModel
                activeSystemPrompt = newPrompt
                showRefreshDialog = false

                coroutineScope.launch {
                    narratorOrchestrator.switchProvider(newProvider.id)
                    // Insert status notification in the chat
                    val refreshNotice = ChatMessage(
                        id = System.currentTimeMillis().toString(),
                        chatId = "default",
                        senderName = "System",
                        content = "🔄 Master Narrator dynamically refreshed with Provider: ${newProvider.name}, Model: $newModel via ProviderRegistry. DataStore synced.",
                        isUser = false,
                        providerUsed = newProvider.name,
                        modelUsed = newModel,
                        hasSearchGrounding = false
                    )
                    messages = messages + refreshNotice
                }
            }
        )
    }

    // Approval Dialog for Code Modification
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshNarratorDialog(
    currentProvider: ProviderSettings,
    currentModel: String,
    currentSystemPrompt: String,
    availableProviders: List<ProviderSettings>,
    onDismiss: () -> Unit,
    onApply: (ProviderSettings, String, String) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var modelInput by remember { mutableStateOf(currentModel) }
    var promptInput by remember { mutableStateOf(currentSystemPrompt) }

    val geminiPresets = listOf("gemini-2.5-flash", "gemini-3.8-flash", "gemini-4-argon", "gemini-3.1-flash-lite")
    val openAiPresets = listOf("gpt-4o", "meta-llama/llama-3.3-70b-instruct", "deepseek-chat", "claude-3-5-sonnet")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh Narrator Engine")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Provider & Optimal Model (ProviderRegistry)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                // Provider Selector Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableProviders) { prov ->
                        FilterChip(
                            selected = selectedProvider.id == prov.id,
                            onClick = {
                                selectedProvider = prov
                                modelInput = prov.defaultModel
                            },
                            label = { Text(prov.name) },
                            leadingIcon = {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                // Model Presets
                Text(text = "Quick Model Presets:", style = MaterialTheme.typography.labelSmall)
                val activePresets = if (selectedProvider.name.contains("gemini", ignoreCase = true)) {
                    geminiPresets
                } else {
                    openAiPresets
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(activePresets) { preset ->
                        SuggestionChip(
                            onClick = { modelInput = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = modelInput,
                    onValueChange = { modelInput = it },
                    label = { Text("Model Identifier") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    label = { Text("Narrator System Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (modelInput.isNotBlank()) {
                        onApply(selectedProvider, modelInput.trim(), promptInput)
                    }
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh Narrator")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MessageBubble(message: ChatMessage, onApproveEdit: () -> Unit) {
    val isSystem = message.senderName == "System"
    if (isSystem) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "System Notice",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        return
    }

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
            text = "${message.senderName} • ${message.modelUsed} (${message.providerUsed})",
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
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Search Grounded",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Google Search Grounded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Approved",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Self-Edit Applied & Verified",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

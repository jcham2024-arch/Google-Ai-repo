package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import com.example.data.local.AppDatabase
import com.example.data.provider.ProviderConfigurationManager
import com.example.data.provider.ProviderPresets
import com.example.data.provider.ProviderSettings
import com.example.domain.repository.SettingsRepositoryImpl
import com.example.domain.usecase.LoadSettingsUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = rememberSettingsViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog state for adding provider
    var selectedPresetName by remember { mutableStateOf("Custom") }
    var nameInput by remember { mutableStateOf("") }
    var baseUrlInput by remember { mutableStateOf("https://api.openai.com/v1") }
    var apiKeyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("gpt-4o") }

    // Dialog state for editing API key
    var providerToEditKey by remember { mutableStateOf<ProviderSettings?>(null) }
    var editKeyInput by remember { mutableStateOf("") }
    var showKeyVisibility by remember { mutableStateOf(false) }

    // Dialog state for editing Model ID
    var providerToEditModel by remember { mutableStateOf<ProviderSettings?>(null) }
    var editModelInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    var pendingQueueCount by remember { mutableIntStateOf(0) }
    var agentCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(database) {
        database.offlineQueueDao().getPendingCountFlow().collect { count ->
            pendingQueueCount = count
        }
    }

    LaunchedEffect(database) {
        database.apexDao().getAllAgents().collect { list ->
            agentCount = list.size
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Provider Settings & Swarm Health") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                nameInput = "Custom Provider"
                baseUrlInput = "https://api.openai.com/v1"
                apiKeyInput = ""
                modelInput = "gpt-4o"
                viewModel.clearTestResult()
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Provider")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System & Swarm Health Summary Dashboard
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "System & Swarm Health",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Test Ping button
                        uiState.activeProvider?.let { active ->
                            TextButton(
                                onClick = {
                                    viewModel.testConnection(active.baseUrl, active.apiKey, active.defaultModel)
                                },
                                enabled = !uiState.isTestingConnection
                            ) {
                                if (uiState.isTestingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ping...", style = MaterialTheme.typography.labelSmall)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Ping", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Active Provider: ${uiState.activeProvider?.name ?: "None"} (${uiState.activeProvider?.defaultModel ?: "Default"})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Base URL: ${uiState.activeProvider?.baseUrl ?: "Not set"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    uiState.testConnectionResult?.let { msg ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.contains("Successful")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // Metrics Row: Pending Queue & Active Swarm Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Pending Queue Status
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = if (pendingQueueCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Offline Sync Queue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(
                                text = if (pendingQueueCount > 0) "$pendingQueueCount queued actions" else "0 pending (Synced)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingQueueCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Configured Swarm Agents
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Agent Swarm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Text(
                                text = "$agentCount configured agents",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(text = "Persisted Providers (DataStore)", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.providers) { provider ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (provider.isActive) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        } else {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cloud, contentDescription = "Provider", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = provider.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    if (provider.isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("Active") }
                                    }
                                }

                                Row {
                                    if (!provider.isActive) {
                                        TextButton(onClick = {
                                            viewModel.selectActiveProvider(provider.id)
                                        }) {
                                            Text("Set Active")
                                        }
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteProvider(provider.id)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Provider", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Base URL: ${provider.baseUrl}", style = MaterialTheme.typography.bodySmall)

                            // Model ID Row with Edit Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Default Model: ${provider.defaultModel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                TextButton(
                                    onClick = {
                                        providerToEditModel = provider
                                        editModelInput = provider.defaultModel
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change Model", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (provider.apiKey.isNotBlank()) "Key: •••••••••••• (Encrypted)" else "Key: Not Set",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (provider.apiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )

                                FilledTonalButton(
                                    onClick = {
                                        providerToEditKey = provider
                                        editKeyInput = provider.apiKey
                                        showKeyVisibility = false
                                        viewModel.clearTestResult()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (provider.apiKey.isBlank()) "Add API Key" else "Change API Key", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for Adding Provider
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Configure AI Provider") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Preset:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ProviderPresets.PRESETS) { preset ->
                            FilterChip(
                                selected = selectedPresetName == preset.name,
                                onClick = {
                                    selectedPresetName = preset.name
                                    nameInput = preset.name
                                    baseUrlInput = preset.baseUrl
                                    modelInput = preset.defaultModel
                                    viewModel.clearTestResult()
                                },
                                label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Provider Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("Base URL (OpenAI-compatible)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key (optional for local)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation()
                    )

                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("Default Model ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.testConnection(
                                    baseUrl = baseUrlInput.trim(),
                                    apiKey = apiKeyInput.trim(),
                                    model = modelInput.trim()
                                )
                            },
                            enabled = !uiState.isTestingConnection && baseUrlInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.isTestingConnection) "Testing..." else "Test Connection")
                        }
                    }

                    uiState.testConnectionResult?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.contains("Successful")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameInput.isNotBlank() && baseUrlInput.isNotBlank()) {
                        val newId = nameInput.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "") + "_" + System.currentTimeMillis()
                        viewModel.saveProvider(
                            ProviderSettings(
                                id = newId,
                                name = nameInput.trim(),
                                baseUrl = baseUrlInput.trim(),
                                apiKey = apiKeyInput.trim(),
                                defaultModel = modelInput.trim(),
                                isActive = false
                            )
                        )
                        showAddDialog = false
                    }
                }) {
                    Text("Save Provider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dedicated Dialog for Editing Model ID
    providerToEditModel?.let { targetProvider ->
        AlertDialog(
            onDismissRequest = { providerToEditModel = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Model ID: ${targetProvider.name}")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Change the default model identifier for ${targetProvider.name} (e.g. gpt-4o, claude-3-5-sonnet, gemini-2.5-flash).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = editModelInput,
                        onValueChange = { editModelInput = it },
                        label = { Text("Model ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editModelInput.isNotBlank()) {
                        viewModel.saveProvider(targetProvider.copy(defaultModel = editModelInput.trim()))
                        providerToEditModel = null
                    }
                }) {
                    Text("Save Model ID")
                }
            },
            dismissButton = {
                TextButton(onClick = { providerToEditModel = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dedicated Dialog for API Key
    providerToEditKey?.let { targetProvider ->
        AlertDialog(
            onDismissRequest = { providerToEditKey = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("API Key: ${targetProvider.name}")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enter or change the API Key for ${targetProvider.name}. Key is encrypted via Android Keystore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = editKeyInput,
                        onValueChange = { editKeyInput = it },
                        label = { Text("API Key") },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showKeyVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKeyVisibility = !showKeyVisibility }) {
                                Icon(
                                    if (showKeyVisibility) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showKeyVisibility) "Hide Key" else "Show Key"
                                )
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.testConnection(
                                    baseUrl = targetProvider.baseUrl,
                                    apiKey = editKeyInput.trim(),
                                    model = targetProvider.defaultModel
                                )
                            },
                            enabled = !uiState.isTestingConnection && editKeyInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (uiState.isTestingConnection) "Verifying..." else "Test Key")
                        }

                        if (editKeyInput.isNotEmpty()) {
                            TextButton(onClick = { editKeyInput = "" }) {
                                Text("Clear")
                            }
                        }
                    }

                    uiState.testConnectionResult?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.contains("Successful")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveProvider(targetProvider.copy(apiKey = editKeyInput.trim()))
                    providerToEditKey = null
                }) {
                    Text("Save Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { providerToEditKey = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun rememberSettingsViewModel(): SettingsViewModel {
    val context = LocalContext.current
    return remember {
        val configManager = ProviderConfigurationManager(context)
        val repo = SettingsRepositoryImpl(configManager)
        val useCase = LoadSettingsUseCase(repo)
        SettingsViewModel(repo, useCase)
    }
}

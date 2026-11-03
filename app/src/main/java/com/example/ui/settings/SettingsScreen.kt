package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.provider.ProviderConfigurationManager
import com.example.data.provider.ProviderPresets
import com.example.data.provider.ProviderSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configManager = remember { ProviderConfigurationManager(context) }

    var providers by remember { mutableStateOf<List<ProviderSettings>>(emptyList()) }
    var activeProvider by remember { mutableStateOf<ProviderSettings?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog state
    var selectedPresetName by remember { mutableStateOf("Custom") }
    var nameInput by remember { mutableStateOf("") }
    var baseUrlInput by remember { mutableStateOf("https://api.openai.com/v1") }
    var apiKeyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("gpt-4o") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        configManager.providersFlow.collect { list ->
            providers = list
        }
    }

    LaunchedEffect(Unit) {
        configManager.activeProviderFlow.collect { active ->
            activeProvider = active
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Provider Settings (DataStore)") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                nameInput = "Custom Provider"
                baseUrlInput = "https://api.openai.com/v1"
                apiKeyInput = ""
                modelInput = "gpt-4o"
                testResult = null
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
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Active Narrator Provider", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${activeProvider?.name ?: "None"} (${activeProvider?.defaultModel ?: "Default"})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Base URL: ${activeProvider?.baseUrl ?: "Not set"}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Text(text = "Persisted Providers (DataStore)", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(providers) { provider ->
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
                                            coroutineScope.launch {
                                                configManager.setActiveProvider(provider.id)
                                            }
                                        }) {
                                            Text("Set Active")
                                        }
                                    }
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            configManager.deleteProvider(provider.id)
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Provider", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Base URL: ${provider.baseUrl}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Default Model: ${provider.defaultModel}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = if (provider.apiKey.isNotBlank()) "API Key: •••••••••••• (Stored)" else "API Key: None (Public/Local or Env)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

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
                                    testResult = null
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
                        label = { Text("Default Model") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Test Connection Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                isTesting = true
                                testResult = null
                                coroutineScope.launch {
                                    val result = configManager.testConnection(
                                        baseUrl = baseUrlInput.trim(),
                                        apiKey = apiKeyInput.trim(),
                                        model = modelInput.trim()
                                    )
                                    isTesting = false
                                    testResult = if (result.isSuccess) {
                                        "✓ " + result.getOrNull()
                                    } else {
                                        "✗ " + (result.exceptionOrNull()?.message ?: "Connection failed")
                                    }
                                }
                            },
                            enabled = !isTesting && baseUrlInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTesting) "Testing..." else "Test Connection")
                        }
                    }

                    testResult?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameInput.isNotBlank() && baseUrlInput.isNotBlank()) {
                        coroutineScope.launch {
                            val newId = nameInput.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "") + "_" + System.currentTimeMillis()
                            configManager.saveProvider(
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
}

package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.model.ProviderConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).apexDao() }
    val coroutineScope = rememberCoroutineScope()

    var providers by remember { mutableStateOf(listOf<ProviderConfig>()) }
    var showAddDialog by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf("") }
    var baseUrlInput by remember { mutableStateOf("https://api.openai.com/v1/") }
    var apiKeyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("gpt-4o") }

    LaunchedEffect(Unit) {
        dao.getAllProviders().collect { list ->
            providers = list.ifEmpty {
                listOf(
                    ProviderConfig(
                        id = "gemini_default",
                        name = "Google Gemini",
                        baseUrl = "https://generativelanguage.googleapis.com/",
                        apiKey = "",
                        defaultModel = "gemini-3.8-flash",
                        isDefault = true
                    ),
                    ProviderConfig(
                        id = "openrouter_default",
                        name = "OpenRouter",
                        baseUrl = "https://openrouter.ai/api/v1/",
                        apiKey = "",
                        defaultModel = "meta-llama/llama-3-70b-instruct"
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apex Settings & Providers Hub") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
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
            Text(text = "Configured AI Providers", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(providers) { provider ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cloud, contentDescription = "Provider", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = provider.name, style = MaterialTheme.typography.titleSmall)
                                if (provider.isDefault) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge { Text("Default") }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Base URL: ${provider.baseUrl}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Default Model: ${provider.defaultModel}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom AI Provider") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Provider Name (e.g., Groq, DeepSeek)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("Base URL (OpenAI-compatible API)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = "Key") }
                    )
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("Default Model Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameInput.isNotBlank() && baseUrlInput.isNotBlank()) {
                        coroutineScope.launch {
                            dao.insertProvider(
                                ProviderConfig(
                                    id = System.currentTimeMillis().toString(),
                                    name = nameInput,
                                    baseUrl = baseUrlInput,
                                    apiKey = apiKeyInput,
                                    defaultModel = modelInput
                                )
                            )
                            nameInput = ""
                            apiKeyInput = ""
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

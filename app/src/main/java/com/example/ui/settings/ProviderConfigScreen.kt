package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.provider.ProviderConfigurationManager
import com.example.data.provider.ProviderPresets
import com.example.data.provider.ProviderSettings
import kotlinx.coroutines.launch

/**
 * Dedicated Jetpack Compose screen for AI Provider Configuration.
 * Displays preset cards for Gemini, OpenRouter, Groq, and other providers with
 * editable Base URL, API Key (with secure VisualTransformation), and Model ID fields,
 * saving updates via [ProviderConfigurationManager].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderConfigScreen(
    configManager: ProviderConfigurationManager = ProviderConfigurationManager(LocalContext.current)
) {
    val coroutineScope = rememberCoroutineScope()

    var persistedProviders by remember { mutableStateOf<List<ProviderSettings>>(emptyList()) }
    var activeProviderId by remember { mutableStateOf("gemini_compat") }

    // Collect persisted provider settings from DataStore
    LaunchedEffect(Unit) {
        configManager.providersFlow.collect { list ->
            persistedProviders = list
            activeProviderId = list.find { it.isActive }?.id ?: "gemini_compat"
        }
    }

    // Target preset cards to present: Gemini, OpenRouter, Groq, DeepSeek, OpenAI
    val featuredPresets = remember {
        listOf(
            ProviderPresets.PRESETS.first { it.id == "gemini_compat" },
            ProviderPresets.PRESETS.first { it.id == "openrouter" },
            ProviderPresets.PRESETS.first { it.id == "groq" },
            ProviderPresets.PRESETS.first { it.id == "deepseek" },
            ProviderPresets.PRESETS.first { it.id == "openai" }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Provider Configuration") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Configure Model AI Providers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("provider_cards_list")
            ) {
                items(featuredPresets, key = { it.id }) { preset ->
                    // Merge persisted settings if available
                    val persisted = persistedProviders.find { it.id == preset.id }
                    val currentBaseUrl = persisted?.baseUrl ?: preset.baseUrl
                    val currentApiKey = persisted?.apiKey ?: preset.apiKey
                    val currentModelId = persisted?.defaultModel ?: preset.defaultModel
                    val isActive = preset.id == activeProviderId

                    ProviderPresetCard(
                        preset = preset,
                        initialBaseUrl = currentBaseUrl,
                        initialApiKey = currentApiKey,
                        initialModelId = currentModelId,
                        isActive = isActive,
                        onSave = { updatedSettings ->
                            coroutineScope.launch {
                                configManager.saveProvider(updatedSettings)
                            }
                        },
                        onSetActive = { providerId ->
                            coroutineScope.launch {
                                configManager.setActiveProvider(providerId)
                            }
                        },
                        onTestConnection = { baseUrl, apiKey, model ->
                            configManager.testConnection(baseUrl, apiKey, model)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderPresetCard(
    preset: ProviderSettings,
    initialBaseUrl: String,
    initialApiKey: String,
    initialModelId: String,
    isActive: Boolean,
    onSave: (ProviderSettings) -> Unit,
    onSetActive: (String) -> Unit,
    onTestConnection: suspend (String, String, String) -> Result<String>
) {
    val coroutineScope = rememberCoroutineScope()

    var baseUrl by remember(initialBaseUrl) { mutableStateOf(initialBaseUrl) }
    var apiKey by remember(initialApiKey) { mutableStateOf(initialApiKey) }
    var modelId by remember(initialModelId) { mutableStateOf(initialModelId) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isSavedSuccessfully by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_provider_${preset.id}"),
        colors = if (isActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Provider Cloud",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("Active")
                        }
                    }
                }

                if (!isActive) {
                    TextButton(
                        onClick = { onSetActive(preset.id) },
                        modifier = Modifier.testTag("set_active_${preset.id}")
                    ) {
                        Text("Set Active")
                    }
                }
            }

            // Editable Base URL TextField
            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it
                    isSavedSuccessfully = false
                },
                label = { Text("Base URL") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_base_url_${preset.id}"),
                singleLine = true
            )

            // Editable API Key TextField with Security VisualTransformation
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    isSavedSuccessfully = false
                },
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_api_key_${preset.id}"),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = "API Key Icon")
                },
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        modifier = Modifier.testTag("toggle_key_visibility_${preset.id}")
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide Key" else "Show Key"
                        )
                    }
                }
            )

            // Editable Model ID TextField
            OutlinedTextField(
                value = modelId,
                onValueChange = {
                    modelId = it
                    isSavedSuccessfully = false
                },
                label = { Text("Model ID") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_model_id_${preset.id}"),
                singleLine = true
            )

            // Actions Row: Test Connection & Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        testResult = null
                        coroutineScope.launch {
                            val res = onTestConnection(baseUrl.trim(), apiKey.trim(), modelId.trim())
                            isTesting = false
                            testResult = if (res.isSuccess) {
                                "✓ " + res.getOrNull()
                            } else {
                                "✗ " + (res.exceptionOrNull()?.message ?: "Test failed")
                            }
                        }
                    },
                    enabled = !isTesting && baseUrl.isNotBlank(),
                    modifier = Modifier.testTag("btn_test_${preset.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = "Test Network",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isTesting) "Testing..." else "Test Connection")
                }

                Button(
                    onClick = {
                        val updated = ProviderSettings(
                            id = preset.id,
                            name = preset.name,
                            baseUrl = baseUrl.trim(),
                            apiKey = apiKey.trim(),
                            defaultModel = modelId.trim(),
                            isActive = isActive
                        )
                        onSave(updated)
                        isSavedSuccessfully = true
                    },
                    enabled = baseUrl.isNotBlank() && modelId.isNotBlank(),
                    modifier = Modifier.testTag("btn_save_${preset.id}")
                ) {
                    Icon(
                        imageVector = if (isSavedSuccessfully) Icons.Default.Check else Icons.Default.Save,
                        contentDescription = "Save Settings",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isSavedSuccessfully) "Saved" else "Save")
                }
            }

            // Connection Diagnostic Feedback Message
            testResult?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (msg.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("test_result_${preset.id}")
                )
            }
        }
    }
}

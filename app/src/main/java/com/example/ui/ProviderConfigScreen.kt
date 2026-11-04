package com.example.ui

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
 * Jetpack Compose screen at 'app/src/main/java/com/example/ui/ProviderConfigScreen.kt'.
 * Displays a Scaffold with a LazyColumn containing input fields for Name, Base URL, API Key,
 * and Model ID for each provider with hardware-secured/masked API key viewing and Save persistence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderConfigScreen(
    configManager: ProviderConfigurationManager = ProviderConfigurationManager(LocalContext.current)
) {
    val coroutineScope = rememberCoroutineScope()

    var persistedProviders by remember { mutableStateOf<List<ProviderSettings>>(emptyList()) }
    var activeProviderId by remember { mutableStateOf("gemini_compat") }

    LaunchedEffect(Unit) {
        configManager.providersFlow.collect { list ->
            persistedProviders = list
            activeProviderId = list.find { it.isActive }?.id ?: "gemini_compat"
        }
    }

    val featuredPresets = remember {
        listOf(
            ProviderPresets.PRESETS.first { it.id == "gemini_compat" },
            ProviderPresets.PRESETS.first { it.id == "openrouter" },
            ProviderPresets.PRESETS.first { it.id == "groq" },
            ProviderPresets.PRESETS.first { it.id == "deepseek" },
            ProviderPresets.PRESETS.first { it.id == "openai" }
        )
    }

    val nameStates = remember { mutableStateMapOf<String, String>() }
    val baseUrlStates = remember { mutableStateMapOf<String, String>() }
    val apiKeyStates = remember { mutableStateMapOf<String, String>() }
    val modelStates = remember { mutableStateMapOf<String, String>() }
    val visibilityStates = remember { mutableStateMapOf<String, Boolean>() }
    val testStatusStates = remember { mutableStateMapOf<String, String>() }
    val testingStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(persistedProviders) {
        featuredPresets.forEach { preset ->
            val existing = persistedProviders.find { it.id == preset.id }
            nameStates[preset.id] = existing?.name ?: preset.name
            baseUrlStates[preset.id] = existing?.baseUrl ?: preset.baseUrl
            apiKeyStates[preset.id] = existing?.apiKey ?: preset.apiKey
            modelStates[preset.id] = existing?.defaultModel ?: preset.defaultModel
            visibilityStates[preset.id] = false
        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Configure Model Providers & API Keys",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Credentials are securely encrypted in Android Keystore and synchronized with DataStore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            items(featuredPresets) { preset ->
                val id = preset.id
                val isSelected = (activeProviderId == id)
                val isVisible = visibilityStates[id] ?: false
                val isTesting = testingStates[id] ?: false
                val testResult = testStatusStates[id]

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isSelected) {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    } else {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = nameStates[id] ?: preset.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("Active") }
                                }
                            }

                            if (!isSelected) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            configManager.setActiveProvider(id)
                                        }
                                    }
                                ) {
                                    Text("Set Active")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = nameStates[id] ?: "",
                            onValueChange = { nameStates[id] = it },
                            label = { Text("Provider Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = baseUrlStates[id] ?: "",
                            onValueChange = { baseUrlStates[id] = it },
                            label = { Text("Base URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = apiKeyStates[id] ?: "",
                            onValueChange = { apiKeyStates[id] = it },
                            label = { Text("API Key") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input_$id"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { visibilityStates[id] = !isVisible }) {
                                    Icon(
                                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isVisible) "Hide API Key" else "Show API Key"
                                    )
                                }
                            }
                        )

                        OutlinedTextField(
                            value = modelStates[id] ?: "",
                            onValueChange = { modelStates[id] = it },
                            label = { Text("Model ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    testingStates[id] = true
                                    testStatusStates.remove(id)
                                    coroutineScope.launch {
                                        val result = configManager.testConnection(
                                            baseUrl = baseUrlStates[id]?.trim() ?: "",
                                            apiKey = apiKeyStates[id]?.trim() ?: "",
                                            model = modelStates[id]?.trim() ?: ""
                                        )
                                        testingStates[id] = false
                                        testStatusStates[id] = if (result.isSuccess) {
                                            "✓ " + result.getOrNull()
                                        } else {
                                            "✗ " + (result.exceptionOrNull()?.message ?: "Test failed")
                                        }
                                    }
                                },
                                enabled = !isTesting
                            ) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isTesting) "Testing..." else "Test Connection")
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val settings = ProviderSettings(
                                            id = id,
                                            name = nameStates[id]?.trim() ?: preset.name,
                                            baseUrl = baseUrlStates[id]?.trim() ?: preset.baseUrl,
                                            apiKey = apiKeyStates[id]?.trim() ?: "",
                                            defaultModel = modelStates[id]?.trim() ?: preset.defaultModel,
                                            isActive = isSelected
                                        )
                                        configManager.saveProvider(settings)
                                        testStatusStates[id] = "✓ Saved Successfully!"
                                    }
                                },
                                modifier = Modifier.testTag("save_button_$id")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
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
                }
            }
        }
    }
}

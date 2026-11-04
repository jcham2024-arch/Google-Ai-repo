package com.example.ui.agents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.model.AgentCard
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentListScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).apexDao() }
    val coroutineScope = rememberCoroutineScope()

    var agents by remember { mutableStateOf(listOf<AgentCard>()) }
    var activeAgentId by remember { mutableStateOf("agent_master") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newAgentName by remember { mutableStateOf("") }
    var newAgentPrompt by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        dao.getAllAgents().collect { list ->
            agents = list.ifEmpty {
                val defaultCard = AgentCard(
                    id = "agent_master",
                    name = "Master Agent",
                    description = "Orchestrates sub-agent swarms and governs task and tool execution.",
                    systemPrompt = "You are Master Agent, an advanced general-purpose AI assistant and orchestrator.",
                    preferredProvider = "Google Gemini",
                    preferredModel = "gemini-3.8-flash",
                    isMasterAgent = true
                )
                dao.insertAgent(defaultCard)
                listOf(defaultCard)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Swarm Registry") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Agent Card")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(agents) { agent ->
                val isActive = agent.id == activeAgentId
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isActive) {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    } else {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SmartToy, contentDescription = "Agent", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = agent.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (agent.isMasterAgent) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge { Text("Master") }
                                }
                            }

                            if (isActive) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                TextButton(onClick = { activeAgentId = agent.id }) {
                                    Text("Activate")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = agent.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "System Prompt: ${agent.systemPrompt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Model: ${agent.preferredModel} (${agent.preferredProvider})", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Specialized Agent Card") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newAgentName,
                        onValueChange = { newAgentName = it },
                        label = { Text("Agent Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newAgentPrompt,
                        onValueChange = { newAgentPrompt = it },
                        label = { Text("System Prompt Instructions") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newAgentName.isNotBlank()) {
                        coroutineScope.launch {
                            val newAgent = AgentCard(
                                id = "agent_" + System.currentTimeMillis(),
                                name = newAgentName.trim(),
                                description = "Custom specialized sub-agent card",
                                systemPrompt = newAgentPrompt.trim(),
                                preferredProvider = "Google Gemini",
                                preferredModel = "gemini-3.8-flash"
                            )
                            dao.insertAgent(newAgent)
                            newAgentName = ""
                            newAgentPrompt = ""
                            showCreateDialog = false
                        }
                    }
                }) {
                    Text("Save Agent Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

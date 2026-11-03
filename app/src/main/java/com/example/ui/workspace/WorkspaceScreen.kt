package com.example.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.model.WorkspaceFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).apexDao() }

    var files by remember { mutableStateOf(listOf<WorkspaceFile>()) }
    var selectedFile by remember { mutableStateOf<WorkspaceFile?>(null) }

    LaunchedEffect(Unit) {
        dao.getAllWorkspaceFiles().collect { list ->
            files = list.ifEmpty {
                listOf(
                    WorkspaceFile(
                        path = "workspace/README.md",
                        name = "README.md",
                        content = "# Apex Agentic Master Content\nWorkspace initialized successfully. Master Narrator governs file creation and self-improvement."
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspace & Self-Improved Code") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    Card(
                        onClick = { selectedFile = file },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "File")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = file.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            VerticalDivider()

            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                if (selectedFile != null) {
                    Column {
                        Text(text = selectedFile!!.path, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = selectedFile!!.content,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Select a workspace file to view code/content",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

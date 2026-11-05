package com.example.ui.workspace

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.model.WorkspaceFile
import com.example.domain.engine.FileGovernanceEngine
import com.example.domain.engine.SandboxStats
import com.example.domain.engine.WorkspaceEvent
import com.example.domain.engine.WorkspaceExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).apexDao() }
    val fileGovernanceEngine = remember { FileGovernanceEngine(context) }
    val workspaceExporter = remember { WorkspaceExporter(context) }
    val coroutineScope = rememberCoroutineScope()

    var files by remember { mutableStateOf(listOf<WorkspaceFile>()) }
    var selectedFile by remember { mutableStateOf<WorkspaceFile?>(null) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var sandboxStats by remember { mutableStateOf<SandboxStats?>(null) }
    var showVersionHistory by remember { mutableStateOf(false) }
    var selectedFileVersions by remember { mutableStateOf<com.example.data.model.WorkspaceFileVersion?>(null) }

    LaunchedEffect(Unit) {
        dao.getAllWorkspaceFiles().collect { list ->
            files = if (list.isEmpty()) {
                listOf(
                    WorkspaceFile(
                        path = "workspace/README.md",
                        name = "README.md",
                        content = "# Apex Agentic Master Content\nWorkspace initialized successfully. Master Agent governs file creation and self-improvement."
                    )
                )
            } else {
                list.toList()
            }
        }
    }

    // Load sandbox stats
    LaunchedEffect(fileGovernanceEngine) {
        sandboxStats = fileGovernanceEngine.getSandboxStats()
    }

    // Observe workspace events for live sync
    LaunchedEffect(fileGovernanceEngine) {
        fileGovernanceEngine.workspaceEvents.collect { event ->
            when (event) {
                is WorkspaceEvent.FileCreated,
                is WorkspaceEvent.FileModified -> {
                    // If the currently selected file was modified, update its content
                    if (event is WorkspaceEvent.FileModified && selectedFile?.path == event.path) {
                        selectedFile = selectedFile?.copy(content = event.content)
                    }
                    // Refresh stats
                    sandboxStats = fileGovernanceEngine.getSandboxStats()
                }
                is WorkspaceEvent.FileDeleted -> {
                    // Clear selection if the deleted file was selected
                    if (selectedFile?.path == event.path) {
                        selectedFile = null
                    }
                    // Refresh stats
                    sandboxStats = fileGovernanceEngine.getSandboxStats()
                }
                null -> {}
            }
        }
    }

    fun exportWorkspace() {
        if (isExporting) return
        isExporting = true
        exportStatus = "Exporting..."
        coroutineScope.launch {
            val result = workspaceExporter.exportWorkspace()
            isExporting = false
            result.onSuccess { uri ->
                exportStatus = "Exported to Downloads: ${uri.lastPathSegment}"
            }.onFailure { e ->
                exportStatus = "Export failed: ${e.message}"
            }
        }
    }
    
    fun loadVersionHistory(filePath: String) {
        showVersionHistory = true
        coroutineScope.launch {
            val versions = fileGovernanceEngine.getFileVersionHistory(filePath)
            if (versions.isNotEmpty()) {
                selectedFileVersions = versions.first()
            }
        }
    }
    
    fun restoreVersion(filePath: String, version: Int) {
        coroutineScope.launch {
            val success = fileGovernanceEngine.restoreFileVersion(filePath, version)
            if (success) {
                exportStatus = "Restored version $version"
            } else {
                exportStatus = "Failed to restore version $version"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspace & Self-Improved Code") },
                actions = {
                    // Sandbox stats indicator
                    sandboxStats?.let { stats ->
                        IconButton(onClick = { /* could show detailed dialog */ }) {
                            Icon(Icons.Default.Storage, contentDescription = "Sandbox Storage", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(
                        onClick = { exportWorkspace() },
                        enabled = !isExporting,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export Workspace")
                        }
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
            // Sandbox stats banner
            sandboxStats?.let { stats ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Sandbox Usage",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "${formatBytes(stats.totalSizeBytes)} / ${formatBytes(stats.maxTotalSizeBytes)} (${String.format("%.1f", stats.usagePercent)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (stats.usagePercent > 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${stats.fileCount} / ${stats.maxFileCount} files (${String.format("%.1f", stats.countUsagePercent)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (stats.countUsagePercent > 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Export status banner
            exportStatus?.let { status ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (status.contains("failed", ignoreCase = true))
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.contains("failed", ignoreCase = true))
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
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
}

@Composable
fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))} MB"
        else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}

package com.example.ui.graph

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Description
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import com.example.data.provider.ProviderPresets
import com.example.data.provider.ProviderSettings
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class NodePort(
    val id: String,
    val name: String,
    val dataType: String,
    val isInput: Boolean
)

enum class SwarmTaskStatus {
    RUNNING,
    STALLED,
    STANDBY
}

data class SwarmDelegationTask(
    val id: String,
    val title: String,
    val targetNode: String,
    var status: SwarmTaskStatus,
    val progressPercent: Float = 0.5f,
    val detail: String = "",
    val timestamp: String = "Just now"
)

data class NodeSettingsProfile(
    val providerId: String = "gemini_compat",
    val providerName: String = "Google Gemini",
    val model: String = "gemini-2.5-flash",
    val agentType: String = "Orchestrator",
    val instructions: String = "Execute tasks and coordinate node workflow in the multi-agent pipeline.",
    val tools: List<String> = listOf("Web Search", "File System", "Code Linter"),
    val temperature: Float = 0.7f,
    val documentation: String = "# Node Documentation\n\nAdd markdown-based notes, explainers, or architecture notes for this node."
)

data class GraphNodeData(
    val id: String,
    var position: Offset,
    val name: String,
    val role: String,
    val status: String,
    val toolsCount: Int,
    val permissionLevel: String,
    val inputs: List<NodePort>,
    val outputs: List<NodePort>,
    var swarmStatus: SwarmTaskStatus = SwarmTaskStatus.RUNNING,
    val delegationTasks: MutableList<SwarmDelegationTask> = mutableListOf(),
    var settings: NodeSettingsProfile = NodeSettingsProfile()
)

data class PipelineEdge(
    val id: String,
    val fromNodeId: String,
    val fromPortId: String,
    val toNodeId: String,
    val toPortId: String,
    val label: String,
    var isActive: Boolean = true
)

data class ValidationResult(
    val isValid: Boolean,
    val reason: String
)

data class NodeTemplateItem(
    val id: String,
    val name: String,
    val role: String,
    val toolsCount: Int,
    val permissionLevel: String,
    val inputs: List<NodePort>,
    val outputs: List<NodePort>
)

data class SerializedNode(
    val id: String,
    val posX: Float,
    val posY: Float,
    val name: String,
    val role: String,
    val status: String,
    val toolsCount: Int,
    val permissionLevel: String,
    val inputs: List<NodePort>,
    val outputs: List<NodePort>,
    val settings: NodeSettingsProfile = NodeSettingsProfile()
)

data class SerializedPipelineGraph(
    val version: Int = 1,
    val name: String = "Swarm Pipeline",
    val createdAt: Long = System.currentTimeMillis(),
    val nodes: List<SerializedNode>,
    val pipelines: List<PipelineEdge>
)

fun validateEdgeTypeCompatibility(
    edge: PipelineEdge,
    nodes: List<GraphNodeData>
): ValidationResult {
    val fromNode = nodes.find { it.id == edge.fromNodeId } ?: return ValidationResult(false, "Source node missing")
    val toNode = nodes.find { it.id == edge.toNodeId } ?: return ValidationResult(false, "Target node missing")

    val fromPort = fromNode.outputs.find { it.id == edge.fromPortId }
    val toPort = toNode.inputs.find { it.id == edge.toPortId }

    if (fromPort == null || toPort == null) {
        return ValidationResult(false, "Mismatched port endpoints")
    }

    val srcType = fromPort.dataType
    val targetType = toPort.dataType

    if (srcType == "Text" || targetType == "Text" || srcType == "Any" || targetType == "Any") {
        return ValidationResult(true, "Compatible (Generic payload)")
    }

    if (srcType.equals(targetType, ignoreCase = true)) {
        return ValidationResult(true, "Compatible ($srcType)")
    }

    return ValidationResult(false, "Type mismatch: Source produces '$srcType' but Target expects '$targetType'")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen() {
    var isExecutionView by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSwarmPaused by remember { mutableStateOf(false) }
    var selectedNodeId by remember { mutableStateOf<String?>("node_orchestrator") }
    var activeSettingsNodeId by remember { mutableStateOf<String?>(null) }
    val availableProviders = remember { ProviderPresets.PRESETS }

    // Sidebar & Dialog states
    var showTemplatesGallerySidebar by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showValidationErrorModal by remember { mutableStateOf<String?>(null) }
    var importJsonInput by remember { mutableStateOf("") }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Canvas Pan & Zoom
    var panOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // Interactive Connection
    var activeSourcePort by remember { mutableStateOf<Pair<String, String>?>(null) }
    var draftTargetPos by remember { mutableStateOf<Offset?>(null) }

    // Saved Node Templates Gallery
    val savedTemplates = remember {
        mutableStateListOf(
            NodeTemplateItem(
                id = "tmpl_orchestrator",
                name = "Orchestrator Agent",
                role = "Master Coordinator",
                toolsCount = 10,
                permissionLevel = "L1-L4 Permissive",
                inputs = listOf(NodePort("in_g", "Goal Prompt", "Text", true)),
                outputs = listOf(
                    NodePort("out_t", "Delegation Task", "TaskPayload", false),
                    NodePort("out_s", "Scratchpad Pointer", "MemoryRef", false)
                )
            ),
            NodeTemplateItem(
                id = "tmpl_ui_builder",
                name = "Compose UI Builder",
                role = "Jetpack Compose Engineer",
                toolsCount = 5,
                permissionLevel = "L3 Read-Write",
                inputs = listOf(NodePort("in_ui", "UI Spec", "TaskPayload", true)),
                outputs = listOf(NodePort("out_ui", "Compose Code", "FileDiff", false))
            ),
            NodeTemplateItem(
                id = "tmpl_db_agent",
                name = "Database Schema Agent",
                role = "Room / Firestore Architect",
                toolsCount = 3,
                permissionLevel = "L3 Read-Write",
                inputs = listOf(NodePort("in_db", "DB Spec", "TaskPayload", true)),
                outputs = listOf(NodePort("out_db", "Entity Schema", "FileDiff", false))
            ),
            NodeTemplateItem(
                id = "tmpl_security",
                name = "Security Interlock Gate",
                role = "Safety & Policy Auditor",
                toolsCount = 2,
                permissionLevel = "L4 Approval Interlock",
                inputs = listOf(NodePort("in_sec", "Action Request", "ToolCallPayload", true)),
                outputs = listOf(NodePort("out_sec", "Approval Token", "SecurityToken", false))
            ),
            NodeTemplateItem(
                id = "tmpl_linter",
                name = "Static Linter Agent",
                role = "Syntax & Rule Checker",
                toolsCount = 2,
                permissionLevel = "L2 Read-Only",
                inputs = listOf(NodePort("in_lint", "Source Code", "TaskPayload", true)),
                outputs = listOf(NodePort("out_lint", "Issues List", "IssuesReport", false))
            )
        )
    }

    // Initial Graph
    val nodes = remember {
        mutableStateListOf(
            GraphNodeData(
                id = "node_orchestrator",
                position = Offset(60f, 100f),
                name = "Orchestrator Agent",
                role = "Master Swarm Coordinator",
                status = "RUNNING",
                toolsCount = 10,
                permissionLevel = "L1-L4 Permissive",
                inputs = listOf(
                    NodePort("in_goal", "Goal Prompt", "Text", true),
                    NodePort("in_user", "User Input", "Signal", true)
                ),
                outputs = listOf(
                    NodePort("out_task", "Delegation Task", "TaskPayload", false),
                    NodePort("out_scratch", "Scratchpad Pointer", "MemoryRef", false)
                ),
                swarmStatus = SwarmTaskStatus.RUNNING,
                delegationTasks = mutableListOf(
                    SwarmDelegationTask("t1", "Auto-delegate UI Layout Spec", "Builder Agent", SwarmTaskStatus.RUNNING, 0.75f, "Writing Jetpack Compose layout canvas", "5s ago"),
                    SwarmDelegationTask("t2", "Auto-delegate Security Audit", "Security Interlock", SwarmTaskStatus.STANDBY, 0.0f, "Awaiting builder code completion", "12s ago")
                )
            ),
            GraphNodeData(
                id = "node_builder",
                position = Offset(440f, 60f),
                name = "Builder Agent",
                role = "Android Code Generator",
                status = "RUNNING",
                toolsCount = 4,
                permissionLevel = "L3 Read-Write",
                inputs = listOf(
                    NodePort("in_task_b", "Task Input", "TaskPayload", true),
                    NodePort("in_ctx_b", "Context Ref", "MemoryRef", true)
                ),
                outputs = listOf(
                    NodePort("out_diff", "Code Diff", "FileDiff", false),
                    NodePort("out_status_b", "Build Status", "StatusSignal", false)
                ),
                swarmStatus = SwarmTaskStatus.RUNNING,
                delegationTasks = mutableListOf(
                    SwarmDelegationTask("tb1", "Generate Compose Canvas Nodes", "Local Worker", SwarmTaskStatus.RUNNING, 0.88f, "Applying gesture matrix transforms", "2s ago"),
                    SwarmDelegationTask("tb2", "AST rule validation", "Linter Agent", SwarmTaskStatus.STANDBY, 0.10f, "Queued in swarm executor", "8s ago")
                )
            ),
            GraphNodeData(
                id = "node_security",
                position = Offset(440f, 320f),
                name = "Security Interlock",
                role = "Policy & Safety Gate",
                status = "DONE",
                toolsCount = 2,
                permissionLevel = "L4 Approval Interlock",
                inputs = listOf(
                    NodePort("in_action", "Action Request", "ToolCallPayload", true)
                ),
                outputs = listOf(
                    NodePort("out_approval", "Approval Token", "SecurityToken", false)
                ),
                swarmStatus = SwarmTaskStatus.STALLED,
                delegationTasks = mutableListOf(
                    SwarmDelegationTask("ts1", "L4 Policy Safety Audit Gate", "Security Gate", SwarmTaskStatus.STALLED, 0.20f, "Delegation stalled: Awaiting human override approval token", "25s ago")
                )
            )
        )
    }

    val pipelines = remember {
        mutableStateListOf(
            PipelineEdge(
                id = "pipe_1",
                fromNodeId = "node_orchestrator",
                fromPortId = "out_task",
                toNodeId = "node_builder",
                toPortId = "in_task_b",
                label = "delegate_task",
                isActive = true
            ),
            PipelineEdge(
                id = "pipe_2",
                fromNodeId = "node_orchestrator",
                fromPortId = "out_scratch",
                toNodeId = "node_builder",
                toPortId = "in_ctx_b",
                label = "scratchpad_ref",
                isActive = true
            )
        )
    }

    val invalidConnections = remember(nodes, pipelines.toList()) {
        pipelines.map { edge -> edge to validateEdgeTypeCompatibility(edge, nodes) }
            .filter { !it.second.isValid }
    }

    val density = LocalDensity.current

    val infiniteTransition = rememberInfiniteTransition(label = "pipeline_flow")
    val phaseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    fun loadGraphFromSerialized(serialized: SerializedPipelineGraph) {
        nodes.clear()
        pipelines.clear()
        serialized.nodes.forEach { sNode ->
            nodes.add(
                GraphNodeData(
                    id = sNode.id,
                    position = Offset(sNode.posX, sNode.posY),
                    name = sNode.name,
                    role = sNode.role,
                    status = sNode.status,
                    toolsCount = sNode.toolsCount,
                    permissionLevel = sNode.permissionLevel,
                    inputs = sNode.inputs,
                    outputs = sNode.outputs,
                    settings = sNode.settings
                )
            )
        }
        pipelines.addAll(serialized.pipelines)
    }

    fun exportCurrentGraphJson(): String {
        val serializedNodes = nodes.map { n ->
            SerializedNode(
                id = n.id,
                posX = n.position.x,
                posY = n.position.y,
                name = n.name,
                role = n.role,
                status = n.status,
                toolsCount = n.toolsCount,
                permissionLevel = n.permissionLevel,
                inputs = n.inputs,
                outputs = n.outputs,
                settings = n.settings
            )
        }
        val serializedGraph = SerializedPipelineGraph(
            version = 1,
            name = "Exported Swarm Pipeline",
            createdAt = System.currentTimeMillis(),
            nodes = serializedNodes,
            pipelines = pipelines.toList()
        )
        return GsonBuilder().setPrettyPrinting().create().toJson(serializedGraph)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Node Graph Canvas")
                    }
                },
                actions = {
                    // Templates Gallery Sidebar Toggle
                    IconButton(onClick = { showTemplatesGallerySidebar = !showTemplatesGallerySidebar }) {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = "Templates Gallery",
                            tint = if (showTemplatesGallerySidebar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Export Graph JSON", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = {
                        importJsonInput = ""
                        importErrorMessage = null
                        showImportDialog = true
                    }) {
                        Icon(Icons.Default.Upload, contentDescription = "Import Pipeline Template", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = {
                        alignNodesToGrid(nodes, pipelines)
                        fitCanvasToScreen(nodes, 900f, 600f) { scale, pan ->
                            zoomScale = scale
                            panOffset = pan
                        }
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Canvas cleaned up: Grid aligned & Fit to screen")
                        }
                    }) {
                        Icon(Icons.Default.GridView, contentDescription = "Cleanup to Grid", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = {
                        fitCanvasToScreen(nodes, 900f, 600f) { scale, pan ->
                            zoomScale = scale
                            panOffset = pan
                        }
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Fit to screen: ${(zoomScale * 100).roundToInt()}%")
                        }
                    }) {
                        Icon(Icons.Default.CenterFocusWeak, contentDescription = "Fit to Screen", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = {
                        runForceDirectedAutoLayout(nodes, pipelines)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Auto layout applied")
                        }
                    }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Auto Layout", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { isSwarmPaused = !isSwarmPaused }) {
                        Icon(
                            imageVector = if (isSwarmPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isSwarmPaused) "Resume" else "Pause",
                            tint = if (isSwarmPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
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
            // View Mode Toolbar
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = isExecutionView,
                            onClick = { isExecutionView = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Execution Tree", style = MaterialTheme.typography.labelMedium)
                        }
                        SegmentedButton(
                            selected = !isExecutionView,
                            onClick = { isExecutionView = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Design Topology", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            label = { Text("Canvas Graph") }
                        )
                        FilterChip(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            label = { Text("Scratchpad") }
                        )
                    }
                }
            }

            // Live Pipeline Validation Status Bar
            if (invalidConnections.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showValidationErrorModal = invalidConnections.first().second.reason
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TYPE VALIDATION ERROR: ${invalidConnections.size} invalid connection(s) highlighted in RED (${invalidConnections.first().second.reason})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (pipelines.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VALIDATION PASSED: All ${pipelines.size} connection port data types are compatible.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Connection Active Banner
            if (activeSourcePort != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CONNECTING PIPELINE: Tap target Input Port on another node card",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = {
                            activeSourcePort = null
                            draftTargetPos = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Connection")
                        }
                    }
                }
            }

            if (isSwarmPaused) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SWARM HALTED BY GLOBAL KILL SWITCH",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // Node Canvas
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    zoomScale = (zoomScale * zoom).coerceIn(0.4f, 2.5f)
                                    panOffset += pan
                                }
                            }
                    ) {
                        val canvasWidthPx = constraints.maxWidth.toFloat()
                        val canvasHeightPx = constraints.maxHeight.toFloat()
                        val outlineColor = MaterialTheme.colorScheme.outlineVariant
                        val activePipeColor = MaterialTheme.colorScheme.tertiary
                        val inactivePipeColor = MaterialTheme.colorScheme.outline
                        val errorPipeColor = MaterialTheme.colorScheme.error
                        val draftPipeColor = MaterialTheme.colorScheme.primary

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomScale
                                    scaleY = zoomScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                        ) {
                            // Grid & Pipelines
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val gridSpacing = 40f
                                var x = 0f
                                while (x < size.width * 2f) {
                                    var y = 0f
                                    while (y < size.height * 2f) {
                                        drawCircle(
                                            color = outlineColor.copy(alpha = 0.35f),
                                            radius = 1.5f,
                                            center = Offset(x, y)
                                        )
                                        y += gridSpacing
                                    }
                                    x += gridSpacing
                                }

                                val cardWidthPx = with(density) { 130.dp.toPx() }
                                val cardHeaderHeightPx = with(density) { 45.dp.toPx() }

                                pipelines.forEach { edge ->
                                    val fromNode = nodes.find { it.id == edge.fromNodeId }
                                    val toNode = nodes.find { it.id == edge.toNodeId }

                                    if (fromNode != null && toNode != null) {
                                        val outPortIndex = fromNode.outputs.indexOfFirst { it.id == edge.fromPortId }.coerceAtLeast(0)
                                        val fromX = fromNode.position.x + cardWidthPx
                                        val fromY = fromNode.position.y + cardHeaderHeightPx + (outPortIndex * 36f) + 30f

                                        val inPortIndex = toNode.inputs.indexOfFirst { it.id == edge.toPortId }.coerceAtLeast(0)
                                        val toX = toNode.position.x
                                        val toY = toNode.position.y + cardHeaderHeightPx + (inPortIndex * 36f) + 30f

                                        val dx = (toX - fromX) / 2f
                                        val path = Path().apply {
                                            moveTo(fromX, fromY)
                                            cubicTo(
                                                fromX + dx, fromY,
                                                toX - dx, toY,
                                                toX, toY
                                            )
                                        }

                                        val validation = validateEdgeTypeCompatibility(edge, nodes)
                                        val isInvalid = !validation.isValid

                                        val color = if (isInvalid) errorPipeColor else if (edge.isActive) activePipeColor else inactivePipeColor
                                        val strokeWidth = if (isInvalid) 4.5f else if (edge.isActive) 3.5f else 2f
                                        val pathEffect = if (isInvalid) {
                                            PathEffect.dashPathEffect(floatArrayOf(10f, 10f), phaseAnim)
                                        } else if (edge.isActive) {
                                            PathEffect.dashPathEffect(floatArrayOf(20f, 15f), phaseAnim)
                                        } else null

                                        drawPath(
                                            path = path,
                                            color = color,
                                            style = Stroke(width = strokeWidth, pathEffect = pathEffect)
                                        )

                                        drawCircle(color = color, radius = if (isInvalid) 8f else 6f, center = Offset(fromX, fromY))
                                        drawCircle(color = color, radius = if (isInvalid) 8f else 6f, center = Offset(toX, toY))
                                    }
                                }

                                activeSourcePort?.let { (srcNodeId, srcPortId) ->
                                    val srcNode = nodes.find { it.id == srcNodeId }
                                    if (srcNode != null) {
                                        val outPortIndex = srcNode.outputs.indexOfFirst { it.id == srcPortId }.coerceAtLeast(0)
                                        val fromX = srcNode.position.x + cardWidthPx
                                        val fromY = srcNode.position.y + cardHeaderHeightPx + (outPortIndex * 36f) + 30f

                                        val target = draftTargetPos ?: Offset(fromX + 120f, fromY + 40f)
                                        val dx = (target.x - fromX) / 2f

                                        val draftPath = Path().apply {
                                            moveTo(fromX, fromY)
                                            cubicTo(
                                                fromX + dx, fromY,
                                                target.x - dx, target.y,
                                                target.x, target.y
                                            )
                                        }

                                        drawPath(
                                            path = draftPath,
                                            color = draftPipeColor,
                                            style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), phaseAnim))
                                        )
                                        drawCircle(color = draftPipeColor, radius = 8f, center = Offset(fromX, fromY))
                                        drawCircle(color = draftPipeColor, radius = 8f, center = target)
                                    }
                                }
                            }

                            // Node Cards
                            nodes.forEach { node ->
                                val isSelected = selectedNodeId == node.id
                                val cardOffsetX = node.position.x.roundToInt()
                                val cardOffsetY = node.position.y.roundToInt()

                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(cardOffsetX, cardOffsetY) }
                                        .width(130.dp)
                                        .pointerInput(node.id) {
                                            detectTransformGestures { _, pan, _, _ ->
                                                node.position = Offset(
                                                    (node.position.x + pan.x / zoomScale).coerceAtLeast(10f),
                                                    (node.position.y + pan.y / zoomScale).coerceAtLeast(10f)
                                                )
                                            }
                                        }
                                ) {
                                    InteractiveCanvasNodeCard(
                                        node = node,
                                        isSelected = isSelected,
                                        activeSourcePort = activeSourcePort,
                                        onSelect = { selectedNodeId = node.id },
                                        onOutputPortClick = { port ->
                                            activeSourcePort = Pair(node.id, port.id)
                                        },
                                        onInputPortClick = { port ->
                                            activeSourcePort?.let { (srcNodeId, srcPortId) ->
                                                if (srcNodeId != node.id) {
                                                    val newEdge = PipelineEdge(
                                                        id = "pipe_${System.currentTimeMillis()}",
                                                        fromNodeId = srcNodeId,
                                                        fromPortId = srcPortId,
                                                        toNodeId = node.id,
                                                        toPortId = port.id,
                                                        label = "${srcPortId}_to_${port.id}",
                                                        isActive = true
                                                    )
                                                    pipelines.add(newEdge)
                                                    activeSourcePort = null
                                                    draftTargetPos = null
                                                }
                                            }
                                        },
                                        onDeleteNode = {
                                            nodes.remove(node)
                                            pipelines.removeAll { it.fromNodeId == node.id || it.toNodeId == node.id }
                                        },
                                        onDuplicateNode = {
                                            val newId = "node_${System.currentTimeMillis()}"
                                            val dupInputs = node.inputs.mapIndexed { idx, port ->
                                                port.copy(id = "in_${newId}_$idx")
                                            }
                                            val dupOutputs = node.outputs.mapIndexed { idx, port ->
                                                port.copy(id = "out_${newId}_$idx")
                                            }
                                            val duplicateNode = GraphNodeData(
                                                id = newId,
                                                position = Offset(node.position.x + 35f, node.position.y + 35f),
                                                name = "${node.name} (Copy)",
                                                role = node.role,
                                                status = "IDLE",
                                                toolsCount = node.toolsCount,
                                                permissionLevel = node.permissionLevel,
                                                inputs = dupInputs,
                                                outputs = dupOutputs
                                            )
                                            nodes.add(duplicateNode)
                                            selectedNodeId = newId
                                        },
                                        onSaveToTemplates = {
                                            val tmplId = "tmpl_${System.currentTimeMillis()}"
                                            savedTemplates.add(
                                                NodeTemplateItem(
                                                    id = tmplId,
                                                    name = node.name,
                                                    role = node.role,
                                                    toolsCount = node.toolsCount,
                                                    permissionLevel = node.permissionLevel,
                                                    inputs = node.inputs,
                                                    outputs = node.outputs
                                                )
                                            )
                                            showTemplatesGallerySidebar = true
                                        },
                                        onOpenNodeSettings = {
                                            activeSettingsNodeId = node.id
                                        }
                                    )
                                }
                            }
                        }

                        // Templates Gallery Sidebar Drawer Overlay
                        if (showTemplatesGallerySidebar) {
                            Surface(
                                modifier = Modifier
                                    .width(220.dp)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterStart),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 8.dp,
                                shadowElevation = 8.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Node Templates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(onClick = { showTemplatesGallerySidebar = false }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Sidebar", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Text(
                                        text = "Tap 'Drop on Canvas' to reuse any saved node configuration.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(savedTemplates) { tmpl ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(text = tmpl.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                    Text(text = tmpl.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = "${tmpl.toolsCount} Tools • ${tmpl.permissionLevel}", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Button(
                                                        onClick = {
                                                            val newId = "node_${System.currentTimeMillis()}"
                                                            val cloneInputs = tmpl.inputs.mapIndexed { idx, p -> p.copy(id = "in_${newId}_$idx") }
                                                            val cloneOutputs = tmpl.outputs.mapIndexed { idx, p -> p.copy(id = "out_${newId}_$idx") }
                                                            val newNode = GraphNodeData(
                                                                id = newId,
                                                                position = Offset(200f, 200f),
                                                                name = tmpl.name,
                                                                role = tmpl.role,
                                                                status = "IDLE",
                                                                toolsCount = tmpl.toolsCount,
                                                                permissionLevel = tmpl.permissionLevel,
                                                                inputs = cloneInputs,
                                                                outputs = cloneOutputs
                                                            )
                                                            nodes.add(newNode)
                                                            selectedNodeId = newId
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        contentPadding = PaddingValues(vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Drop on Canvas", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Node Configuration & Agent Swarm Inspector Overlay
                        val selectedNodeObj = selectedNodeId?.let { selId -> nodes.find { it.id == selId } }
                        if (selectedNodeObj != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .align(Alignment.BottomStart)
                                    .padding(start = 10.dp, bottom = 68.dp)
                            ) {
                                NodeConfigurationPanel(
                                    node = selectedNodeObj,
                                    onClose = { selectedNodeId = null },
                                    onStatusChanged = { newStatus ->
                                        val idx = nodes.indexOfFirst { it.id == selectedNodeObj.id }
                                        if (idx != -1) {
                                            nodes[idx] = nodes[idx].copy(swarmStatus = newStatus)
                                        }
                                    },
                                    onAddSubTask = {
                                        val idx = nodes.indexOfFirst { it.id == selectedNodeObj.id }
                                        if (idx != -1) {
                                            val currentTasks = nodes[idx].delegationTasks.toMutableList()
                                            currentTasks.add(
                                                SwarmDelegationTask(
                                                    id = "sub_${System.currentTimeMillis()}",
                                                    title = "Auto-delegate sub-task #${currentTasks.size + 1}",
                                                    targetNode = "Peer Agent",
                                                    status = SwarmTaskStatus.RUNNING,
                                                    progressPercent = 0.5f,
                                                    detail = "Processing in parallel swarm queue",
                                                    timestamp = "Just now"
                                                )
                                            )
                                            nodes[idx] = nodes[idx].copy(delegationTasks = currentTasks, swarmStatus = SwarmTaskStatus.RUNNING)
                                        }
                                    },
                                    onRetryStalled = {
                                        val idx = nodes.indexOfFirst { it.id == selectedNodeObj.id }
                                        if (idx != -1) {
                                            val currentTasks = nodes[idx].delegationTasks.map { task ->
                                                if (task.status == SwarmTaskStatus.STALLED) task.copy(status = SwarmTaskStatus.RUNNING, detail = "Resumed after lock retry")
                                                else task
                                            }.toMutableList()
                                            nodes[idx] = nodes[idx].copy(delegationTasks = currentTasks, swarmStatus = SwarmTaskStatus.RUNNING)
                                        }
                                    }
                                )
                            }
                        }

                        // Right Slide-Out Node Settings Drawer Tab Overlay
                        val settingsNodeObj = activeSettingsNodeId?.let { sId -> nodes.find { it.id == sId } }
                        if (settingsNodeObj != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .align(Alignment.CenterEnd)
                            ) {
                                NodeSettingsRightDrawer(
                                    node = settingsNodeObj,
                                    availableProviders = availableProviders,
                                    onClose = { activeSettingsNodeId = null },
                                    onSaveSettings = { updatedProfile ->
                                        val idx = nodes.indexOfFirst { it.id == settingsNodeObj.id }
                                        if (idx != -1) {
                                            nodes[idx] = nodes[idx].copy(
                                                name = "${updatedProfile.agentType} Node",
                                                role = updatedProfile.agentType,
                                                settings = updatedProfile
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        // Floating Canvas Controls
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(24.dp),
                            tonalElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { zoomScale = (zoomScale - 0.2f).coerceAtLeast(0.4f) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                                }

                                Text("${(zoomScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                                IconButton(
                                    onClick = { zoomScale = (zoomScale + 0.2f).coerceAtMost(2.5f) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        fitCanvasToScreen(nodes, canvasWidthPx, canvasHeightPx) { scale, pan ->
                                            zoomScale = scale
                                            panOffset = pan
                                        }
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Fit to screen: ${(zoomScale * 100).roundToInt()}%")
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.CenterFocusWeak, contentDescription = "Fit to Screen", modifier = Modifier.size(16.dp))
                                }

                                VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                var showCleanupMenu by remember { mutableStateOf(false) }

                                Box {
                                    Button(
                                        onClick = {
                                            alignNodesToGrid(nodes, pipelines)
                                            fitCanvasToScreen(nodes, canvasWidthPx, canvasHeightPx) { scale, pan ->
                                                zoomScale = scale
                                                panOffset = pan
                                            }
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Canvas cleaned up: Grid aligned & Fit to screen")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        contentPadding = PaddingValues(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.GridView,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Cleanup",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { showCleanupMenu = true },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Cleanup Options",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showCleanupMenu,
                                        onDismissRequest = { showCleanupMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Clean Grid & Fit to Screen", style = MaterialTheme.typography.bodySmall) },
                                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                showCleanupMenu = false
                                                alignNodesToGrid(nodes, pipelines)
                                                fitCanvasToScreen(nodes, canvasWidthPx, canvasHeightPx) { scale, pan ->
                                                    zoomScale = scale
                                                    panOffset = pan
                                                }
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Grid aligned & Fit to screen")
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Align Nodes to Grid Only", style = MaterialTheme.typography.bodySmall) },
                                            leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                showCleanupMenu = false
                                                alignNodesToGrid(nodes, pipelines)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Nodes aligned into structured grid")
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Fit to Screen (Reset Zoom/Pan)", style = MaterialTheme.typography.bodySmall) },
                                            leadingIcon = { Icon(Icons.Default.CenterFocusWeak, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                showCleanupMenu = false
                                                fitCanvasToScreen(nodes, canvasWidthPx, canvasHeightPx) { scale, pan ->
                                                    zoomScale = scale
                                                    panOffset = pan
                                                }
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Reset view to fit screen: ${(zoomScale * 100).roundToInt()}%")
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Force-Directed Auto Layout", style = MaterialTheme.typography.bodySmall) },
                                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                showCleanupMenu = false
                                                runForceDirectedAutoLayout(nodes, pipelines)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Force-directed auto layout applied")
                                                }
                                            }
                                        )
                                    }
                                }

                                Button(
                                    onClick = { runForceDirectedAutoLayout(nodes, pipelines) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto Layout", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        val newNodeId = "node_${System.currentTimeMillis()}"
                                        nodes.add(
                                            GraphNodeData(
                                                id = newNodeId,
                                                position = Offset(220f, 220f),
                                                name = "Sub-Agent Node",
                                                role = "Specialized Task Executor",
                                                status = "IDLE",
                                                toolsCount = 3,
                                                permissionLevel = "L2 Standard",
                                                inputs = listOf(NodePort("in_${newNodeId}", "Task Input", "TaskPayload", true)),
                                                outputs = listOf(NodePort("out_${newNodeId}", "Result Output", "Text", false))
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Card", style = MaterialTheme.typography.labelSmall)
                                }

                                if (pipelines.isNotEmpty()) {
                                    IconButton(
                                        onClick = { pipelines.clear() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Clear Pipelines", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Scratchpad Inspector
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Swarm Scratchpad Inspector",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Shared memory pointers passed along pipeline connections between cards.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val sampleScratchpad = remember {
                            listOf(
                                "scratchpad://master/plan_v1" to "Task: Build WorkManager Sync Queue & Canvas UI",
                                "scratchpad://builder/diff_002" to "Added GraphScreen Canvas with pipeline Bezier curves",
                                "scratchpad://verifier/audit_passed" to "Security Interlock L3 Approval Granted"
                            )
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sampleScratchpad) { (ref, value) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = ref, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        val currentJson = remember { exportCurrentGraphJson() }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Pipeline Graph JSON")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Serialized pipeline schema containing node layouts, ports, and edge connections.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    OutlinedTextField(
                        value = currentJson,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Pipeline Graph JSON", currentJson)
                    clipboard.setPrimaryClip(clip)
                    showExportDialog = false
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Agent Pipeline Template")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Built-In Pipeline Template:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                    OutlinedButton(
                        onClick = {
                            val templateJson = """
                            {
                              "version": 1,
                              "name": "Full-Stack App Generator",
                              "nodes": [
                                {"id": "node_orchestrator", "posX": 60.0, "posY": 100.0, "name": "Orchestrator Agent", "role": "Master Coordinator", "status": "RUNNING", "toolsCount": 10, "permissionLevel": "L1-L4 Permissive", "inputs": [{"id": "in_goal", "name": "Goal", "dataType": "Text", "isInput": true}], "outputs": [{"id": "out_task", "name": "Task", "dataType": "TaskPayload", "isInput": false}]},
                                {"id": "node_ui_builder", "posX": 440.0, "posY": 50.0, "name": "UI Builder Agent", "role": "Compose Layout Engineer", "status": "IDLE", "toolsCount": 5, "permissionLevel": "L3 Read-Write", "inputs": [{"id": "in_task_ui", "name": "UI Spec", "dataType": "TaskPayload", "isInput": true}], "outputs": [{"id": "out_ui_code", "name": "Compose Code", "dataType": "FileDiff", "isInput": false}]},
                                {"id": "node_db_agent", "posX": 440.0, "posY": 260.0, "name": "Database Agent", "role": "Room/Firestore Schema", "status": "IDLE", "toolsCount": 3, "permissionLevel": "L3 Read-Write", "inputs": [{"id": "in_task_db", "name": "DB Spec", "dataType": "TaskPayload", "isInput": true}], "outputs": [{"id": "out_db_code", "name": "Entity Schema", "dataType": "FileDiff", "isInput": false}]}
                              ],
                              "pipelines": [
                                {"id": "p1", "fromNodeId": "node_orchestrator", "fromPortId": "out_task", "toNodeId": "node_ui_builder", "toPortId": "in_task_ui", "label": "ui_spec", "isActive": true},
                                {"id": "p2", "fromNodeId": "node_orchestrator", "fromPortId": "out_task", "toNodeId": "node_db_agent", "toPortId": "in_task_db", "label": "db_spec", "isActive": true}
                              ]
                            }
                            """.trimIndent()
                            importJsonInput = templateJson
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Template 1: Full-Stack App Generator", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            val templateJson = """
                            {
                              "version": 1,
                              "name": "Mismatched Connection Test",
                              "nodes": [
                                {"id": "node_orchestrator", "posX": 60.0, "posY": 100.0, "name": "Orchestrator Agent", "role": "Master Coordinator", "status": "RUNNING", "toolsCount": 10, "permissionLevel": "L1-L4 Permissive", "inputs": [{"id": "in_goal", "name": "Goal", "dataType": "Text", "isInput": true}], "outputs": [{"id": "out_task", "name": "Task", "dataType": "TaskPayload", "isInput": false}]},
                                {"id": "node_security", "posX": 440.0, "posY": 100.0, "name": "Security Interlock", "role": "Safety Gate", "status": "IDLE", "toolsCount": 2, "permissionLevel": "L4 Approval Interlock", "inputs": [{"id": "in_action", "name": "Action Request", "dataType": "ToolCallPayload", "isInput": true}], "outputs": [{"id": "out_approval", "name": "Approval Token", "dataType": "SecurityToken", "isInput": false}]}
                              ],
                              "pipelines": [
                                {"id": "p_invalid", "fromNodeId": "node_orchestrator", "fromPortId": "out_task", "toNodeId": "node_security", "toPortId": "in_action", "label": "mismatched_pipe", "isActive": true}
                              ]
                            }
                            """.trimIndent()
                            importJsonInput = templateJson
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Template 2: Mismatched Connection Test (Highlight RED)", style = MaterialTheme.typography.labelSmall)
                    }

                    Text("Or Paste Custom Pipeline JSON:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = {
                            importJsonInput = it
                            importErrorMessage = null
                        },
                        placeholder = { Text("{\n  \"nodes\": [...],\n  \"pipelines\": [...]\n}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )

                    importErrorMessage?.let { err ->
                        Text(text = err, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonInput.isNotBlank()) {
                            try {
                                val gson = Gson()
                                val parsed = gson.fromJson(importJsonInput, SerializedPipelineGraph::class.java)
                                if (parsed != null && parsed.nodes != null) {
                                    loadGraphFromSerialized(parsed)
                                    showImportDialog = false
                                } else {
                                    importErrorMessage = "Invalid graph format: missing nodes list."
                                }
                            } catch (e: Exception) {
                                importErrorMessage = "JSON Parse Error: ${e.message}"
                            }
                        }
                    },
                    enabled = importJsonInput.isNotBlank()
                ) {
                    Text("Import & Load Canvas")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Dialog for Validation Errors
    showValidationErrorModal?.let { errorDetails ->
        AlertDialog(
            onDismissRequest = { showValidationErrorModal = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pipeline Validation Error")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The graph contains incompatible port type connections highlighted in RED.")
                    Text(
                        text = "Reason: $errorDetails",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text("Please disconnect or re-wire the invalid pipeline connections before executing the agent swarm.")
                }
            },
            confirmButton = {
                Button(onClick = { showValidationErrorModal = null }) {
                    Text("Understood")
                }
            }
        )
    }
}

/**
 * Force-directed graph layout algorithm.
 */
fun runForceDirectedAutoLayout(
    nodes: List<GraphNodeData>,
    edges: List<PipelineEdge>,
    iterations: Int = 60
) {
    if (nodes.isEmpty()) return

    val kRepulsion = 1200000f
    val kAttraction = 0.008f
    val damping = 0.82f

    val velocities = mutableMapOf<String, Offset>()
    nodes.forEach { velocities[it.id] = Offset.Zero }

    for (iter in 0 until iterations) {
        for (i in nodes.indices) {
            val nodeA = nodes[i]
            var forceX = 0f
            var forceY = 0f

            for (j in nodes.indices) {
                if (i == j) continue
                val nodeB = nodes[j]
                val dx = nodeA.position.x - nodeB.position.x
                val dy = nodeA.position.y - nodeB.position.y
                val distSq = (dx * dx + dy * dy).coerceAtLeast(150f)
                val dist = sqrt(distSq)

                val force = kRepulsion / distSq
                forceX += (dx / dist) * force
                forceY += (dy / dist) * force
            }

            edges.forEach { edge ->
                if (edge.fromNodeId == nodeA.id) {
                    val target = nodes.find { it.id == edge.toNodeId }
                    if (target != null) {
                        val dx = target.position.x - nodeA.position.x
                        val dy = target.position.y - nodeA.position.y
                        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(10f)
                        val force = dist * kAttraction
                        forceX += (dx / dist) * force
                        forceY += (dy / dist) * force
                    }
                } else if (edge.toNodeId == nodeA.id) {
                    val source = nodes.find { it.id == edge.fromNodeId }
                    if (source != null) {
                        val dx = source.position.x - nodeA.position.x
                        val dy = source.position.y - nodeA.position.y
                        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(10f)
                        val force = dist * kAttraction
                        forceX += (dx / dist) * force
                        forceY += (dy / dist) * force
                    }
                }
            }

            val currentVel = velocities[nodeA.id] ?: Offset.Zero
            val newVelX = (currentVel.x + forceX) * damping
            val newVelY = (currentVel.y + forceY) * damping
            velocities[nodeA.id] = Offset(newVelX, newVelY)

            val newX = (nodeA.position.x + newVelX).coerceIn(40f, 1000f)
            val newY = (nodeA.position.y + newVelY).coerceIn(40f, 800f)
            nodeA.position = Offset(newX, newY)
        }
    }
}

/**
 * Aligns all graph nodes into a neat, balanced grid structure.
 */
fun alignNodesToGrid(
    nodes: List<GraphNodeData>,
    edges: List<PipelineEdge> = emptyList()
) {
    if (nodes.isEmpty()) return

    // Sort nodes to arrange orchestrator/root nodes first, followed by downstream nodes
    val sortedNodes = nodes.sortedWith(
        compareBy(
            { if (it.id == "node_orchestrator") 0 else 1 },
            { node -> edges.count { it.toNodeId == node.id } },
            { node -> -edges.count { it.fromNodeId == node.id } }
        )
    )

    val numCols = when {
        nodes.size <= 2 -> 2
        nodes.size <= 6 -> 2
        nodes.size <= 12 -> 3
        else -> 4
    }

    val startX = 60f
    val startY = 60f
    val spacingX = 220f
    val spacingY = 220f

    sortedNodes.forEachIndexed { index, node ->
        val col = index % numCols
        val row = index / numCols
        node.position = Offset(startX + col * spacingX, startY + row * spacingY)
    }
}

/**
 * Resets zoom and pan to a 'Fit to Screen' view for optimal canvas visibility.
 */
fun fitCanvasToScreen(
    nodes: List<GraphNodeData>,
    viewportWidth: Float,
    viewportHeight: Float,
    onUpdateTransform: (zoom: Float, pan: Offset) -> Unit
) {
    if (nodes.isEmpty()) {
        onUpdateTransform(1.0f, Offset(0f, 0f))
        return
    }

    val cardWidth = 140f
    val cardHeight = 180f

    val minX = nodes.minOf { it.position.x }
    val maxX = nodes.maxOf { it.position.x + cardWidth }
    val minY = nodes.minOf { it.position.y }
    val maxY = nodes.maxOf { it.position.y + cardHeight }

    val contentWidth = (maxX - minX).coerceAtLeast(100f)
    val contentHeight = (maxY - minY).coerceAtLeast(100f)

    val paddingX = 80f
    val paddingY = 80f

    val safeViewportW = (viewportWidth - paddingX * 2f).coerceAtLeast(100f)
    val safeViewportH = (viewportHeight - paddingY * 2f).coerceAtLeast(100f)

    val scaleX = safeViewportW / contentWidth
    val scaleY = safeViewportH / contentHeight
    val targetScale = minOf(scaleX, scaleY).coerceIn(0.4f, 1.5f)

    val contentCenterX = (minX + maxX) / 2f
    val contentCenterY = (minY + maxY) / 2f

    val targetPanX = (viewportWidth / 2f) - (contentCenterX * targetScale)
    val targetPanY = (viewportHeight / 2f) - (contentCenterY * targetScale)

    onUpdateTransform(targetScale, Offset(targetPanX, targetPanY))
}

@Composable
fun InteractiveCanvasNodeCard(
    node: GraphNodeData,
    isSelected: Boolean,
    activeSourcePort: Pair<String, String>?,
    onSelect: () -> Unit,
    onOutputPortClick: (NodePort) -> Unit,
    onInputPortClick: (NodePort) -> Unit,
    onDeleteNode: () -> Unit,
    onDuplicateNode: () -> Unit = {},
    onSaveToTemplates: () -> Unit = {},
    onOpenNodeSettings: () -> Unit = {}
) {
    val statusColor = when (node.status) {
        "RUNNING" -> MaterialTheme.colorScheme.primary
        "DONE" -> MaterialTheme.colorScheme.primary
        "BLOCKED" -> MaterialTheme.colorScheme.tertiary
        "FAILED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (node.id == "node_orchestrator") {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            // Header: Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Column {
                        Text(
                            text = node.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                        Text(
                            text = node.role,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 7.sp,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onOpenNodeSettings,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Open Node Settings Tab",
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (node.settings.documentation.isNotBlank()) {
                        IconButton(
                            onClick = onOpenNodeSettings,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "View Node Documentation Notes",
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onSaveToTemplates,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = "Save to Templates Gallery",
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDuplicateNode,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Duplicate Node Card",
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = node.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 7.sp
                        )
                    }

                    if (node.id != "node_orchestrator") {
                        IconButton(
                            onClick = onDeleteNode,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Delete Node", modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Agent Swarm Auto-Delegation Status Pill
            val swarmBadgeColor = when (node.swarmStatus) {
                SwarmTaskStatus.RUNNING -> MaterialTheme.colorScheme.primary
                SwarmTaskStatus.STALLED -> MaterialTheme.colorScheme.error
                SwarmTaskStatus.STANDBY -> MaterialTheme.colorScheme.secondary
            }
            val swarmBadgeText = when (node.swarmStatus) {
                SwarmTaskStatus.RUNNING -> "Swarm: RUNNING (${node.delegationTasks.size})"
                SwarmTaskStatus.STALLED -> "Swarm: STALLED (!)"
                SwarmTaskStatus.STANDBY -> "Swarm: STANDBY"
            }

            Surface(
                color = swarmBadgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(swarmBadgeColor)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = swarmBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = swarmBadgeColor
                    )
                }
            }

            // Capability Badge
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${node.toolsCount} Tools",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = node.permissionLevel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 7.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Mapped Inputs & Outputs Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Inputs Column (Left)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("INPUTS", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    node.inputs.forEach { port ->
                        val isTargetHighlight = activeSourcePort != null && activeSourcePort.first != node.id

                        Surface(
                            onClick = { onInputPortClick(port) },
                            color = if (isTargetHighlight) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isTargetHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = port.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (isTargetHighlight) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Outputs Column (Right)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("OUTPUTS", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.Output, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.tertiary)
                    }
                    node.outputs.forEach { port ->
                        val isSourceActive = activeSourcePort?.first == node.id && activeSourcePort.second == port.id

                        Surface(
                            onClick = { onOutputPortClick(port) },
                            color = if (isSourceActive) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(2.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = port.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSourceActive) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isSourceActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentSwarmTaskStatusIndicator(
    swarmStatus: SwarmTaskStatus,
    taskCount: Int,
    stalledCount: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "swarm_indicator_anim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (containerColor, contentColor, icon, titleText, subText) = when (swarmStatus) {
        SwarmTaskStatus.RUNNING -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.PlayArrow
        ).let { (c1, c2, ic) ->
            listOf(c1, c2, ic, "SWARM RUNNING", "$taskCount auto-delegation sub-task(s) active in parallel pipeline")
        }
        SwarmTaskStatus.STALLED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Warning
        ).let { (c1, c2, ic) ->
            listOf(c1, c2, ic, "DELEGATION STALLED", "$stalledCount task(s) stalled: waiting for lock resolution or policy gate")
        }
        SwarmTaskStatus.STANDBY -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Pause
        ).let { (c1, c2, ic) ->
            listOf(c1, c2, ic, "SWARM STANDBY", "Swarm node standby: idle awaiting orchestrator dispatch")
        }
    }

    val cColor = containerColor as Color
    val txtColor = contentColor as Color
    val iconVec = icon as androidx.compose.ui.graphics.vector.ImageVector
    val titleStr = titleText as String
    val subStr = subText as String

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = cColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (swarmStatus == SwarmTaskStatus.STALLED) MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing Indicator Ring
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (swarmStatus == SwarmTaskStatus.RUNNING) txtColor.copy(alpha = 0.2f * pulseAlpha)
                        else if (swarmStatus == SwarmTaskStatus.STALLED) MaterialTheme.colorScheme.error.copy(alpha = 0.3f * pulseAlpha)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (swarmStatus == SwarmTaskStatus.RUNNING) txtColor
                            else if (swarmStatus == SwarmTaskStatus.STALLED) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.outline
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVec,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = titleStr,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = txtColor,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = txtColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = swarmStatus.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = txtColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subStr,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = txtColor.copy(alpha = 0.85f)
                )

                if (swarmStatus == SwarmTaskStatus.RUNNING) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = txtColor,
                        trackColor = txtColor.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
fun NodeConfigurationPanel(
    node: GraphNodeData,
    onClose: () -> Unit,
    onStatusChanged: (SwarmTaskStatus) -> Unit,
    onAddSubTask: () -> Unit,
    onRetryStalled: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Agent Node Config: ${node.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${node.role} • ${node.permissionLevel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Config", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Visual Indicator: Agent Swarm Auto-Delegation Status
            Text(
                text = "Agent Swarm Auto-Delegation Status",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            val stalledCount = node.delegationTasks.count { it.status == SwarmTaskStatus.STALLED }
            AgentSwarmTaskStatusIndicator(
                swarmStatus = node.swarmStatus,
                taskCount = node.delegationTasks.size,
                stalledCount = stalledCount
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Status Switcher (Chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Force Swarm Status:",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                FilterChip(
                    selected = node.swarmStatus == SwarmTaskStatus.RUNNING,
                    onClick = { onStatusChanged(SwarmTaskStatus.RUNNING) },
                    label = { Text("Running", fontSize = 10.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                )

                FilterChip(
                    selected = node.swarmStatus == SwarmTaskStatus.STALLED,
                    onClick = { onStatusChanged(SwarmTaskStatus.STALLED) },
                    label = { Text("Stalled", fontSize = 10.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.error)
                    }
                )

                FilterChip(
                    selected = node.swarmStatus == SwarmTaskStatus.STANDBY,
                    onClick = { onStatusChanged(SwarmTaskStatus.STANDBY) },
                    label = { Text("Standby", fontSize = 10.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Sub-Tasks Breakdown Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-Delegation Tasks (${node.delegationTasks.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (stalledCount > 0) {
                        OutlinedButton(
                            onClick = onRetryStalled,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry Stalled", fontSize = 9.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Button(
                        onClick = onAddSubTask,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Sub-Task", fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (node.delegationTasks.isEmpty()) {
                Text(
                    text = "No active auto-delegation tasks configured for this agent node.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    node.delegationTasks.forEach { task ->
                        val taskColor = when (task.status) {
                            SwarmTaskStatus.RUNNING -> MaterialTheme.colorScheme.primary
                            SwarmTaskStatus.STALLED -> MaterialTheme.colorScheme.error
                            SwarmTaskStatus.STANDBY -> MaterialTheme.colorScheme.secondary
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(taskColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Surface(
                                        color = taskColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = task.status.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = taskColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                if (task.detail.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = task.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = if (task.status == SwarmTaskStatus.STALLED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Target: ${task.targetNode} • Updated ${task.timestamp}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    if (task.status == SwarmTaskStatus.STALLED) {
                                        TextButton(
                                            onClick = {
                                                task.status = SwarmTaskStatus.RUNNING
                                                node.swarmStatus = SwarmTaskStatus.RUNNING
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(20.dp)
                                        ) {
                                            Text("Resume Task", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeSettingsRightDrawer(
    node: GraphNodeData,
    availableProviders: List<ProviderSettings>,
    onClose: () -> Unit,
    onSaveSettings: (NodeSettingsProfile) -> Unit
) {
    var selectedProviderId by remember(node.settings) { mutableStateOf(node.settings.providerId) }
    var selectedModel by remember(node.settings) { mutableStateOf(node.settings.model) }
    var selectedAgentType by remember(node.settings) { mutableStateOf(node.settings.agentType) }
    var instructionsText by remember(node.settings) { mutableStateOf(node.settings.instructions) }
    var selectedTools by remember(node.settings) { mutableStateOf(node.settings.tools.toSet()) }
    var documentationText by remember(node.settings) { mutableStateOf(node.settings.documentation) }

    var selectedTab by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier
            .width(340.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 12.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Node Settings Tab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${node.name} (${node.id})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Settings Tab", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-navigation Tabs
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Provider", fontSize = 9.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Instructions", fontSize = 9.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Tools", fontSize = 9.sp) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Docs", fontSize = 9.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        Text(
                            text = "Configured Providers (from App Home Menu)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        availableProviders.forEach { prov ->
                            val isChosen = prov.id == selectedProviderId
                            Card(
                                onClick = { selectedProviderId = prov.id },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = prov.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text(text = prov.baseUrl, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    if (isChosen) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Node Model Selection",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val commonModels = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gpt-4o", "claude-3-5-sonnet", "llama3.2:latest", "deepseek-coder-v2")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(commonModels) { m ->
                                FilterChip(
                                    selected = selectedModel == m,
                                    onClick = { selectedModel = m },
                                    label = { Text(m, fontSize = 9.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = { selectedModel = it },
                            label = { Text("Model Identifier") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                    }

                    1 -> {
                        Text(
                            text = "Agent Type & Persona",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val agentTypes = listOf("Orchestrator", "Code Generator", "Linter", "Security Auditor", "Database Architect", "Custom Agent")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(agentTypes) { type ->
                                FilterChip(
                                    selected = selectedAgentType == type,
                                    onClick = { selectedAgentType = type },
                                    label = { Text(type, fontSize = 9.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = selectedAgentType,
                            onValueChange = { selectedAgentType = it },
                            label = { Text("Agent Type") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Node System Instructions",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = instructionsText,
                            onValueChange = { instructionsText = it },
                            label = { Text("Instructions / System Prompt") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    2 -> {
                        Text(
                            text = "Node Capabilities & Allowed Tools",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val allTools = listOf(
                            "Web Search Grounding",
                            "File System Read/Write",
                            "Code Linter & AST Checker",
                            "Database Query Engine",
                            "Terminal Execution",
                            "Security Interlock Gate",
                            "Scratchpad Memory Pointer"
                        )

                        allTools.forEach { t ->
                            val isChecked = selectedTools.contains(t)
                            Card(
                                onClick = {
                                    selectedTools = if (isChecked) selectedTools - t else selectedTools + t
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = t, style = MaterialTheme.typography.labelMedium, fontSize = 11.sp)
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            selectedTools = if (it) selectedTools + t else selectedTools - t
                                        }
                                    )
                                }
                            }
                        }
                    }

                    3 -> {
                        Text(
                            text = "Node Documentation & Markdown Notes",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add explainers, architecture notes, or team collaboration guidelines in Markdown.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = documentationText,
                            onValueChange = { documentationText = it },
                            label = { Text("Markdown Documentation / Notes") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val provName = availableProviders.find { it.id == selectedProviderId }?.name ?: selectedProviderId
                    val updatedProfile = NodeSettingsProfile(
                        providerId = selectedProviderId,
                        providerName = provName,
                        model = selectedModel,
                        agentType = selectedAgentType,
                        instructions = instructionsText,
                        tools = selectedTools.toList(),
                        documentation = documentationText
                    )
                    onSaveSettings(updatedProfile)
                    onClose()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Node Settings")
            }
        }
    }
}

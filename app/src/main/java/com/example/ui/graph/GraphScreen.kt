package com.example.ui.graph

import androidx.compose.animation.core.*
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

data class GraphNodeData(
    val id: String,
    var position: Offset,
    val name: String,
    val role: String,
    val status: String,
    val toolsCount: Int,
    val permissionLevel: String,
    val inputs: List<NodePort>,
    val outputs: List<NodePort>
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
    val outputs: List<NodePort>
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

    // Sidebar & Dialog states
    var showTemplatesGallerySidebar by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showValidationErrorModal by remember { mutableStateOf<String?>(null) }
    var importJsonInput by remember { mutableStateOf("") }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

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
                    outputs = sNode.outputs
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
                outputs = n.outputs
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
                        runForceDirectedAutoLayout(nodes, pipelines)
                    }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Auto Layout", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = {
                        panOffset = Offset(0f, 0f)
                        zoomScale = 1.0f
                    }) {
                        Icon(Icons.Default.CenterFocusWeak, contentDescription = "Recenter View")
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

                                VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                Button(
                                    onClick = { runForceDirectedAutoLayout(nodes, pipelines) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto Layout", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
    onSaveToTemplates: () -> Unit = {}
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

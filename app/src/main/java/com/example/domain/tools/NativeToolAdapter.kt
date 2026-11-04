package com.example.domain.tools

import com.apex.core.json.MiniJson
import com.apex.core.tool.ToolDefinition
import com.apex.core.tool.ToolWorker
import com.apex.core.tool.TransportHint

object NativeToolAdapter {
    fun wrap(worker: ToolWorker, schemaJson: String? = null, riskLevel: String = "L2"): ToolDefinition {
        val parsedSchema = if (!schemaJson.isNullOrBlank()) {
            try { MiniJson.parse(schemaJson) } catch (e: Exception) { null }
        } else null

        return ToolDefinition(
            "native://" + worker.name,
            worker.name,
            worker.description,
            ToolDefinition.Kind.TOOL,
            ToolDefinition.Source.NATIVE,
            parsedSchema,
            riskLevel,
            TransportHint.local(),
            emptyMap()
        )
    }
}

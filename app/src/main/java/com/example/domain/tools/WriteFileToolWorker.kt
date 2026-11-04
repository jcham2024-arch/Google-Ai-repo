package com.example.domain.tools

import com.apex.core.tool.ToolWorker
import com.example.domain.engine.FileGovernanceEngine
import kotlinx.coroutines.runBlocking
import java.io.File

class WriteFileToolWorker(
    private val fileGovernanceEngine: FileGovernanceEngine,
    private val baseDir: File
) : ToolWorker {
    override fun getName(): String = "write_file"
    override fun getDescription(): String = "Writes text content to a specified file path."

    override fun execute(arguments: Map<String, String>): String {
        val path = arguments["path"] ?: throw IllegalArgumentException("Missing 'path' argument")
        val content = arguments["content"] ?: ""

        // Delegate to FileGovernanceEngine for Room + disk persistence
        val success = runBlocking {
            fileGovernanceEngine.applyApprovedSelfEdit(path, content)
        }

        return if (success) {
            "Successfully wrote ${content.length} characters to $path"
        } else {
            throw Exception("Failed to write file via FileGovernanceEngine")
        }
    }
}

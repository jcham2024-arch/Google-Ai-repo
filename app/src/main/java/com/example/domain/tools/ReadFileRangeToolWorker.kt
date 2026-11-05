package com.example.domain.tools

import com.apex.core.tool.ToolWorker
import com.example.domain.engine.WorkspacePathGuard
import java.io.File

class ReadFileRangeToolWorker(private val sandboxRoot: File) : ToolWorker {
    override fun getName(): String = "read_file_range"
    override fun getDescription(): String = "Reads a specific range of lines from a file. Use this for large files to avoid context overflow."

    override fun execute(arguments: Map<String, String>): String {
        val path = arguments["path"] ?: throw IllegalArgumentException("Missing 'path' argument")
        val startLine = arguments["start_line"]?.toIntOrNull() ?: 1
        val endLine = arguments["end_line"]?.toIntOrNull() ?: Int.MAX_VALUE
        
        val file = WorkspacePathGuard.resolveOrThrow(sandboxRoot, path)
        if (!file.exists()) {
            throw NoSuchFileException(file, reason = "File not found: $path")
        }
        
        val lines = file.readLines()
        val totalLines = lines.size
        
        // Validate and clamp line numbers
        val start = maxOf(1, startLine)
        val end = minOf(totalLines, endLine)
        
        if (start > totalLines) {
            return "File has $totalLines lines. Requested start line $start is beyond file end."
        }
        
        val selectedLines = lines.subList(start - 1, end)
        val content = selectedLines.joinToString("\n")
        
        val header = "File: $path (lines $start-$end of $totalLines)\n"
        val footer = if (end < totalLines) "\n[... TRUNCATED. File has ${totalLines - end} more lines. Use read_file_range with end_line to read more ...]" else ""
        
        return header + content + footer
    }
}

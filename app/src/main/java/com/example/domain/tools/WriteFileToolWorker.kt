package com.example.domain.tools

import com.apex.core.tool.ToolWorker
import java.io.File

class WriteFileToolWorker(private val baseDir: File) : ToolWorker {
    override fun getName(): String = "write_file"
    override fun getDescription(): String = "Writes text content to a specified file path."

    override fun execute(arguments: Map<String, String>): String {
        val path = arguments["path"] ?: throw IllegalArgumentException("Missing 'path' argument")
        val content = arguments["content"] ?: ""
        val file = File(baseDir, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
        return "Successfully wrote ${content.length} characters to $path"
    }
}

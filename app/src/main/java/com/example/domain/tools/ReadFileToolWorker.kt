package com.example.domain.tools

import com.apex.core.tool.ToolWorker
import java.io.File

class ReadFileToolWorker(private val baseDir: File) : ToolWorker {
    override fun getName(): String = "read_file"
    override fun getDescription(): String = "Reads text content of a specified file path."

    override fun execute(arguments: Map<String, String>): String {
        val path = arguments["path"] ?: throw IllegalArgumentException("Missing 'path' argument")
        val file = File(baseDir, path)
        if (!file.exists()) {
            throw NoSuchFileException(file, reason = "File not found: $path")
        }
        return file.readText()
    }
}

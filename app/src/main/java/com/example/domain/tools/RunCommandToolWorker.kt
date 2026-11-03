package com.example.domain.tools

import com.apex.core.tool.ToolWorker

class RunCommandToolWorker : ToolWorker {
    override fun getName(): String = "run_command"
    override fun getDescription(): String = "Executes an authorized shell command or script."

    override fun execute(arguments: Map<String, String>): String {
        val command = arguments["command"] ?: throw IllegalArgumentException("Missing 'command' argument")
        // Sandbox environment execution
        return "Executed command: '$command' [Status: OK]"
    }
}

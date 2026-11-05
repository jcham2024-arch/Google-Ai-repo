package com.example.domain.tools

import com.apex.core.tool.ToolWorker

/**
 * Deliberately inert: this build has no command execution surface.
 *
 * Every call fails closed instead of returning a formatted success string, so the model and the
 * audit trail can never record a side effect that did not happen. The tool keeps its L4 risk entry
 * in `ToolRiskRegistry`; implementing execution means deciding the allowed command set, the timeout
 * and the output cap first.
 */
class RunCommandToolWorker : ToolWorker {
    override fun getName(): String = "run_command"
    override fun getDescription(): String = "Unavailable: this build executes nothing and reports that no command was run."

    override fun execute(arguments: Map<String, String>): String {
        arguments["command"] ?: throw IllegalArgumentException("Missing 'command' argument")
        throw UnsupportedOperationException(
            "run_command is not implemented in this build; no command was executed"
        )
    }
}

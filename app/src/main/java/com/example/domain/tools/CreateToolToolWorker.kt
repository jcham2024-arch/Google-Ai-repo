package com.example.domain.tools

import android.content.Context
import com.apex.core.tool.ToolWorker
import java.io.File

class CreateToolToolWorker(
    private val context: Context,
    private val toolRegistry: com.apex.core.tool.ToolRegistry
) : ToolWorker {
    override fun getName(): String = "create_tool"
    override fun getDescription(): String = "Creates a new tool by writing Kotlin source code to the workspace and dynamically loading it. Use this when you need a tool that doesn't exist yet."

    override fun execute(arguments: Map<String, String>): String {
        val toolName = arguments["tool_name"] ?: return "Error: tool_name is required"
        val sourceCode = arguments["source_code"] ?: return "Error: source_code is required"
        val description = arguments["description"] ?: "Dynamically created tool"

        // Validate the tool name
        if (!toolName.matches("^[a-z_][a-z0-9_]*$".toRegex())) {
            return "Error: tool_name must be lowercase with underscores only (e.g., 'calculate_fibonacci')"
        }

        // Write the source code to a file in the workspace
        val toolsDir = File(context.filesDir, "workspace/tools")
        toolsDir.mkdirs()
        
        val fileName = "${toolName}ToolWorker.kt"
        val toolFile = File(toolsDir, fileName)
        
        // Wrap the source code in a proper class if needed
        val fullSourceCode = if (sourceCode.contains("class ${toolName.capitalize()}ToolWorker")) {
            sourceCode
        } else {
            // Generate a basic tool worker class
            """
            package com.example.domain.tools.dynamic
            
            import com.apex.core.tool.ToolWorker
            
            class ${toolName.capitalize()}ToolWorker : ToolWorker {
                override fun getName(): String = "$toolName"
                override fun getDescription(): String = "$description"
                
                override fun execute(arguments: Map<String, String>): String {
                    $sourceCode
                }
            }
            """.trimIndent()
        }
        
        toolFile.writeText(fullSourceCode)
        
        // Note: Dynamic loading with DexClassLoader would require:
        // 1. Compiling the Kotlin to .class files (requires kotlinc or kotlinc-jvm at runtime - not feasible on Android)
        // 2. Converting to .dex format
        // 3. Loading with DexClassLoader
        // 
        // For now, we save the file to workspace. The user can then:
        // 1. Export the workspace
        // 2. Compile externally
        // 3. Add to the app in a future build
        
        return "Tool source code saved to workspace/tools/$fileName. " +
               "To use this tool, export the workspace, compile the Kotlin code externally, " +
               "and add the compiled class to the app in a future build. " +
               "Dynamic runtime loading via DexClassLoader requires pre-compiled .dex files."
    }
}

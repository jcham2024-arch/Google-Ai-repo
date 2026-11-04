package com.example.domain.engine

/**
 * Simple diff utility for showing line-by-line differences between two texts.
 */
object DiffUtil {
    data class DiffLine(
        val type: DiffType,
        val content: String,
        val oldLineNumber: Int? = null,
        val newLineNumber: Int? = null
    )
    
    enum class DiffType {
        UNCHANGED, ADDED, REMOVED
    }
    
    /**
     * Generates a simple unified diff between old and new text.
     * Returns a list of DiffLine objects representing the diff.
     */
    fun generateDiff(oldText: String, newText: String): List<DiffLine> {
        val oldLines = oldText.split("\n").toMutableList()
        val newLines = newText.split("\n").toMutableList()
        
        val result = mutableListOf<DiffLine>()
        var oldIndex = 0
        var newIndex = 0
        var oldLineNum = 1
        var newLineNum = 1
        
        // Simple LCS-based diff (Myers diff would be better but this is simpler)
        while (oldIndex < oldLines.size || newIndex < newLines.size) {
            val oldLine = if (oldIndex < oldLines.size) oldLines[oldIndex] else null
            val newLine = if (newIndex < newLines.size) newLines[newIndex] else null
            
            if (oldLine != null && newLine != null && oldLine == newLine) {
                result.add(DiffLine(DiffType.UNCHANGED, oldLine, oldLineNum, newLineNum))
                oldIndex++
                newIndex++
                oldLineNum++
                newLineNum++
            } else if (oldLine != null && (newLine == null || !newLines.subList(newIndex, newLines.size).contains(oldLine))) {
                // Line removed
                result.add(DiffLine(DiffType.REMOVED, oldLine, oldLineNum, null))
                oldIndex++
                oldLineNum++
            } else if (newLine != null && (oldLine == null || !oldLines.subList(oldIndex, oldLines.size).contains(newLine))) {
                // Line added
                result.add(DiffLine(DiffType.ADDED, newLine, null, newLineNum))
                newIndex++
                newLineNum++
            } else if (oldLine != null && newLine != null) {
                // Both exist but different - treat as remove + add
                result.add(DiffLine(DiffType.REMOVED, oldLine, oldLineNum, null))
                result.add(DiffLine(DiffType.ADDED, newLine, null, newLineNum))
                oldIndex++
                newIndex++
                oldLineNum++
                newLineNum++
            }
        }
        
        return result
    }
    
    /**
     * Generates a compact diff summary for display.
     */
    fun generateDiffSummary(oldText: String, newText: String): String {
        val diff = generateDiff(oldText, newText)
        val changes = diff.filter { it.type != DiffType.UNCHANGED }
        if (changes.isEmpty()) return "No changes"
        
        val added = changes.count { it.type == DiffType.ADDED }
        val removed = changes.count { it.type == DiffType.REMOVED }
        return "$added lines added, $removed lines removed"
    }
}

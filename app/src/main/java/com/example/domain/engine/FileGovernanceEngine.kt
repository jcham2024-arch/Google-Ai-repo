package com.example.domain.engine

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import com.example.data.local.AppDatabase
import com.example.data.model.WorkspaceFile
import com.example.data.model.WorkspaceFileVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class WorkspaceEvent {
    data class FileCreated(val path: String, val name: String, val content: String) : WorkspaceEvent()
    data class FileModified(val path: String, val name: String, val content: String) : WorkspaceEvent()
    data class FileDeleted(val path: String) : WorkspaceEvent()
}

class FileGovernanceEngine(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).apexDao()
    private val _workspaceEvents = MutableStateFlow<WorkspaceEvent?>(null)
    val workspaceEvents: StateFlow<WorkspaceEvent?> = _workspaceEvents

    // Sandbox configuration. Every tool worker receives this same root.
    val sandboxRoot = File(context.filesDir, "workspace")
    companion object {
        const val MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024 // 10MB per file
        const val MAX_TOTAL_SIZE_BYTES = 100L * 1024 * 1024 // 100MB total
        const val MAX_FILE_COUNT = 1000
        const val MAX_VERSIONS_PER_FILE = 10
    }

    init {
        sandboxRoot.mkdirs()
    }

    /**
     * Validates and sanitizes a path to prevent directory traversal.
     * Returns the canonical path within the sandbox, or null if invalid.
     */
    private fun validatePath(path: String): File? {
        val targetFile = WorkspacePathGuard.resolve(sandboxRoot, path) ?: return null

        // Ensure the parent directory exists or can be created
        targetFile.parentFile?.mkdirs()

        return targetFile
    }

    /**
     * Checks if the sandbox has space for a new file of the given size.
     */
    private suspend fun checkQuota(newFileSize: Long): Boolean = withContext(Dispatchers.IO) {
        val files = dao.getAllWorkspaceFiles().first()
        val totalSize = files.sumOf { it.sizeBytes }
        val fileCount = files.size
        
        (totalSize + newFileSize <= MAX_TOTAL_SIZE_BYTES) && (fileCount < MAX_FILE_COUNT)
    }

    suspend fun createFile(path: String, name: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val fileSize = content.toByteArray().size.toLong()
        
        // Validate path
        val targetFile = validatePath(path) ?: return@withContext false
        
        // Check quota
        if (!checkQuota(fileSize)) {
            return@withContext false
        }
        
        // Check file size limit
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            return@withContext false
        }
        
        try {
            val workspaceFile = WorkspaceFile(
                path = path,
                name = name,
                content = content,
                isDirectory = false,
                lastModified = System.currentTimeMillis(),
                sizeBytes = fileSize,
                version = 1
            )
            dao.insertWorkspaceFile(workspaceFile)

            // Save version history
            val version = WorkspaceFileVersion(
                filePath = path,
                content = content,
                sizeBytes = fileSize,
                version = 1,
                timestamp = System.currentTimeMillis(),
                changeType = "CREATE"
            )
            dao.insertFileVersion(version)

            // Write physical file
            targetFile.writeText(content)
            
            _workspaceEvents.value = WorkspaceEvent.FileCreated(path, name, content)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun applyApprovedSelfEdit(path: String, content: String): Boolean = withContext(Dispatchers.IO) {
        // Validate path
        val targetFile = validatePath(path) ?: return@withContext false
        
        val fileSize = content.toByteArray().size.toLong()
        
        // Check file size limit
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            return@withContext false
        }
        
        // Get current version for history
        val currentFile = dao.getAllWorkspaceFiles().first().firstOrNull { it.path == path }
        val nextVersion = (currentFile?.version ?: 0) + 1
        
        try {
            val workspaceFile = WorkspaceFile(
                path = path,
                name = File(path).name,
                content = content,
                isDirectory = false,
                lastModified = System.currentTimeMillis(),
                sizeBytes = fileSize,
                version = nextVersion
            )
            dao.insertWorkspaceFile(workspaceFile)

            // Save version history
            val version = WorkspaceFileVersion(
                filePath = path,
                content = content,
                sizeBytes = fileSize,
                version = nextVersion,
                timestamp = System.currentTimeMillis(),
                changeType = "MODIFY"
            )
            dao.insertFileVersion(version)

            // Cleanup old versions beyond max
            cleanupOldVersions(path)

            // Write physical file
            targetFile.writeText(content)
            
            _workspaceEvents.value = WorkspaceEvent.FileModified(path, File(path).name, content)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun cleanupOldVersions(path: String) = withContext(Dispatchers.IO) {
        val versions = dao.getFileVersions(path).first()
        if (versions.size > MAX_VERSIONS_PER_FILE) {
            val toDelete = versions.drop(MAX_VERSIONS_PER_FILE)
            toDelete.forEach { dao.deleteFileVersions(it.filePath) }
        }
    }
    
    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val targetFile = validatePath(path) ?: return@withContext false
        
        try {
            dao.deleteWorkspaceFile(path)
            // Keep version history for potential recovery
            targetFile.delete()
            _workspaceEvents.value = WorkspaceEvent.FileDeleted(path)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets the version history for a file.
     */
    suspend fun getFileVersionHistory(path: String): List<WorkspaceFileVersion> = withContext(Dispatchers.IO) {
        dao.getFileVersions(path).first()
    }
    
    /**
     * Restores a file to a specific version.
     */
    suspend fun restoreFileVersion(path: String, version: Int): Boolean = withContext(Dispatchers.IO) {
        val targetFile = validatePath(path) ?: return@withContext false
        
        val versions = dao.getFileVersions(path).first()
        val versionToRestore = versions.firstOrNull { it.version == version } ?: return@withContext false
        
        val fileSize = versionToRestore.content.toByteArray().size.toLong()
        val nextVersion = (dao.getAllWorkspaceFiles().first().firstOrNull { it.path == path }?.version ?: 0) + 1
        
        try {
            val workspaceFile = WorkspaceFile(
                path = path,
                name = File(path).name,
                content = versionToRestore.content,
                isDirectory = false,
                lastModified = System.currentTimeMillis(),
                sizeBytes = fileSize,
                version = nextVersion
            )
            dao.insertWorkspaceFile(workspaceFile)

            // Save version history
            val versionRecord = WorkspaceFileVersion(
                filePath = path,
                content = versionToRestore.content,
                sizeBytes = fileSize,
                version = nextVersion,
                timestamp = System.currentTimeMillis(),
                changeType = "RESTORE"
            )
            dao.insertFileVersion(versionRecord)

            targetFile.writeText(versionToRestore.content)
            
            _workspaceEvents.value = WorkspaceEvent.FileModified(path, File(path).name, versionToRestore.content)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Reads file content from disk (for diff generation).
     * Returns null if file doesn't exist.
     */
    fun readFile(path: String): String? {
        return try {
            val targetFile = validatePath(path) ?: return null
            if (targetFile.exists()) targetFile.readText() else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets sandbox usage statistics.
     */
    suspend fun getSandboxStats(): SandboxStats = withContext(Dispatchers.IO) {
        val files = dao.getAllWorkspaceFiles().first()
        val totalSize = files.sumOf { it.sizeBytes }
        val fileCount = files.size
        SandboxStats(
            totalSizeBytes = totalSize,
            fileCount = fileCount,
            maxTotalSizeBytes = MAX_TOTAL_SIZE_BYTES,
            maxFileCount = MAX_FILE_COUNT,
            maxFileSizeBytes = MAX_FILE_SIZE_BYTES
        )
    }
}

data class SandboxStats(
    val totalSizeBytes: Long,
    val fileCount: Int,
    val maxTotalSizeBytes: Long,
    val maxFileCount: Int,
    val maxFileSizeBytes: Long
) {
    val usagePercent: Double = if (maxTotalSizeBytes > 0) (totalSizeBytes.toDouble() / maxTotalSizeBytes) * 100 else 0.0
    val countUsagePercent: Double = if (maxFileCount > 0) (fileCount.toDouble() / maxFileCount) * 100 else 0.0
}

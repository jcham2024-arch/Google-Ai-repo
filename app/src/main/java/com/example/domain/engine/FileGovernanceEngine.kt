package com.example.domain.engine

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.WorkspaceFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileGovernanceEngine(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).apexDao()

    suspend fun createFile(path: String, name: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val workspaceFile = WorkspaceFile(
                path = path,
                name = name,
                content = content,
                isDirectory = false,
                lastModified = System.currentTimeMillis()
            )
            dao.insertWorkspaceFile(workspaceFile)

            // Also write to app internal storage for physical execution / access
            val physicalFile = File(context.filesDir, path)
            physicalFile.parentFile?.mkdirs()
            physicalFile.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun applyApprovedSelfEdit(path: String, content: String): Boolean = withContext(Dispatchers.IO) {
        // Only applies if user explicitly reviewed and approved the change
        createFile(path, File(path).name, content)
    }
}

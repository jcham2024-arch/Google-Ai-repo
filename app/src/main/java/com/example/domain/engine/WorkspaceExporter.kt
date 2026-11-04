package com.example.domain.engine

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Exports the workspace directory as a ZIP file for external compilation.
 */
class WorkspaceExporter(private val context: Context) {

    /**
     * Creates a ZIP file of the workspace and saves it to the user's Downloads folder
     * via MediaStore. Returns the URI of the created file.
     */
    suspend fun exportWorkspace(): Result<Uri> = withContext(ioDispatcher) {
        try {
            val workspaceDir = File(context.filesDir, "workspace")
            if (!workspaceDir.exists() || workspaceDir.listFiles()?.isEmpty() != false) {
                return Result.failure(IllegalStateException("Workspace is empty or does not exist"))
            }

            // Create a unique filename with timestamp
            val timestamp = System.currentTimeMillis()
            val fileName = "apex-workspace-$timestamp.zip"

            // Use MediaStore to save to Downloads folder
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return Result.failure(IOException("Failed to create MediaStore entry"))

            resolver.openOutputStream(uri)?.use { outputStream ->
                zipDirectory(workspaceDir, outputStream, "")
            } ?: return Result.failure(IOException("Failed to open output stream"))

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a ZIP file and returns it as a File (for sharing via FileProvider).
     * Use this if you want to share the file via Intent.
     */
    suspend fun exportWorkspaceToFile(): Result<File> = withContext(ioDispatcher) {
        try {
            val workspaceDir = File(context.filesDir, "workspace")
            if (!workspaceDir.exists() || workspaceDir.listFiles()?.isEmpty() != false) {
                return Result.failure(IllegalStateException("Workspace is empty or does not exist"))
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "apex-workspace-$timestamp.zip"
            val zipFile = File(context.cacheDir, fileName)

            FileOutputStream(zipFile).use { outputStream ->
                zipDirectory(workspaceDir, outputStream, "")
            }

            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun zipDirectory(sourceDir: File, outputStream: java.io.OutputStream, parentPath: String) {
        val zipOutputStream = ZipOutputStream(outputStream)
        try {
            val files = sourceDir.listFiles() ?: return
            for (file in files) {
                val entryName = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
                if (file.isDirectory) {
                    zipDirectory(file, outputStream, entryName)
                } else {
                    val entry = ZipEntry(entryName)
                    zipOutputStream.putNextEntry(entry)
                    file.copyTo(zipOutputStream)
                    zipOutputStream.closeEntry()
                }
            }
        } finally {
            zipOutputStream.finish()
        }
    }

    companion object {
        private val ioDispatcher = kotlinx.coroutines.Dispatchers.IO
    }
}

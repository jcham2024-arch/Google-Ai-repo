package com.example.domain.engine

import java.io.File
import java.io.IOException

/**
 * Single validator for every model-supplied path that reaches the workspace sandbox.
 *
 * Rejects absolute paths, empty paths and traversal segments, then confirms the canonical target
 * still sits under the canonical sandbox root. [FileGovernanceEngine] and the read tool workers
 * share this object so the read and write paths cannot drift apart.
 */
object WorkspacePathGuard {

    /**
     * Resolves [path] inside [sandboxRoot], or returns null when the path is malformed or escapes.
     */
    fun resolve(sandboxRoot: File, path: String): File? {
        val normalizedPath = path.trim().replace('\\', '/')

        // Reject absolute paths, empty paths, and paths with traversal attempts
        if (normalizedPath.isEmpty() || normalizedPath.startsWith("/") || normalizedPath.contains("..")) {
            return null
        }

        return try {
            val canonicalSandbox = sandboxRoot.canonicalFile
            val canonicalRequested = File(sandboxRoot, normalizedPath).canonicalFile
            val sandboxPrefix = canonicalSandbox.path + File.separator

            // Boundary-safe containment: a sibling such as "$sandbox-evil" must not pass
            if (canonicalRequested.path != canonicalSandbox.path &&
                !canonicalRequested.path.startsWith(sandboxPrefix)
            ) {
                return null
            }

            canonicalRequested
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Resolves [path] or throws, for tool workers that must fail loudly instead of returning null.
     */
    fun resolveOrThrow(sandboxRoot: File, path: String): File =
        resolve(sandboxRoot, path)
            ?: throw IllegalArgumentException(
                "Invalid path: '$path' is empty, absolute, or resolves outside the workspace sandbox"
            )
}

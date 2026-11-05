package com.example.domain.engine

import com.apex.core.port.ApprovalPort
import com.apex.core.port.model.ApprovalDecision
import com.apex.core.port.model.ApprovalRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class AndroidApprovalPort(
    private val onApprovalRequested: (ApprovalRequest) -> Unit,
    private val getFileContent: ((String) -> String?)? = null
) : ApprovalPort {
    private val pendingDeferreds = ConcurrentHashMap<String, CompletableDeferred<ApprovalDecision>>()

    override fun requestApproval(request: ApprovalRequest): ApprovalDecision {
        // For write_file, fetch current file content and generate diff
        val enhancedRequest = if (request.toolName == "write_file" && getFileContent != null) {
            val path = request.arguments["path"]
            val newContent = request.arguments["content"]
            if (path != null && newContent != null) {
                val oldContent = getFileContent(path)
                if (oldContent != null && oldContent != newContent) {
                    val diff = DiffUtil.generateDiff(oldContent, newContent)
                    val diffSummary = DiffUtil.generateDiffSummary(oldContent, newContent)
                    val enhancedReason = "${request.reason}\n\n$diffSummary"
                    // Create enhanced request with diff info in arguments
                    val enhancedArgs = request.arguments.toMutableMap()
                    enhancedArgs["_diff_summary"] = diffSummary
                    enhancedArgs["_old_content"] = oldContent
                    enhancedArgs["_new_content"] = newContent
                    ApprovalRequest(
                        request.requestId,
                        request.toolName,
                        enhancedArgs,
                        request.riskLevel,
                        enhancedReason
                    )
                } else {
                    request
                }
            } else {
                request
            }
        } else {
            request
        }
        
        val deferred = CompletableDeferred<ApprovalDecision>()
        pendingDeferreds[enhancedRequest.requestId] = deferred
        onApprovalRequested(enhancedRequest)
        return runBlocking {
            deferred.await()
        }
    }

    fun provideDecision(requestId: String, approved: Boolean, comment: String = "") {
        val deferred = pendingDeferreds.remove(requestId)
        if (deferred != null) {
            val decision = ApprovalDecision(requestId, approved, comment)
            deferred.complete(decision)
        }
    }
}

package com.example.domain.engine

import com.apex.core.port.ApprovalPort
import com.apex.core.port.model.ApprovalDecision
import com.apex.core.port.model.ApprovalRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class AndroidApprovalPort(
    private val onApprovalRequested: (ApprovalRequest) -> Unit
) : ApprovalPort {
    private val pendingDeferreds = ConcurrentHashMap<String, CompletableDeferred<ApprovalDecision>>()

    override fun requestApproval(request: ApprovalRequest): ApprovalDecision {
        val deferred = CompletableDeferred<ApprovalDecision>()
        pendingDeferreds[request.requestId] = deferred
        onApprovalRequested(request)
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

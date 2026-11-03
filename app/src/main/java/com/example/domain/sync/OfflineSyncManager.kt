package com.example.domain.sync

import com.example.data.local.OfflineQueueDao
import com.example.data.model.PendingActionEntity
import com.example.domain.engine.AgentOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Manages offline-first action queuing and background synchronization replay upon network recovery.
 */
class OfflineSyncManager(
    private val offlineQueueDao: OfflineQueueDao,
    private val agentOrchestrator: AgentOrchestrator? = null,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val pendingCountFlow: Flow<Int> = offlineQueueDao.getPendingCountFlow()

    suspend fun enqueueChatMessage(prompt: String, systemInstruction: String): String {
        val actionId = "act_" + System.currentTimeMillis() + "_" + (1000..9999).random()
        val payload = JSONObject().apply {
            put("prompt", prompt)
            put("systemInstruction", systemInstruction)
        }.toString()

        val entity = PendingActionEntity(
            actionId = actionId,
            actionType = "CHAT_MESSAGE",
            payloadJson = payload
        )
        offlineQueueDao.enqueueAction(entity)
        return actionId
    }

    suspend fun syncPendingActions(): Int {
        val pendingList = offlineQueueDao.getPendingActionsList()
        var syncedCount = 0

        for (action in pendingList) {
            try {
                offlineQueueDao.updateStatus(action.actionId, "SYNCING")
                if (action.actionType == "CHAT_MESSAGE" && agentOrchestrator != null) {
                    val json = JSONObject(action.payloadJson)
                    val prompt = json.optString("prompt", "")
                    val sysInst = json.optString("systemInstruction", "")
                    if (prompt.isNotBlank()) {
                        agentOrchestrator.orchestrateTurn(prompt, sysInst)
                    }
                }
                offlineQueueDao.updateStatus(action.actionId, "COMPLETED")
                offlineQueueDao.deleteAction(action.actionId)
                syncedCount++
            } catch (e: Exception) {
                offlineQueueDao.updateStatus(action.actionId, "PENDING")
            }
        }
        return syncedCount
    }

    fun triggerBackgroundSync() {
        externalScope.launch {
            syncPendingActions()
        }
    }
}

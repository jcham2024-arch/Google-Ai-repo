package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey
    val actionId: String,
    val actionType: String, // "CHAT_MESSAGE", "TOOL_CALL"
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "PENDING" // "PENDING", "SYNCING", "COMPLETED", "FAILED"
)

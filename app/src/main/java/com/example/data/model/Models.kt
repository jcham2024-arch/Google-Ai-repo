package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_cards")
data class AgentCard(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val preferredProvider: String,
    val preferredModel: String,
    val toolsJson: String = "[]",
    val isNarrator: Boolean = false,
    val isMasterAgent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val chatId: String = "default",
    val senderName: String = "User",
    val content: String,
    val isUser: Boolean = true,
    val providerUsed: String = "Apex",
    val modelUsed: String = "Default",
    val hasSearchGrounding: Boolean = false,
    val pendingFileEdit: String? = null,
    val isFileEditApproved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val imageBase64: String? = null,  // Base64 encoded image for multimodal
    val imageMimeType: String? = null,  // e.g., "image/png", "image/jpeg"
    val toolCallsJson: String? = null  // JSON array of tool calls executed for this message
)

@Entity(tableName = "workspace_files")
data class WorkspaceFile(
    @PrimaryKey val path: String,
    val name: String,
    val content: String,
    val isDirectory: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0,
    val version: Int = 1
)

@Entity(tableName = "workspace_file_versions")
data class WorkspaceFileVersion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val content: String,
    val sizeBytes: Long,
    val version: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val changeType: String  // "CREATE", "MODIFY", "DELETE"
)

@Entity(tableName = "lorebooks")
data class Lorebook(
    @PrimaryKey val id: String,
    val title: String,
    val keywords: String,
    val content: String,
    val isConstant: Boolean = false
)

@Entity(tableName = "memory_items")
data class MemoryItem(
    @PrimaryKey val id: String,
    val textContent: String,
    val embeddingJson: String,  // JSON array of floats
    val timestamp: Long = System.currentTimeMillis(),
    val agentId: String = "default"
)

@Entity(tableName = "provider_configs")
data class ProviderConfig(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val defaultModel: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "chat_conversations")
data class ChatConversation(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val lastMessagePreview: String = "",
    val systemInstruction: String = "You are Apex Agent, a general-purpose AI assistant."
)

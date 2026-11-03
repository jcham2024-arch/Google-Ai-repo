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
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderName: String,
    val content: String,
    val isUser: Boolean,
    val providerUsed: String,
    val modelUsed: String,
    val hasSearchGrounding: Boolean = false,
    val pendingFileEdit: String? = null,
    val isFileEditApproved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "workspace_files")
data class WorkspaceFile(
    @PrimaryKey val path: String,
    val name: String,
    val content: String,
    val isDirectory: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "lorebooks")
data class Lorebook(
    @PrimaryKey val id: String,
    val title: String,
    val keywords: String,
    val content: String,
    val isConstant: Boolean = false
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

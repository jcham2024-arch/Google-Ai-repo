package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AgentCard
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.model.Lorebook
import com.example.data.model.MemoryItem
import com.example.data.model.PendingActionEntity
import com.example.data.model.ProviderConfig
import com.example.data.model.WorkspaceFile
import com.example.data.model.WorkspaceFileVersion
import kotlinx.coroutines.flow.Flow
import java.util.List

@Dao
interface ApexDao {
    @Query("SELECT * FROM agent_cards")
    fun getAllAgents(): Flow<List<AgentCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentCard)

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("SELECT * FROM workspace_files")
    fun getAllWorkspaceFiles(): Flow<List<WorkspaceFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceFile(file: WorkspaceFile)

    @Query("DELETE FROM workspace_files WHERE path = :path")
    suspend fun deleteWorkspaceFile(path: String)

    @Query("SELECT * FROM lorebooks")
    fun getAllLorebooks(): Flow<List<Lorebook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLorebook(lorebook: Lorebook)

    @Query("SELECT * FROM provider_configs")
    fun getAllProviders(): Flow<List<ProviderConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ProviderConfig)

    @Query("SELECT * FROM agent_cards WHERE isNarrator = 1 LIMIT 1")
    fun getNarratorAgent(): Flow<AgentCard?>

    @Query("SELECT * FROM agent_cards WHERE id = :id LIMIT 1")
    suspend fun getAgentById(id: String): AgentCard?

    // Memory operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryItem(item: MemoryItem)

    @Query("SELECT * FROM memory_items WHERE agentId = :agentId ORDER BY timestamp DESC LIMIT :limit")
    fun getMemoryItems(agentId: String, limit: Int): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memory_items WHERE agentId = :agentId ORDER BY timestamp DESC")
    fun getAllMemoryItems(agentId: String): Flow<List<MemoryItem>>

    @Query("DELETE FROM memory_items WHERE agentId = :agentId")
    suspend fun clearMemory(agentId: String)

    @Query("UPDATE agent_cards SET preferredProvider = :provider, preferredModel = :model, systemPrompt = :systemPrompt WHERE isNarrator = 1")
    suspend fun updateNarratorSettings(provider: String, model: String, systemPrompt: String)

    // Conversation operations
    @Query("SELECT * FROM chat_conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ChatConversation>>

    @Query("SELECT * FROM chat_conversations WHERE id = :id LIMIT 1")
    suspend fun getConversation(id: String): ChatConversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ChatConversation)

    @Query("DELETE FROM chat_conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("UPDATE chat_conversations SET title = :title, updatedAt = :updatedAt, messageCount = :messageCount, lastMessagePreview = :lastMessagePreview WHERE id = :id")
    suspend fun updateConversation(id: String, title: String, updatedAt: Long, messageCount: Int, lastMessagePreview: String)

    // Workspace file version history
    @Query("SELECT * FROM workspace_file_versions WHERE filePath = :filePath ORDER BY version DESC")
    fun getFileVersions(filePath: String): Flow<List<WorkspaceFileVersion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileVersion(version: WorkspaceFileVersion)

    @Query("DELETE FROM workspace_file_versions WHERE filePath = :filePath")
    suspend fun deleteFileVersions(filePath: String)

    @Query("SELECT * FROM workspace_files ORDER BY path ASC")
    fun getAllWorkspaceFiles(): Flow<List<WorkspaceFile>>
}

@Database(entities = [AgentCard::class, ChatMessage::class, WorkspaceFile::class, Lorebook::class, ProviderConfig::class, PendingActionEntity::class, MemoryItem::class, ChatConversation::class, WorkspaceFileVersion::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun apexDao(): ApexDao
    abstract fun offlineQueueDao(): OfflineQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apex_master_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

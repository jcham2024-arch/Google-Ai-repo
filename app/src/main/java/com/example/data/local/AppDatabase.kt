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
import com.example.data.model.ChatMessage
import com.example.data.model.Lorebook
import com.example.data.model.ProviderConfig
import com.example.data.model.WorkspaceFile
import kotlinx.coroutines.flow.Flow

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

    @Query("UPDATE agent_cards SET preferredProvider = :provider, preferredModel = :model, systemPrompt = :systemPrompt WHERE isNarrator = 1")
    suspend fun updateNarratorSettings(provider: String, model: String, systemPrompt: String)
}

@Database(entities = [AgentCard::class, ChatMessage::class, WorkspaceFile::class, Lorebook::class, ProviderConfig::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun apexDao(): ApexDao

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

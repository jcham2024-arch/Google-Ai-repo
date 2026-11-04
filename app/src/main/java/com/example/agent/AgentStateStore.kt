package com.example.agent

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "agent_state_store")

data class AgentState(
    val sessionId: String,
    val turnIndex: Int = 0,
    val messages: List<String> = emptyList(),
    val pendingToolCall: String? = null,
    val status: Status = Status.RUNNING,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val idempotencyKeys: List<String> = emptyList()
) {
    enum class Status { RUNNING, WAITING_NETWORK, DONE, FAILED }
}

class AgentStateStore(private val context: Context) {
    private val gson = Gson()
    private val keyState = stringPreferencesKey("current_agent_state")

    suspend fun load(): AgentState? {
        val json = context.dataStore.data.map { prefs -> prefs[keyState] }.first()
        return if (!json.isNullOrBlank()) {
            try {
                gson.fromJson(json, AgentState::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    suspend fun save(state: AgentState) {
        val json = gson.toJson(state)
        context.dataStore.edit { prefs ->
            prefs[keyState] = json
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(keyState)
        }
    }
}

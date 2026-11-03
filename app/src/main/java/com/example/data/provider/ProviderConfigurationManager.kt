package com.example.data.provider

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

val Context.providerDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_provider_settings_datastore")

class ProviderConfigurationManager(private val context: Context) {

    private val keyStoreManager = AndroidKeyStoreManager()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private val KEY_PROVIDERS_JSON = stringPreferencesKey("providers_json")
        private val KEY_ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")
    }

    val providersFlow: Flow<List<ProviderSettings>> = context.providerDataStore.data.map { preferences ->
        val jsonStr = preferences[KEY_PROVIDERS_JSON]
        val activeId = preferences[KEY_ACTIVE_PROVIDER_ID] ?: "gemini_compat"

        if (jsonStr.isNullOrBlank()) {
            ProviderPresets.PRESETS.map { it.copy(isActive = it.id == activeId) }
        } else {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<ProviderSettings>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val encryptedKey = obj.optString("apiKey", "")
                    val decryptedKey = if (encryptedKey.isNotBlank()) keyStoreManager.decrypt(encryptedKey).ifBlank { encryptedKey } else ""
                    list.add(
                        ProviderSettings(
                            id = id,
                            name = obj.getString("name"),
                            baseUrl = obj.getString("baseUrl"),
                            apiKey = decryptedKey,
                            defaultModel = obj.optString("defaultModel", "default-model"),
                            isActive = id == activeId
                        )
                    )
                }
                if (list.isEmpty()) {
                    ProviderPresets.PRESETS.map { it.copy(isActive = it.id == activeId) }
                } else {
                    list
                }
            } catch (e: Exception) {
                ProviderPresets.PRESETS.map { it.copy(isActive = it.id == activeId) }
            }
        }
    }

    val activeProviderFlow: Flow<ProviderSettings?> = providersFlow.map { list ->
        list.find { it.isActive } ?: list.firstOrNull()
    }

    suspend fun saveProvider(provider: ProviderSettings) = withContext(Dispatchers.IO) {
        context.providerDataStore.edit { preferences ->
            val jsonStr = preferences[KEY_PROVIDERS_JSON]
            val currentList = if (jsonStr.isNullOrBlank()) {
                ProviderPresets.PRESETS.toMutableList()
            } else {
                try {
                    val array = JSONArray(jsonStr)
                    val list = mutableListOf<ProviderSettings>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = obj.getString("id")
                        val encryptedKey = obj.optString("apiKey", "")
                        val decryptedKey = if (encryptedKey.isNotBlank()) keyStoreManager.decrypt(encryptedKey).ifBlank { encryptedKey } else ""
                        list.add(
                            ProviderSettings(
                                id = id,
                                name = obj.getString("name"),
                                baseUrl = obj.getString("baseUrl"),
                                apiKey = decryptedKey,
                                defaultModel = obj.optString("defaultModel", "default-model"),
                                isActive = false
                            )
                        )
                    }
                    list
                } catch (e: Exception) {
                    ProviderPresets.PRESETS.toMutableList()
                }
            }

            val index = currentList.indexOfFirst { it.id == provider.id }
            if (index >= 0) {
                currentList[index] = provider
            } else {
                currentList.add(provider)
            }

            val newArray = JSONArray()
            currentList.forEach { p ->
                val encryptedKey = if (p.apiKey.isNotBlank()) keyStoreManager.encrypt(p.apiKey) else ""
                newArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("baseUrl", p.baseUrl)
                    put("apiKey", encryptedKey)
                    put("defaultModel", p.defaultModel)
                })
            }
            preferences[KEY_PROVIDERS_JSON] = newArray.toString()
            if (provider.isActive) {
                preferences[KEY_ACTIVE_PROVIDER_ID] = provider.id
            }
        }
    }

    suspend fun deleteProvider(providerId: String) = withContext(Dispatchers.IO) {
        context.providerDataStore.edit { preferences ->
            val jsonStr = preferences[KEY_PROVIDERS_JSON] ?: return@edit
            try {
                val array = JSONArray(jsonStr)
                val newArray = JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("id") != providerId) {
                        newArray.put(obj)
                    }
                }
                preferences[KEY_PROVIDERS_JSON] = newArray.toString()
                if (preferences[KEY_ACTIVE_PROVIDER_ID] == providerId) {
                    preferences[KEY_ACTIVE_PROVIDER_ID] = "gemini_compat"
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun setActiveProvider(providerId: String) = withContext(Dispatchers.IO) {
        context.providerDataStore.edit { preferences ->
            preferences[KEY_ACTIVE_PROVIDER_ID] = providerId
        }
    }

    suspend fun testConnection(baseUrl: String, apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanBaseUrl = baseUrl.trimEnd('/')

        // Step 1: Try GET {baseUrl}/models
        val modelsUrl = if (cleanBaseUrl.endsWith("/models")) cleanBaseUrl else "$cleanBaseUrl/models"
        val modelsRequest = Request.Builder()
            .url(modelsUrl)
            .get()
            .apply {
                if (apiKey.isNotBlank()) addHeader("Authorization", "Bearer $apiKey")
            }
            .build()

        try {
            httpClient.newCall(modelsRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data")
                    val modelCount = data?.length() ?: 0
                    return@withContext Result.success("Connection Successful! /models returned $modelCount models.")
                }
            }
        } catch (_: Exception) {
            // Fall back to chat completions ping
        }

        // Step 2: Fall back to POST {baseUrl}/chat/completions with max_tokens: 1
        val chatUrl = if (cleanBaseUrl.endsWith("/chat/completions")) cleanBaseUrl else "$cleanBaseUrl/chat/completions"
        val pingBody = JSONObject().apply {
            put("model", model.ifBlank { "gpt-4o" })
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "ping")
                })
            })
            put("max_tokens", 1)
        }

        val chatRequest = Request.Builder()
            .url(chatUrl)
            .post(pingBody.toString().toRequestBody("application/json".toMediaType()))
            .apply {
                if (apiKey.isNotBlank()) addHeader("Authorization", "Bearer $apiKey")
            }
            .build()

        try {
            httpClient.newCall(chatRequest).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext Result.success("Connection Successful! Ping test passed (${response.code}).")
                } else {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $err"))
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
}

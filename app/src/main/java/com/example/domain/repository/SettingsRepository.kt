package com.example.domain.repository

import com.example.data.provider.ProviderSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getProviders(): Flow<List<ProviderSettings>>
    fun getActiveProvider(): Flow<ProviderSettings?>
    suspend fun saveProvider(provider: ProviderSettings)
    suspend fun updateApiKey(providerId: String, apiKey: String)
    suspend fun deleteProvider(providerId: String)
    suspend fun setActiveProvider(providerId: String)
    suspend fun testConnection(baseUrl: String, apiKey: String, model: String): Result<String>
}

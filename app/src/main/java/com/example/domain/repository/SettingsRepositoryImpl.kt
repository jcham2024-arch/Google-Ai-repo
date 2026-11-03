package com.example.domain.repository

import com.example.data.provider.ProviderConfigurationManager
import com.example.data.provider.ProviderSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val configManager: ProviderConfigurationManager
) : SettingsRepository {

    override fun getProviders(): Flow<List<ProviderSettings>> {
        return configManager.providersFlow
    }

    override fun getActiveProvider(): Flow<ProviderSettings?> {
        return configManager.activeProviderFlow
    }

    override suspend fun saveProvider(provider: ProviderSettings) {
        configManager.saveProvider(provider)
    }

    override suspend fun updateApiKey(providerId: String, apiKey: String) {
        configManager.updateApiKey(providerId, apiKey)
    }

    override suspend fun deleteProvider(providerId: String) {
        configManager.deleteProvider(providerId)
    }

    override suspend fun setActiveProvider(providerId: String) {
        configManager.setActiveProvider(providerId)
    }

    override suspend fun testConnection(baseUrl: String, apiKey: String, model: String): Result<String> {
        return configManager.testConnection(baseUrl, apiKey, model)
    }
}

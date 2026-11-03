package com.example.domain.usecase

import com.example.data.provider.ProviderSettings
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class LoadSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    fun getProviders(): Flow<List<ProviderSettings>> = settingsRepository.getProviders()

    fun getActiveProvider(): Flow<ProviderSettings?> = settingsRepository.getActiveProvider()

    suspend fun saveProvider(provider: ProviderSettings) {
        settingsRepository.saveProvider(provider)
    }

    suspend fun setActiveProvider(providerId: String) {
        settingsRepository.setActiveProvider(providerId)
    }

    suspend fun testConnection(baseUrl: String, apiKey: String, model: String): Result<String> {
        return settingsRepository.testConnection(baseUrl, apiKey, model)
    }
}

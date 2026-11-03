package com.example.domain.engine

import com.example.data.provider.ProviderConfigurationManager
import com.example.data.provider.ProviderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProviderRegistry(
    private val configManager: ProviderConfigurationManager,
    private val scope: CoroutineScope
) {
    private val _activeProvider = MutableStateFlow<ProviderSettings?>(null)
    val activeProvider: StateFlow<ProviderSettings?> = _activeProvider.asStateFlow()

    private val _allProviders = MutableStateFlow<List<ProviderSettings>>(emptyList())
    val allProviders: StateFlow<List<ProviderSettings>> = _allProviders.asStateFlow()

    init {
        scope.launch {
            configManager.providersFlow.collect { list ->
                _allProviders.value = list
            }
        }
        scope.launch {
            configManager.activeProviderFlow.collect { active ->
                _activeProvider.value = active
            }
        }
    }

    fun getProvider(id: String): ProviderSettings? {
        return _allProviders.value.find { it.id == id }
    }

    suspend fun selectActiveProvider(id: String) {
        configManager.setActiveProvider(id)
    }

    suspend fun registerOrUpdateProvider(settings: ProviderSettings) {
        configManager.saveProvider(settings)
    }

    suspend fun removeProvider(id: String) {
        configManager.deleteProvider(id)
    }

    suspend fun testConnection(baseUrl: String, apiKey: String, model: String): Result<String> {
        return configManager.testConnection(baseUrl, apiKey, model)
    }
}

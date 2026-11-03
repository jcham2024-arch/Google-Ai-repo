package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.provider.ProviderSettings
import com.example.domain.repository.SettingsRepository
import com.example.domain.usecase.LoadSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val loadSettingsUseCase: LoadSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeProviders()
        observeActiveProvider()
    }

    private fun observeProviders() {
        viewModelScope.launch {
            loadSettingsUseCase.getProviders()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(providers = list) }
                }
        }
    }

    private fun observeActiveProvider() {
        viewModelScope.launch {
            loadSettingsUseCase.getActiveProvider()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { active ->
                    _uiState.update { it.copy(activeProvider = active) }
                }
        }
    }

    fun saveProvider(provider: ProviderSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.saveProvider(provider)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun selectActiveProvider(providerId: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setActiveProvider(providerId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteProvider(providerId: String) {
        viewModelScope.launch {
            try {
                settingsRepository.deleteProvider(providerId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun testConnection(baseUrl: String, apiKey: String, model: String) {
        _uiState.update { it.copy(isTestingConnection = true, testConnectionResult = null) }
        viewModelScope.launch {
            val result = settingsRepository.testConnection(baseUrl, apiKey, model)
            _uiState.update { state ->
                state.copy(
                    isTestingConnection = false,
                    testConnectionResult = if (result.isSuccess) result.getOrNull() else "Connection Failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun clearTestResult() {
        _uiState.update { it.copy(testConnectionResult = null, errorMessage = null) }
    }
}

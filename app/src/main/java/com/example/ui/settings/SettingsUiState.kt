package com.example.ui.settings

import com.example.data.provider.ProviderSettings

data class SettingsUiState(
    val providers: List<ProviderSettings> = emptyList(),
    val activeProvider: ProviderSettings? = null,
    val isTestingConnection: Boolean = false,
    val testConnectionResult: String? = null,
    val errorMessage: String? = null
)

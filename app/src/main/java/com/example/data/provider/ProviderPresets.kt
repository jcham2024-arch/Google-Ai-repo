package com.example.data.provider

data class ProviderSettings(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val defaultModel: String = "default-model",
    val isActive: Boolean = false
)

object ProviderPresets {
    val PRESETS = listOf(
        ProviderSettings(
            id = "gemini_compat",
            name = "Gemini (OpenAI Compatible)",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            apiKey = "",
            defaultModel = "gemini-2.5-flash",
            isActive = true
        ),
        ProviderSettings(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            apiKey = "",
            defaultModel = "meta-llama/llama-3.3-70b-instruct"
        ),
        ProviderSettings(
            id = "groq",
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            apiKey = "",
            defaultModel = "llama-3.3-70b-versatile"
        ),
        ProviderSettings(
            id = "openai",
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            apiKey = "",
            defaultModel = "gpt-4o"
        ),
        ProviderSettings(
            id = "nvidia_nim",
            name = "NVIDIA NIM",
            baseUrl = "https://integrate.api.nvidia.com/v1",
            apiKey = "",
            defaultModel = "meta/llama-3.3-70b-instruct"
        ),
        ProviderSettings(
            id = "ollama_local",
            name = "Ollama (Local)",
            baseUrl = "http://10.0.2.2:11434/v1",
            apiKey = "",
            defaultModel = "llama3"
        ),
        ProviderSettings(
            id = "lm_studio_local",
            name = "LM Studio (Local)",
            baseUrl = "http://10.0.2.2:1234/v1",
            apiKey = "",
            defaultModel = "local-model"
        ),
        ProviderSettings(
            id = "custom",
            name = "Custom",
            baseUrl = "https://api.example.com/v1",
            apiKey = "",
            defaultModel = "default-model"
        )
    )
}

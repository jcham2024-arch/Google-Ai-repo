package com.example.data.provider

data class ProviderSettings(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val defaultModel: String = "default-model",
    val maxTokens: Int = 4096,
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
            maxTokens = 65536,
            isActive = true
        ),
        ProviderSettings(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            apiKey = "",
            defaultModel = "meta-llama/llama-3.3-70b-instruct",
            maxTokens = 8192
        ),
        ProviderSettings(
            id = "groq",
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            apiKey = "",
            defaultModel = "llama-3.3-70b-versatile",
            maxTokens = 8192
        ),
        ProviderSettings(
            id = "deepseek",
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1",
            apiKey = "",
            defaultModel = "deepseek-chat",
            maxTokens = 8192
        ),
        ProviderSettings(
            id = "openai",
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            apiKey = "",
            defaultModel = "gpt-4o",
            maxTokens = 4096
        ),
        ProviderSettings(
            id = "nvidia_nim",
            name = "NVIDIA NIM",
            baseUrl = "https://integrate.api.nvidia.com/v1",
            apiKey = "",
            defaultModel = "meta/llama-3.3-70b-instruct",
            maxTokens = 4096
        ),
        ProviderSettings(
            id = "ollama_local",
            name = "Ollama (Local)",
            baseUrl = "http://10.0.2.2:11434/v1",
            apiKey = "ollama",
            defaultModel = "llama3",
            maxTokens = 4096
        )
    )
}

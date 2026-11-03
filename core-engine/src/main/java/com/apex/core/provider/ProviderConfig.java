package com.apex.core.provider;

import java.util.Objects;

/**
 * Immutable configuration entity for an AI model provider in the core engine.
 */
public final class ProviderConfig {
    private final String id;
    private final String name;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final boolean active;

    public ProviderConfig(String id, String name, String baseUrl, String apiKey, String defaultModel, boolean active) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.baseUrl = baseUrl != null ? baseUrl.trim().replaceAll("/+$", "") : "";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.defaultModel = defaultModel != null ? defaultModel.trim() : "default-model";
        this.active = active;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getDefaultModel() { return defaultModel; }
    public boolean isActive() { return active; }

    public ProviderConfig withApiKey(String newApiKey) {
        return new ProviderConfig(this.id, this.name, this.baseUrl, newApiKey, this.defaultModel, this.active);
    }

    public ProviderConfig withActive(boolean newActive) {
        return new ProviderConfig(this.id, this.name, this.baseUrl, this.apiKey, this.defaultModel, newActive);
    }

    public ProviderConfig withModel(String newModel) {
        return new ProviderConfig(this.id, this.name, this.baseUrl, this.apiKey, newModel, this.active);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderConfig that = (ProviderConfig) o;
        return active == that.active &&
               Objects.equals(id, that.id) &&
               Objects.equals(name, that.name) &&
               Objects.equals(baseUrl, that.baseUrl) &&
               Objects.equals(apiKey, that.apiKey) &&
               Objects.equals(defaultModel, that.defaultModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, baseUrl, apiKey, defaultModel, active);
    }

    @Override
    public String toString() {
        return "ProviderConfig{id='" + id + "', name='" + name + "', baseUrl='" + baseUrl + "', model='" + defaultModel + "', active=" + active + "}";
    }
}

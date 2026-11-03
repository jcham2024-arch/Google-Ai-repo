package com.apex.core.provider;

import com.apex.core.json.MiniJson;
import com.apex.core.port.StoragePort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages persistence of multiple AI provider configurations in the core engine.
 * <p>
 * Delegates key-value storage to {@link StoragePort} (which is implemented on Android by Jetpack
 * DataStore in the {@code :app} module). Synchronizes all updates with {@link ProviderRegistry}
 * for dynamic injection into the Narrator orchestrator.
 */
public class ProviderConfigurationManager {
    public static final String KEY_PROVIDERS_PREFIX = "provider_config_";
    public static final String KEY_ACTIVE_PROVIDER = "provider_active_id";

    private final StoragePort storagePort;
    private final ProviderRegistry providerRegistry;

    public ProviderConfigurationManager(StoragePort storagePort, ProviderRegistry providerRegistry) {
        this.storagePort = storagePort;
        this.providerRegistry = providerRegistry;
        loadProviders();
    }

    /**
     * Loads persisted providers from the underlying storage port and registers them into the registry.
     */
    public synchronized List<ProviderConfig> loadProviders() {
        if (storagePort == null) {
            return Collections.emptyList();
        }

        List<String> keys = storagePort.listKeys(KEY_PROVIDERS_PREFIX);
        String activeId = storagePort.get(KEY_ACTIVE_PROVIDER).orElse("");
        List<ProviderConfig> loaded = new ArrayList<>();

        for (String key : keys) {
            Optional<String> valOpt = storagePort.get(key);
            if (valOpt.isPresent()) {
                String json = valOpt.get();
                String id = MiniJson.extractField(json, "id");
                String name = MiniJson.extractField(json, "name");
                String baseUrl = MiniJson.extractField(json, "baseUrl");
                String apiKey = MiniJson.extractField(json, "apiKey");
                String model = MiniJson.extractField(json, "defaultModel");
                boolean isActive = id.equals(activeId);

                if (!id.isEmpty()) {
                    ProviderConfig config = new ProviderConfig(id, name, baseUrl, apiKey, model, isActive);
                    loaded.add(config);
                }
            }
        }

        if (providerRegistry != null && !loaded.isEmpty()) {
            providerRegistry.registerAll(loaded);
            if (!activeId.isEmpty()) {
                providerRegistry.setActiveProvider(activeId);
            }
        }

        return Collections.unmodifiableList(loaded);
    }

    /**
     * Persists or updates a provider configuration and updates the central registry.
     */
    public synchronized void saveProvider(ProviderConfig provider) {
        if (provider == null || storagePort == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("id", provider.getId());
        map.put("name", provider.getName());
        map.put("baseUrl", provider.getBaseUrl());
        map.put("apiKey", provider.getApiKey());
        map.put("defaultModel", provider.getDefaultModel());
        map.put("active", provider.isActive());

        String json = MiniJson.toJson(map);
        storagePort.save(KEY_PROVIDERS_PREFIX + provider.getId(), json);

        if (provider.isActive()) {
            storagePort.save(KEY_ACTIVE_PROVIDER, provider.getId());
        }

        if (providerRegistry != null) {
            providerRegistry.registerProvider(provider);
        }
    }

    /**
     * Updates only the API key for an existing provider and synchronizes storage.
     */
    public synchronized void updateApiKey(String providerId, String newApiKey) {
        if (providerId == null || storagePort == null) return;

        Optional<String> valOpt = storagePort.get(KEY_PROVIDERS_PREFIX + providerId);
        if (valOpt.isPresent()) {
            String json = valOpt.get();
            String name = MiniJson.extractField(json, "name");
            String baseUrl = MiniJson.extractField(json, "baseUrl");
            String model = MiniJson.extractField(json, "defaultModel");
            String activeId = storagePort.get(KEY_ACTIVE_PROVIDER).orElse("");
            boolean isActive = providerId.equals(activeId);

            ProviderConfig updated = new ProviderConfig(providerId, name, baseUrl, newApiKey, model, isActive);
            saveProvider(updated);
        } else if (providerRegistry != null) {
            providerRegistry.updateApiKey(providerId, newApiKey);
        }
    }

    /**
     * Sets the active provider ID in storage and registry.
     */
    public synchronized void setActiveProvider(String providerId) {
        if (storagePort != null) {
            storagePort.save(KEY_ACTIVE_PROVIDER, providerId);
        }
        if (providerRegistry != null) {
            providerRegistry.setActiveProvider(providerId);
        }
    }

    /**
     * Deletes a provider from storage and removes it from the central registry.
     */
    public synchronized void deleteProvider(String providerId) {
        if (storagePort != null) {
            storagePort.delete(KEY_PROVIDERS_PREFIX + providerId);
            String activeId = storagePort.get(KEY_ACTIVE_PROVIDER).orElse("");
            if (providerId.equals(activeId)) {
                storagePort.delete(KEY_ACTIVE_PROVIDER);
            }
        }
        if (providerRegistry != null) {
            providerRegistry.removeProvider(providerId);
        }
    }

    public ProviderRegistry getProviderRegistry() {
        return providerRegistry;
    }
}

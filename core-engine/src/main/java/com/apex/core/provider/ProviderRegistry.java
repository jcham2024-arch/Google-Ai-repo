package com.apex.core.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central registry in the core engine managing active AI providers and dynamically
 * injecting them into orchestrators and clients.
 */
public class ProviderRegistry {
    private final Map<String, ProviderConfig> providers = new ConcurrentHashMap<>();
    private final List<ProviderChangeListener> listeners = new CopyOnWriteArrayList<>();
    private volatile String activeProviderId = "";

    public interface ProviderChangeListener {
        void onActiveProviderChanged(ProviderConfig activeProvider);
        void onProvidersChanged(List<ProviderConfig> allProviders);
    }

    public void addListener(ProviderChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ProviderChangeListener listener) {
        listeners.remove(listener);
    }

    public void registerProvider(ProviderConfig config) {
        if (config == null) return;
        providers.put(config.getId(), config);
        if (config.isActive() || activeProviderId.isEmpty()) {
            activeProviderId = config.getId();
        }
        notifyListeners();
    }

    public void registerAll(List<ProviderConfig> configList) {
        if (configList == null) return;
        for (ProviderConfig config : configList) {
            if (config != null) {
                providers.put(config.getId(), config);
                if (config.isActive()) {
                    activeProviderId = config.getId();
                }
            }
        }
        if (activeProviderId.isEmpty() && !providers.isEmpty()) {
            activeProviderId = providers.keySet().iterator().next();
        }
        notifyListeners();
    }

    public Optional<ProviderConfig> getProvider(String id) {
        return Optional.ofNullable(providers.get(id));
    }

    public Optional<ProviderConfig> getActiveProvider() {
        ProviderConfig active = providers.get(activeProviderId);
        if (active != null) {
            return Optional.of(active);
        }
        return providers.values().stream().findFirst();
    }

    public boolean setActiveProvider(String id) {
        if (providers.containsKey(id)) {
            activeProviderId = id;
            for (Map.Entry<String, ProviderConfig> entry : providers.entrySet()) {
                boolean isCurrent = entry.getKey().equals(id);
                providers.put(entry.getKey(), entry.getValue().withActive(isCurrent));
            }
            notifyListeners();
            return true;
        }
        return false;
    }

    public void updateApiKey(String id, String newApiKey) {
        ProviderConfig existing = providers.get(id);
        if (existing != null) {
            ProviderConfig updated = existing.withApiKey(newApiKey);
            providers.put(id, updated);
            notifyListeners();
        }
    }

    public void removeProvider(String id) {
        providers.remove(id);
        if (activeProviderId.equals(id)) {
            activeProviderId = providers.isEmpty() ? "" : providers.keySet().iterator().next();
        }
        notifyListeners();
    }

    public List<ProviderConfig> getAllProviders() {
        return Collections.unmodifiableList(new ArrayList<>(providers.values()));
    }

    private void notifyListeners() {
        Optional<ProviderConfig> activeOpt = getActiveProvider();
        List<ProviderConfig> all = getAllProviders();
        for (ProviderChangeListener listener : listeners) {
            activeOpt.ifPresent(listener::onActiveProviderChanged);
            listener.onProvidersChanged(all);
        }
    }
}

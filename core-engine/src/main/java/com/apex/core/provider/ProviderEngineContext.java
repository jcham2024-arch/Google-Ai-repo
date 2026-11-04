package com.apex.core.provider;

import com.apex.core.port.StoragePort;

import java.util.Optional;

/**
 * Central singleton DI container and provider factory for the core engine,
 * enabling safe initialization and injection of {@link ProviderConfigurationManager}
 * and {@link ProviderRegistry} across modules.
 */
public final class ProviderEngineContext {
    private static volatile ProviderRegistry registryInstance;
    private static volatile ProviderConfigurationManager managerInstance;

    private ProviderEngineContext() {}

    public static synchronized ProviderRegistry getProviderRegistry() {
        if (registryInstance == null) {
            registryInstance = new ProviderRegistry();
        }
        return registryInstance;
    }

    public static synchronized ProviderConfigurationManager getConfigurationManager(StoragePort storagePort) {
        if (managerInstance == null) {
            managerInstance = new ProviderConfigurationManager(storagePort, getProviderRegistry());
        }
        return managerInstance;
    }

    public static synchronized void resetForTesting() {
        registryInstance = null;
        managerInstance = null;
    }
}

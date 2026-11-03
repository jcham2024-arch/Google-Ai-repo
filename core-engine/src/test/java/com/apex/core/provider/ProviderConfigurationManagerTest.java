package com.apex.core.provider;

import com.apex.core.port.StoragePort;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying CRUD operations, persistence via StoragePort, and dynamic registry synchronization
 * in {@link ProviderConfigurationManager} and {@link ProviderRegistry}.
 */
public class ProviderConfigurationManagerTest {

    private StoragePort fakeStoragePort;
    private ProviderRegistry providerRegistry;
    private ProviderConfigurationManager manager;

    @Before
    public void setUp() {
        Map<String, String> storage = new HashMap<>();

        fakeStoragePort = new StoragePort() {
            @Override
            public void save(String key, String value) {
                storage.put(key, value);
            }

            @Override
            public Optional<String> get(String key) {
                return Optional.ofNullable(storage.get(key));
            }

            @Override
            public void delete(String key) {
                storage.remove(key);
            }

            @Override
            public List<String> listKeys(String prefix) {
                List<String> keys = new ArrayList<>();
                for (String k : storage.keySet()) {
                    if (k.startsWith(prefix)) {
                        keys.add(k);
                    }
                }
                return keys;
            }
        };

        providerRegistry = new ProviderRegistry();
        manager = new ProviderConfigurationManager(fakeStoragePort, providerRegistry);
    }

    @Test
    public void testSaveAndRetrieveProvider() {
        ProviderConfig deepseek = new ProviderConfig("deepseek", "DeepSeek AI", "https://api.deepseek.com/v1", "sk-deepseek-123", "deepseek-chat", true);
        manager.saveProvider(deepseek);

        List<ProviderConfig> loaded = manager.loadProviders();
        assertEquals(1, loaded.size());
        assertEquals("DeepSeek AI", loaded.get(0).getName());
        assertEquals("https://api.deepseek.com/v1", loaded.get(0).getBaseUrl());
        assertEquals("sk-deepseek-123", loaded.get(0).getApiKey());
        assertEquals("deepseek-chat", loaded.get(0).getDefaultModel());
        assertTrue(loaded.get(0).isActive());

        // Verify active provider in registry
        Optional<ProviderConfig> active = providerRegistry.getActiveProvider();
        assertTrue(active.isPresent());
        assertEquals("deepseek", active.get().getId());
    }

    @Test
    public void testUpdateApiKey() {
        ProviderConfig groq = new ProviderConfig("groq", "Groq AI", "https://api.groq.com/openai/v1", "old-key", "llama-3.3-70b-versatile", false);
        manager.saveProvider(groq);

        manager.updateApiKey("groq", "new-groq-key-456");

        List<ProviderConfig> loaded = manager.loadProviders();
        assertEquals(1, loaded.size());
        assertEquals("new-groq-key-456", loaded.get(0).getApiKey());
    }

    @Test
    public void testSetActiveProviderAndListeners() {
        List<String> activeChanges = new ArrayList<>();
        providerRegistry.addListener(new ProviderRegistry.ProviderChangeListener() {
            @Override
            public void onActiveProviderChanged(ProviderConfig activeProvider) {
                activeChanges.add(activeProvider.getId());
            }

            @Override
            public void onProvidersChanged(List<ProviderConfig> allProviders) {}
        });

        ProviderConfig p1 = new ProviderConfig("p1", "P1", "https://p1.com", "k1", "m1", true);
        ProviderConfig p2 = new ProviderConfig("p2", "P2", "https://p2.com", "k2", "m2", false);

        manager.saveProvider(p1);
        manager.saveProvider(p2);

        manager.setActiveProvider("p2");

        Optional<ProviderConfig> active = providerRegistry.getActiveProvider();
        assertTrue(active.isPresent());
        assertEquals("p2", active.get().getId());
        assertTrue(activeChanges.contains("p2"));
    }

    @Test
    public void testDeleteProvider() {
        ProviderConfig p1 = new ProviderConfig("p1", "P1", "https://p1.com", "k1", "m1", true);
        manager.saveProvider(p1);

        manager.deleteProvider("p1");

        List<ProviderConfig> loaded = manager.loadProviders();
        assertTrue(loaded.isEmpty());
        assertFalse(providerRegistry.getProvider("p1").isPresent());
    }
}

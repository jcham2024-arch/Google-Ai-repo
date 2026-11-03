package com.example.data.provider

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.apex.core.port.StoragePort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Optional

/**
 * Adapter implementing the core-engine's [StoragePort] using Android Jetpack DataStore Preferences.
 *
 * This allows the [com.apex.core.provider.ProviderConfigurationManager] in `:core-engine` to persist
 * multiple AI provider configurations seamlessly onto Android DataStore without coupling
 * `:core-engine` directly to Android framework packages.
 */
class DataStoreStorageAdapter(private val context: Context) : StoragePort {

    override fun save(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        runBlocking {
            context.providerDataStore.edit { prefs ->
                prefs[prefKey] = value
            }
        }
    }

    override fun get(key: String): Optional<String> {
        val prefKey = stringPreferencesKey(key)
        val value = runBlocking {
            val prefs = context.providerDataStore.data.first()
            prefs[prefKey]
        }
        return Optional.ofNullable(value)
    }

    override fun delete(key: String) {
        val prefKey = stringPreferencesKey(key)
        runBlocking {
            context.providerDataStore.edit { prefs ->
                prefs.remove(prefKey)
            }
        }
    }

    override fun listKeys(prefix: String): List<String> {
        return runBlocking {
            val prefs = context.providerDataStore.data.first()
            prefs.asMap().keys
                .map { it.name }
                .filter { it.startsWith(prefix) }
        }
    }
}

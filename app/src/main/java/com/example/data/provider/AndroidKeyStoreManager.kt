package com.example.data.provider

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages hardware-backed cryptographic operations using the Android Keystore system.
 *
 * Security Invariants:
 * - Encryption algorithm: AES-256 in Galois/Counter Mode (GCM) with no padding (`AES/GCM/NoPadding`).
 * - Authentication tag: 128-bit authentication tag ensuring ciphertext integrity.
 * - Key storage: Private AES master key is generated and stored exclusively within the device's
 *   hardware-backed Secure Element (SE) or Trusted Execution Environment (TEE).
 * - Key export: The raw key material can never be extracted or exported from the Keystore.
 * - Ciphertext format: Prepends a unique 12-byte initialization vector (IV) to the ciphertext,
 *   encoded as a Base64 string for safe persistence.
 */
class AndroidKeyStoreManager {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "ApexProviderKeyStoreAlias"

    init {
        getOrCreateSecretKey()
    }

    /**
     * Retrieves the existing master key entry from Android Keystore, or generates a new 256-bit AES
     * key inside hardware storage if none exists.
     */
    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts plain text API credentials using AES-GCM.
     *
     * @param plainText cleartext API key or credential string.
     * @return Base64-encoded string containing the 12-byte IV followed by the encrypted ciphertext.
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts a Base64-encoded AES-GCM ciphertext payload.
     *
     * @param cipherText Base64 string containing the 12-byte IV and encrypted ciphertext.
     * @return cleartext string, or empty string if decryption fails or authentication tag is invalid.
     */
    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        try {
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)
            if (combined.size < 12) return ""
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, combined, 0, 12)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            val decrypted = cipher.doFinal(combined, 12, combined.size - 12)
            return String(decrypted, Charsets.UTF_8)
        } catch (_: Exception) {
            return ""
        }
    }
}

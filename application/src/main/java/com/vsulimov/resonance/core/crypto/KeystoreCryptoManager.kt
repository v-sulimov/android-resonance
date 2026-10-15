package com.vsulimov.resonance.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles symmetric encryption and decryption of sensitive strings (e.g., passwords)
 * using the Android KeyStore system.
 *
 * Uses AES/GCM/NoPadding which provides authenticated encryption.
 */
class KeystoreCryptoManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "resonance_credentials_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AUTH_TAG_LENGTH_BITS = 128
        private const val IV_LENGTH_BYTES = 12
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    /**
     * Encrypts a plain text string.
     *
     * @param plainText The string to encrypt.
     * @return A byte array containing the Initialization Vector (IV) followed by the cipher text.
     */
    fun encrypt(plainText: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return iv + cipherText
    }

    /**
     * Decrypts a byte array previously encrypted by [encrypt].
     *
     * @param encryptedData A byte array containing the IV followed by the cipher text.
     * @return The decrypted plain text string.
     */
    fun decrypt(encryptedData: ByteArray): String {
        require(encryptedData.size > IV_LENGTH_BYTES) { "Encrypted data is too short to contain an IV" }

        val iv = encryptedData.copyOfRange(0, IV_LENGTH_BYTES)
        val cipherText = encryptedData.copyOfRange(IV_LENGTH_BYTES, encryptedData.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

        val plainTextBytes = cipher.doFinal(cipherText)
        return String(plainTextBytes, Charsets.UTF_8)
    }

    /**
     * Retrieves the existing [SecretKey] from the Android KeyStore, or creates a new one if it does not exist.
     *
     * @return The [SecretKey] used for encryption and decryption.
     */
    private fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    /**
     * Generates and stores a new AES key in the Android KeyStore.
     * The key is configured strictly for encryption and decryption using GCM block mode
     * without any padding. It requires randomized encryption to be true.
     *
     * @return The newly generated [SecretKey].
     */
    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
}

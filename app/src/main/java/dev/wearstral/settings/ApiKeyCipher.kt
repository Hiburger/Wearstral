package dev.wearstral.settings

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts secrets with an AES-256/GCM key generated
 * inside the android keystore. That data is non-exportable and never leaves
 * device hardware, only ciphertext is ever persisted to disk
 *
 * wire format: Base64 (12-byte random IV || GCM ciphertext + 128-bit tag)
 *
 * singleton on purpose: several instances could race each other and overwrite
 * the Keystore alias which would silently invalidate previously stored data
 */

object ApiKeyCipher {

    private val key: SecretKey by lazy { getOrCreateKey() }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainText.encodeToByteArray())
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size > IV_SIZE_BYTES) { "Encrypted payload is too short" }
        val iv = bytes.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = bytes.copyOfRange(IV_SIZE_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))
        return cipher.doFinal(ciphertext).decodeToString()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        val existing = runCatching {
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        }.getOrNull()
        if (existing != null) return existing
        return generateKey()
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()
        )
        return generator.generateKey()
    }

    private const val PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "wearstral_api_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256
    private const val IV_SIZE_BYTES = 12
    private const val TAG_SIZE_BITS = 128
}

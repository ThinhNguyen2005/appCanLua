package com.giathinh.canlua.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.encoding.Base64

@Singleton
class CccdCrypto @Inject constructor() {
    private val currentCipher = AesGcmCccdCipher(::getOrCreateKey)

    fun encrypt(plainText: String?): String? {
        if (plainText.isNullOrBlank()) return plainText
        return currentCipher.encrypt(plainText)
    }

    fun decrypt(cipherText: String?): String? {
        if (cipherText.isNullOrBlank()) return cipherText
        return if (cipherText.startsWith(AesGcmCccdCipher.PREFIX)) {
            currentCipher.decrypt(cipherText)
        } else {
            LegacyCccdCipher.decryptOrNull(cipherText) ?: cipherText
        }
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "canlua_cccd_aes_gcm_v2"
    }
}

internal class AesGcmCccdCipher(
    private val keyProvider: () -> SecretKey
) {
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return listOf(
            VERSION,
            Base64.encode(cipher.iv),
            Base64.encode(encrypted)
        ).joinToString(":")
    }

    fun decrypt(payload: String): String {
        val parts = payload.split(":", limit = 3)
        require(parts.size == 3 && parts[0] == VERSION) { "Invalid CCCD ciphertext" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            keyProvider(),
            GCMParameterSpec(GCM_TAG_BITS, Base64.decode(parts[1]))
        )
        return String(cipher.doFinal(Base64.decode(parts[2])), Charsets.UTF_8)
    }

    companion object {
        const val PREFIX = "v2:"
        private const val VERSION = "v2"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}

internal object LegacyCccdCipher {
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private val key = SecretKeySpec(
        Base64.decode("Q2FuTHVhQXBwQ2NjZEtleTIwMjZTZWN1cmVLZXlLZXk="),
        "AES"
    )
    private val iv = IvParameterSpec(
        Base64.decode("Q2FuTHVhQXBwSXYyMDI2IQ==")
    )

    fun decryptOrNull(cipherText: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        String(cipher.doFinal(Base64.decode(cipherText)), Charsets.UTF_8)
    }.getOrNull()

    internal fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return Base64.encode(cipher.doFinal(plainText.toByteArray(Charsets.UTF_8)))
    }
}
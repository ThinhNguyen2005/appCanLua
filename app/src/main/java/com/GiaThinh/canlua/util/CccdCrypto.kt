package com.GiaThinh.canlua.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CccdCrypto {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private val keySpec: SecretKeySpec
    private val ivSpec: IvParameterSpec

    init {
        // "CanLuaAppCccdKey2026SecureKey!" (32 bytes) base64 encoded
        val decodedKey = Base64.decode("Q2FuTHVhQXBwQ2NjZEtleTIwMjZTZWN1cmVLZXkh", Base64.DEFAULT)
        keySpec = SecretKeySpec(decodedKey, "AES")
        // "CanLuaAppIv2026!" (16 bytes) base64 encoded
        val decodedIv = Base64.decode("Q2FuTHVhQXBwSXYyMDI2IQ==", Base64.DEFAULT)
        ivSpec = IvParameterSpec(decodedIv)
    }

    /**
     * Mã hóa số CCCD thô sang chuỗi Base64 đã mã hóa AES
     */
    fun encrypt(plainText: String?): String? {
        if (plainText.isNullOrBlank()) return plainText
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    /**
     * Giải mã chuỗi Base64 đã mã hóa AES về số CCCD thô.
     * Tự động trả về bản thô nếu giải mã thất bại (đảm bảo tương thích ngược với dữ liệu cũ).
     */
    fun decrypt(cipherText: String?): String? {
        if (cipherText.isNullOrBlank()) return cipherText
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            cipherText
        }
    }
}

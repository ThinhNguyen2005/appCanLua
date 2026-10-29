package com.giathinh.canlua.util

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Tiện ích mã hóa bảo vệ số CCCD người dùng.
 *
 * Kiến trúc Hybrid:
 * - V2 (Chuẩn mới): Sử dụng PBKDF2 phái sinh SecretKey từ UID người dùng kết hợp Salt bí mật,
 *   kết hợp Vector khởi tạo ngẫu nhiên (Random IV) 16 bytes cho mỗi lần mã hóa (AES/CBC/PKCS5Padding).
 *   Định dạng lưu trữ: "v2:<Base64_IV>:<Base64_Ciphertext>".
 *   Cho phép đa thiết bị đăng nhập cùng tài khoản (cùng UID) giải mã đồng nhất dữ liệu đám mây.
 * - V1 (Legacy Fallback): Tự động nhận diện và giải mã dữ liệu cũ được mã hóa bằng khóa tĩnh trước đây,
 *   đảm bảo 100% không mất dữ liệu CCCD của người dùng cũ.
 */
object CccdCrypto {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val PREFIX_V2 = "v2:"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 1000
    private const val KEY_LENGTH = 256

    // Secret salt của ứng dụng dùng để phái sinh khóa theo từng tài khoản
    private val APP_SALT = "CanLua2026_SecureRiceCccdSalt_V2".toByteArray(Charsets.UTF_8)

    // Legacy Key & IV spec dùng để fallback giải mã dữ liệu cũ
    private val legacyKeySpec: SecretKeySpec
    private val legacyIvSpec: IvParameterSpec

    init {
        val decodedKey = Base64.getDecoder().decode("Q2FuTHVhQXBwQ2NjZEtleTIwMjZTZWN1cmVLZXlLZXk=")
        legacyKeySpec = SecretKeySpec(decodedKey, "AES")
        val decodedIv = Base64.getDecoder().decode("Q2FuTHVhQXBwSXYyMDI2IQ==")
        legacyIvSpec = IvParameterSpec(decodedIv)
    }

    /**
     * Phái sinh SecretKey 256-bit từ UID của người dùng.
     */
    private fun deriveKey(uid: String?): SecretKeySpec {
        val seed = if (uid.isNullOrBlank()) "CanLua_Default_User_Seed_2026" else uid
        val keySpec = PBEKeySpec(seed.toCharArray(), APP_SALT, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Mã hóa số CCCD sang định dạng V2 an toàn với Random IV.
     */
    fun encrypt(plainText: String?, uid: String? = null): String? {
        if (plainText.isNullOrBlank()) return plainText
        return try {
            val key = deriveKey(uid)
            val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val ivB64 = Base64.getEncoder().encodeToString(iv)
            val cipherB64 = Base64.getEncoder().encodeToString(encryptedBytes)
            "$PREFIX_V2$ivB64:$cipherB64"
        } catch (e: Exception) {
            plainText
        }
    }

    /**
     * Giải mã chuỗi CCCD.
     * Tự động phát hiện phiên bản V2 (Random IV) hoặc V1 (Legacy Key).
     * Trả về bản thô nếu thất bại (bảo vệ backward compatibility).
     */
    fun decrypt(cipherText: String?, uid: String? = null): String? {
        if (cipherText.isNullOrBlank()) return cipherText

        // 1. Kiểm tra định dạng V2
        if (cipherText.startsWith(PREFIX_V2)) {
            val parts = cipherText.substring(PREFIX_V2.length).split(":")
            if (parts.size == 2) {
                try {
                    val iv = Base64.getDecoder().decode(parts[0])
                    val encryptedBytes = Base64.getDecoder().decode(parts[1])
                    val key = deriveKey(uid)
                    val cipher = Cipher.getInstance(ALGORITHM)
                    cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                    val decryptedBytes = cipher.doFinal(encryptedBytes)
                    return String(decryptedBytes, Charsets.UTF_8)
                } catch (e: Exception) {
                    // Fallback thử với key mặc định nếu giải mã với UID thất bại
                    if (!uid.isNullOrBlank()) {
                        try {
                            val iv = Base64.getDecoder().decode(parts[0])
                            val encryptedBytes = Base64.getDecoder().decode(parts[1])
                            val key = deriveKey(null)
                            val cipher = Cipher.getInstance(ALGORITHM)
                            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                            val decryptedBytes = cipher.doFinal(encryptedBytes)
                            return String(decryptedBytes, Charsets.UTF_8)
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // 2. Fallback sang V1 Legacy Decryption cho dữ liệu cũ
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, legacyKeySpec, legacyIvSpec)
            val decodedBytes = Base64.getDecoder().decode(cipherText)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            cipherText // Trả về text gốc nếu là chuỗi thô chưa từng mã hóa
        }
    }
}

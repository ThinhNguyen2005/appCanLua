package com.giathinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CccdCryptoTest {

    @Test
    fun `encrypt should securely obfuscate plain text and decrypt should recover it`() {
        val originalCccd = "037092123456"
        
        val encrypted = CccdCrypto.encrypt(originalCccd)
        assertNotEquals(originalCccd, encrypted)
        assertTrue(encrypted?.startsWith("v2:") == true)
        
        val decrypted = CccdCrypto.decrypt(encrypted)
        assertEquals(originalCccd, decrypted)
    }

    @Test
    fun `encrypt with random IV should produce different ciphertexts for same plain text`() {
        val originalCccd = "037092123456"
        val enc1 = CccdCrypto.encrypt(originalCccd)
        val enc2 = CccdCrypto.encrypt(originalCccd)

        assertNotEquals(enc1, enc2)
        assertEquals(originalCccd, CccdCrypto.decrypt(enc1))
        assertEquals(originalCccd, CccdCrypto.decrypt(enc2))
    }

    @Test
    fun `encrypt and decrypt with user UID should work consistently across devices`() {
        val originalCccd = "037092888999"
        val uid = "user_firebase_abc123"

        val encrypted = CccdCrypto.encrypt(originalCccd, uid)
        val decrypted = CccdCrypto.decrypt(encrypted, uid)

        assertEquals(originalCccd, decrypted)
    }

    @Test
    fun `encrypt and decrypt should return null or blank as-is`() {
        assertNull(CccdCrypto.encrypt(null))
        assertEquals("", CccdCrypto.encrypt(""))
        assertEquals("   ", CccdCrypto.encrypt("   "))

        assertNull(CccdCrypto.decrypt(null))
        assertEquals("", CccdCrypto.decrypt(""))
        assertEquals("   ", CccdCrypto.decrypt("   "))
    }

    @Test
    fun `decrypt should return original text on failure for backward compatibility`() {
        // Một số CCCD cũ lưu dạng thô (không mã hóa) thì khi decrypt lỗi sẽ tự động trả về giá trị thô ban đầu
        val legacyRawCccd = "037092000123"
        val decrypted = CccdCrypto.decrypt(legacyRawCccd)
        assertEquals(legacyRawCccd, decrypted)
    }

    @Test
    fun `decrypt legacy v1 ciphertext correctly using fallback`() {
        val originalCccd = "037092777888"
        // Tạo ciphertext theo đúng chuẩn V1 cũ (hardcoded key + static IV)
        val decodedKey = Base64.getDecoder().decode("Q2FuTHVhQXBwQ2NjZEtleTIwMjZTZWN1cmVLZXlLZXk=")
        val keySpec = SecretKeySpec(decodedKey, "AES")
        val decodedIv = Base64.getDecoder().decode("Q2FuTHVhQXBwSXYyMDI2IQ==")
        val ivSpec = IvParameterSpec(decodedIv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val legacyEncrypted = Base64.getEncoder().encodeToString(cipher.doFinal(originalCccd.toByteArray(Charsets.UTF_8)))

        // Đảm bảo không có tiền tố v2:
        assertTrue(!legacyEncrypted.startsWith("v2:"))

        // Decrypt với CccdCrypto mới
        val recovered = CccdCrypto.decrypt(legacyEncrypted)
        assertEquals(originalCccd, recovered)
    }

    @Test
    fun `decrypt should unwrap nested v2 layers caused by repeated encryption`() {
        val originalCccd = "079092001234"
        val uid = "user_firebase_xyz"
        val layer1 = CccdCrypto.encrypt(originalCccd, uid)!!
        // Simulate legacy bug where layer1 was wrapped inside another v2 layer via raw cipher
        val layer2 = wrapRawV2(layer1, uid)
        val layer3 = wrapRawV2(layer2, uid)

        assertEquals(originalCccd, CccdCrypto.decrypt(layer3, uid))
    }

    @Test
    fun `encrypt should be idempotent when given an already v2 encrypted string`() {
        val originalCccd = "079092005678"
        val uid = "user_firebase_xyz"
        val encryptedOnce = CccdCrypto.encrypt(originalCccd, uid)!!
        val encryptedTwice = CccdCrypto.encrypt(encryptedOnce, uid)!!

        assertTrue(encryptedTwice.startsWith("v2:"))
        // Must only have 1 layer: decrypting once via decrypt gives plaintext
        assertEquals(originalCccd, CccdCrypto.decrypt(encryptedTwice, uid))
        val singleUnwrap = encryptedTwice.substring("v2:".length).split(":")
        val keySpec = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(javax.crypto.spec.PBEKeySpec(uid.toCharArray(), "CanLua2026_SecureRiceCccdSalt_V2".toByteArray(), 1000, 256))
            .encoded
            .let { SecretKeySpec(it, "AES") }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            keySpec,
            IvParameterSpec(Base64.getDecoder().decode(singleUnwrap[0]))
        )
        val decodedOnce = String(cipher.doFinal(Base64.getDecoder().decode(singleUnwrap[1])), Charsets.UTF_8)
        assertEquals(originalCccd, decodedOnce)
    }

    private fun wrapRawV2(value: String, uid: String): String {
        val keyBytes = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(javax.crypto.spec.PBEKeySpec(uid.toCharArray(), "CanLua2026_SecureRiceCccdSalt_V2".toByteArray(), 1000, 256))
            .encoded
        val iv = ByteArray(16).apply { java.security.SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(iv))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return "v2:${Base64.getEncoder().encodeToString(iv)}:${Base64.getEncoder().encodeToString(encrypted)}"
    }
}

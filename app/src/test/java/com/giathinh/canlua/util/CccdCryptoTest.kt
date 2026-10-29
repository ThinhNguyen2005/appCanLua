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
}

package com.giathinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CccdCryptoTest {

    @Test
    fun `encrypt should securely obfuscate plain text and decrypt should recover it`() {
        val originalCccd = "037092123456"
        
        val encrypted = CccdCrypto.encrypt(originalCccd)
        assertNotEquals(originalCccd, encrypted)
        
        val decrypted = CccdCrypto.decrypt(encrypted)
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
}

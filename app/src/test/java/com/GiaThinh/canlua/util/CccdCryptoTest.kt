package com.giathinh.canlua.util

import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CccdCryptoTest {
    private val cipher = AesGcmCccdCipher {
        SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
    }

    @Test
    fun `AES GCM roundtrip uses a random IV`() {
        val first = cipher.encrypt("037092123456")
        val second = cipher.encrypt("037092123456")

        assertNotEquals(first, second)
        assertEquals("037092123456", cipher.decrypt(first))
        assertEquals("037092123456", cipher.decrypt(second))
    }

    @Test
    fun `AES GCM rejects modified ciphertext`() {
        val encrypted = cipher.encrypt("037092123456")
        val tampered = encrypted.dropLast(1) + if (encrypted.last() == 'A') "B" else "A"

        assertThrows(Exception::class.java) { cipher.decrypt(tampered) }
    }

    @Test
    fun `legacy AES CBC remains readable`() {
        val encrypted = LegacyCccdCipher.encrypt("037092000123")

        assertEquals("037092000123", LegacyCccdCipher.decryptOrNull(encrypted))
    }

    @Test
    fun `blank values do not initialize Android Keystore`() {
        val crypto = CccdCrypto()

        assertNull(crypto.encrypt(null))
        assertEquals("", crypto.encrypt(""))
        assertEquals("   ", crypto.decrypt("   "))
    }
}
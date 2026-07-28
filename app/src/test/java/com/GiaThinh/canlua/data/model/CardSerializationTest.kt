package com.giathinh.canlua.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Unit tests cho [Card.serialize] / [deserializeCard] — bảo vệ format JSON
 * dùng cho QR Handshake payload + cross-device export.
 *
 * Lưu ý: `org.json.JSONObject` là Android stub. Trên JVM unit test thuần
 * (mockable-android.jar), JSONObject.get() thường throw "Stub!" cho một số
 * overload. Implementation này dùng `JSONObject.keys()` + `get(String)` —
 * tùy AGP version có thể work hoặc không. Nếu fail toàn bộ, fallback là
 * chuyển sang `org.json:json` thuần (xem note cuối file).
 */
class CardSerializationTest {

    private val baseDate = Date(1_700_000_000_000L) // 2023-11-14T22:13:20Z

    // === Roundtrip: serialize → deserialize giữ nguyên dữ liệu ===

    @Test
    fun `roundtrip preserves all scalar fields`() {
        val original = sampleCard()

        val json = original.serialize()
        val restored = deserializeCard(json)

        assertNotNull("Deserialize phải trả về non-null", restored)
        // ownerUid KHÔNG được serialize (deserialize hardcode "") — đây là behavior hiện tại
        assertEquals("", restored!!.ownerUid)
        assertEquals(original.name, restored.name)
        assertEquals(original.cccd, restored.cccd)
        assertEquals(original.traderName, restored.traderName)
        assertEquals(original.date.time, restored.date.time)
        assertEquals(original.totalWeight, restored.totalWeight, 0.0)
        assertEquals(original.bagWeight, restored.bagWeight, 0.0)
        assertEquals(original.impurityWeight, restored.impurityWeight, 0.0)
        assertEquals(original.netWeight, restored.netWeight, 0.0)
        assertEquals(original.depositAmount, restored.depositAmount, 0.0)
        assertEquals(original.pricePerKg, restored.pricePerKg, 0.0)
        assertEquals(original.totalAmount, restored.totalAmount, 0.0)
        assertEquals(original.paidAmount, restored.paidAmount, 0.0)
        assertEquals(original.remainingAmount, restored.remainingAmount, 0.0)
        assertEquals(original.bagCount, restored.bagCount)
        assertEquals(original.isLocked, restored.isLocked)
        assertEquals(original.isPaid, restored.isPaid)
        assertEquals(original.riceVariety, restored.riceVariety)
        assertEquals(original.moisturePercent, restored.moisturePercent, 0.0)
        assertEquals(original.seasonLabel, restored.seasonLabel)
        assertEquals(original.qrToken, restored.qrToken)
        assertEquals(original.lockedByTraderId, restored.lockedByTraderId)
        assertEquals(original.latitude, restored.latitude)
        assertEquals(original.longitude, restored.longitude)
        assertEquals(original.traderPhone, restored.traderPhone)
        assertEquals(original.fieldAddress, restored.fieldAddress)
        assertEquals(original.impurityIsPercent, restored.impurityIsPercent)
        assertEquals(original.bagMethodIsSampling, restored.bagMethodIsSampling)
        assertEquals(original.bagSampleCount, restored.bagSampleCount)
        assertEquals(original.bagSampleTotalWeight, restored.bagSampleTotalWeight, 0.0)
        assertEquals(original.weightInputMode, restored.weightInputMode)
    }

    @Test
    fun `roundtrip preserves nullable fields when null`() {
        val original = sampleCard().copy(
            cccd = null,
            qrToken = null,
            lockedByTraderId = null,
            latitude = null,
            longitude = null
        )

        val restored = deserializeCard(original.serialize())!!

        assertNull(restored.cccd)
        assertNull(restored.qrToken)
        assertNull(restored.lockedByTraderId)
        assertNull(restored.latitude)
        assertNull(restored.longitude)
    }

    @Test
    fun `roundtrip preserves unicode - Vietnamese diacritics and emoji`() {
        val original = sampleCard().copy(
            name = "Nông dân Trần Văn Hùng",
            traderName = "Thương lái Nguyễn Thị Tư",
            fieldAddress = "Ấp 4, xã Tân Thành, huyện Châu Thành 🌾",
            seasonLabel = "Đông Xuân 2025–2026"
        )

        val restored = deserializeCard(original.serialize())!!

        assertEquals(original.name, restored.name)
        assertEquals(original.traderName, restored.traderName)
        assertEquals(original.fieldAddress, restored.fieldAddress)
        assertEquals(original.seasonLabel, restored.seasonLabel)
    }

    @Test
    fun `roundtrip preserves special characters in strings - quotes and backslashes`() {
        // jsonString() escape ký tự đặc biệt: \\ → \\\\, " → \\\"
        val original = sampleCard().copy(
            name = "Anh \"Hùng\" nói: cần 1.000kg",
            fieldAddress = "C:\\Users\\Farmer\\Documents"
        )

        val restored = deserializeCard(original.serialize())!!

        assertEquals(original.name, restored.name)
        assertEquals(original.fieldAddress, restored.fieldAddress)
    }

    @Test
    fun `roundtrip preserves JSON control characters`() {
        val original = sampleCard().copy(
            name = "Farmer\nNew season",
            fieldAddress = "Hamlet 1\tCommune A\r\nDistrict B"
        )

        val restored = deserializeCard(original.serialize())!!

        assertEquals(original.name, restored.name)
        assertEquals(original.fieldAddress, restored.fieldAddress)
    }

    @Test
    fun `roundtrip preserves boolean false - deserialize must not coerce to true`() {
        // Bug potential: as? Boolean ?: false — nếu field null trong JSON thì mất giá trị.
        val original = sampleCard().copy(
            isLocked = false,
            isPaid = false,
            impurityIsPercent = false,
            bagMethodIsSampling = false
        )

        val restored = deserializeCard(original.serialize())!!

        assertEquals(false, restored.isLocked)
        assertEquals(false, restored.isPaid)
        assertEquals(false, restored.impurityIsPercent)
        assertEquals(false, restored.bagMethodIsSampling)
    }

    @Test
    fun `roundtrip preserves boolean true`() {
        val original = sampleCard().copy(
            isLocked = true,
            isPaid = true,
            impurityIsPercent = true,
            bagMethodIsSampling = true
        )

        val restored = deserializeCard(original.serialize())!!

        assertEquals(true, restored.isLocked)
        assertEquals(true, restored.isPaid)
        assertEquals(true, restored.impurityIsPercent)
        assertEquals(true, restored.bagMethodIsSampling)
    }

    @Test
    fun `roundtrip preserves integer fields - bagCount, bagSampleCount`() {
        val original = sampleCard().copy(bagCount = 250, bagSampleCount = 25)

        val restored = deserializeCard(original.serialize())!!

        assertEquals(250, restored.bagCount)
        assertEquals(25, restored.bagSampleCount)
    }

    @Test
    fun `roundtrip preserves zero integer - not coerced to null`() {
        val original = sampleCard().copy(bagCount = 0, bagSampleCount = 0)

        val restored = deserializeCard(original.serialize())!!

        assertEquals(0, restored.bagCount)
        assertEquals(0, restored.bagSampleCount)
    }

    @Test
    fun `roundtrip preserves zero double - not coerced to null`() {
        val original = sampleCard().copy(
            totalWeight = 0.0,
            bagWeight = 0.0,
            impurityWeight = 0.0,
            netWeight = 0.0,
            depositAmount = 0.0,
            pricePerKg = 0.0,
            totalAmount = 0.0,
            paidAmount = 0.0,
            remainingAmount = 0.0,
            moisturePercent = 0.0,
            bagSampleTotalWeight = 0.0
        )

        val restored = deserializeCard(original.serialize())!!

        assertEquals(0.0, restored.totalWeight, 0.0)
        assertEquals(0.0, restored.netWeight, 0.0)
        assertEquals(0.0, restored.totalAmount, 0.0)
        assertEquals(0.0, restored.moisturePercent, 0.0)
        assertEquals(0.0, restored.bagSampleTotalWeight, 0.0)
    }

    @Test
    fun `roundtrip preserves very small and very large doubles`() {
        val original = sampleCard().copy(
            totalWeight = 0.001,
            totalAmount = 1_000_000_000.0
        )

        val restored = deserializeCard(original.serialize())!!

        assertEquals(0.001, restored.totalWeight, 1e-9)
        assertEquals(1_000_000_000.0, restored.totalAmount, 1.0)
    }

    @Test
    fun `roundtrip preserves weightInputMode values - SMALL and LARGE`() {
        for (mode in listOf("SMALL", "LARGE")) {
            val original = sampleCard().copy(weightInputMode = mode)
            val restored = deserializeCard(original.serialize())!!
            assertEquals(mode, restored.weightInputMode)
        }
    }

    // === serialize: structural ===

    @Test
    fun `serialize produces valid JSON object - starts with brace and ends with brace`() {
        val json = sampleCard().serialize()
        assertEquals('{', json.first())
        assertEquals('}', json.last())
    }

    @Test
    fun `serialize includes all required field names as keys`() {
        val json = sampleCard().serialize()
        // Không ép parser — chỉ check substring có mặt các key.
        val expectedKeys = listOf(
            "\"name\"", "\"cccd\"", "\"traderName\"", "\"date\"",
            "\"totalWeight\"", "\"bagWeight\"", "\"impurityWeight\"", "\"netWeight\"",
            "\"depositAmount\"", "\"pricePerKg\"", "\"totalAmount\"",
            "\"paidAmount\"", "\"remainingAmount\"", "\"bagCount\"",
            "\"isLocked\"", "\"isPaid\"", "\"riceVariety\"",
            "\"moisturePercent\"", "\"seasonLabel\"", "\"qrToken\"",
            "\"lockedByTraderId\"", "\"latitude\"", "\"longitude\"",
            "\"traderPhone\"", "\"fieldAddress\"", "\"impurityIsPercent\"",
            "\"bagMethodIsSampling\"", "\"bagSampleCount\"",
            "\"bagSampleTotalWeight\"", "\"weightInputMode\""
        )
        for (key in expectedKeys) {
            assertTrue("Phải có key $key trong JSON. Got: $json", json.contains(key))
        }
    }

    @Test
    fun `serialize omits ownerUid - by design (security - prevent cross-user leak)`() {
        // ownerUid là per-user; QR Handshake payload chỉ chia sẻ business fields.
        // Test document behavior: ownerUid KHÔNG xuất hiện trong JSON.
        val card = sampleCard().copy(ownerUid = "user-secret-A")
        val json = card.serialize()
        assertTrue(
            "ownerUid KHÔNG được leak vào JSON. Got: $json",
            !json.contains("ownerUid") && !json.contains("user-secret-A")
        )
    }

    @Test
    fun `serialize omits firestoreId - cross-device ID not needed in handshake`() {
        val card = sampleCard().copy(firestoreId = "fs-secret-123")
        val json = card.serialize()
        assertTrue(
            "firestoreId KHÔNG được leak vào JSON. Got: $json",
            !json.contains("firestoreId") && !json.contains("fs-secret-123")
        )
    }

    @Test
    fun `serialize omits lastModifiedMs - per-device sync metadata not in handshake`() {
        val card = sampleCard().copy(lastModifiedMs = 9_999_999L)
        val json = card.serialize()
        assertTrue(
            "lastModifiedMs KHÔNG được leak vào JSON. Got: $json",
            !json.contains("lastModifiedMs") && !json.contains("9999999")
        )
    }

    @Test
    fun `serialize omits id - autoincrement local ID not portable`() {
        val card = sampleCard().copy(id = 42L)
        val json = card.serialize()
        assertTrue(
            "id KHÔNG được leak vào JSON. Got: $json",
            !json.contains("\"id\"") && !json.contains("\"42\"")
        )
    }

    // === deserialize: edge cases ===

    @Test
    fun `deserialize invalid JSON returns null gracefully`() {
        assertNull(deserializeCard("not json at all"))
    }

    @Test
    fun `deserialize empty JSON object returns card with defaults`() {
        // Empty {} → mọi field fallback default → name = "" vì required string.
        // Tuy nhiên `name` được lấy qua `.orEmpty()` → "" vẫn OK.
        val restored = deserializeCard("{}")
        assertNotNull(restored)
        assertEquals("", restored!!.name)
        assertEquals("", restored.ownerUid) // hardcode
        assertEquals(0L, restored.date.time) // (Number).toLong() ?: 0L
    }

    @Test
    fun `deserialize missing number field uses zero default`() {
        // totalWeight thiếu → default 0.0
        val json = """{"name":"X","date":1700000000000}"""
        val restored = deserializeCard(json)!!
        assertEquals(0.0, restored.totalWeight, 0.0)
        assertEquals(0, restored.bagCount)
    }

    @Test
    fun `deserialize missing boolean field uses false default`() {
        val json = """{"name":"X","date":1700000000000,"isLocked":true}"""
        val restored = deserializeCard(json)!!
        assertEquals(true, restored.isLocked)
        // isPaid missing → false
        assertEquals(false, restored.isPaid)
    }

    @Test
    fun `deserialize blank weightInputMode falls back to SMALL`() {
        val json = """{"name":"X","date":1700000000000,"weightInputMode":""}"""
        val restored = deserializeCard(json)!!
        assertEquals("SMALL", restored.weightInputMode)
    }

    @Test
    fun `deserialize weightInputMode of unknown value is kept as-is`() {
        // Caller chịu trách nhiệm validate enum — deserialize giữ nguyên string.
        val json = """{"name":"X","date":1700000000000,"weightInputMode":"FOO"}"""
        val restored = deserializeCard(json)!!
        assertEquals("FOO", restored.weightInputMode)
    }

    // === helpers ===

    private fun sampleCard() = Card(
        id = 1L,
        ownerUid = "user-A",
        name = "Nông dân X",
        cccd = "012345678901",
        traderName = "Thương lái Y",
        date = baseDate,
        totalWeight = 1000.0,
        bagWeight = 50.0,
        impurityWeight = 10.0,
        netWeight = 940.0,
        depositAmount = 500_000.0,
        pricePerKg = 7000.0,
        totalAmount = 6_580_000.0,
        paidAmount = 3_000_000.0,
        remainingAmount = 3_580_000.0,
        bagCount = 100,
        isLocked = false,
        isPaid = false,
        riceVariety = "ST25",
        moisturePercent = 14.0,
        seasonLabel = "Đông Xuân 2026",
        qrToken = "abc123def",
        lockedByTraderId = "trader-Z",
        latitude = 10.762622,
        longitude = 106.660172,
        traderPhone = "0901234567",
        fieldAddress = "Ấp 1, xã A",
        impurityIsPercent = false,
        bagMethodIsSampling = false,
        bagSampleCount = 0,
        bagSampleTotalWeight = 0.0,
        weightInputMode = "SMALL",
        firestoreId = "fs-1",
        lastModifiedMs = 1_700_000_000_000L
    )
}
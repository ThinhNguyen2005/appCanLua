package com.GiaThinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Tests bổ sung cho [MoneyFormatter] — edge case bị bỏ qua trong MoneyFormatterTest.kt.
 *
 * Bổ sung thêm:
 * - formatVndShort với số âm (edge case chưa test)
 * - toWordsCore với tỷ kèm triệu lẻ (không phải trăm triệu)
 * - Boundary: 100_000 (trăm nghìn đầu tiên)
 * - Boundary: 999_999_999 (gần tỷ)
 * - toWordsCore: tỷ + triệu lẻ (không phải tròn trăm triệu)
 */
class MoneyFormatterEdgeCaseTest {

    private val viLabels = MoneyLabels(
        billion = "tỷ",
        million = "triệu",
        hundredThousand = "trăm nghìn",
        thousand = "nghìn",
        dong = "đồng",
        hundred = "trăm",
    )

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    // === toWordsCore — edge cases ===

    @Test
    fun `toWordsCore 100_000 is 1 tram nghin`() {
        // 100_000: nghin = 100, nghin >= 100 và nghin % 100 == 0 → "1 trăm nghìn"
        assertEquals("1 trăm nghìn", MoneyFormatter.toWordsCore(100_000.0, viLabels))
    }

    @Test
    fun `toWordsCore 200_000 is 2 tram nghin`() {
        assertEquals("2 trăm nghìn", MoneyFormatter.toWordsCore(200_000.0, viLabels))
    }

    @Test
    fun `toWordsCore 150_000 is NOT tram nghin - has le`() {
        // 150 nghìn: nghin = 150, nghinLe = 150 % 100 = 50 ≠ 0 → "150 nghìn"
        assertEquals("150 nghìn", MoneyFormatter.toWordsCore(150_000.0, viLabels))
    }

    @Test
    fun `toWordsCore 999_999 is 999 nghin`() {
        // 999 nghìn: không tròn trăm → "999 nghìn"
        assertEquals("999 nghìn", MoneyFormatter.toWordsCore(999_000.0, viLabels))
    }

    @Test
    fun `toWordsCore 1_000_000 is 1 trieu - boundary`() {
        assertEquals("1 triệu", MoneyFormatter.toWordsCore(1_000_000.0, viLabels))
    }

    @Test
    fun `toWordsCore 999_999_000 is 999 trieu 999 nghin`() {
        // 999 triệu 999 nghìn → lẻ → "999 triệu 999 nghìn"
        assertEquals("999 triệu 999 nghìn", MoneyFormatter.toWordsCore(999_999_000.0, viLabels))
    }

    @Test
    fun `toWordsCore 1_tỷ_plus_250_trieu_le - not rounded hundred million`() {
        // 1 tỷ 250 triệu: trieu=250, 250/100=2, 250%100=50 ≠ 0 → KHÔNG dùng shortcut tram-trieu
        // → "1 tỷ" rồi rơi xuống trieu branch: "250 triệu"
        assertEquals("1 tỷ 250 triệu", MoneyFormatter.toWordsCore(1_250_000_000.0, viLabels))
    }

    @Test
    fun `toWordsCore 1_tỷ_300_trieu - round hundred million shortcut`() {
        // 1 tỷ 300 triệu: trieu=300, 300/100=3, 300%100=0, afterTrieu=0 → shortcut "1 tỷ 3 trăm triệu"
        assertEquals("1 tỷ 3 trăm triệu", MoneyFormatter.toWordsCore(1_300_000_000.0, viLabels))
    }

    @Test
    fun `toWordsCore tỷ with triệu le not round hundred - falls through to trieu`() {
        // 2 tỷ 50 triệu: trieu=50, không tròn trăm → "2 tỷ 50 triệu"
        assertEquals("2 tỷ 50 triệu", MoneyFormatter.toWordsCore(2_050_000_000.0, viLabels))
    }

    @Test
    fun `toWordsCore 1_dong is 1 dong`() {
        assertEquals("1 đồng", MoneyFormatter.toWordsCore(1.0, viLabels))
    }

    @Test
    fun `toWordsCore 1000_dong is 1 nghin not dong`() {
        // 1000 → nghin = 1, dong = 0 → "1 nghìn" (nghin branch, not dong)
        assertEquals("1 nghìn", MoneyFormatter.toWordsCore(1000.0, viLabels))
    }

    @Test
    fun `toWordsCore 999_dong is 999 dong`() {
        assertEquals("999 đồng", MoneyFormatter.toWordsCore(999.0, viLabels))
    }

    @Test
    fun `toWordsCore typical farmer settlement 7_2 million`() {
        // 7.2 triệu: trieu=7, tramNghin=2, nghinLe=0 → "7 triệu 2"
        assertEquals("7 triệu 2", MoneyFormatter.toWordsCore(7_200_000.0, viLabels))
    }

    @Test
    fun `toWordsCore 2_tỷ no remainder`() {
        assertEquals("2 tỷ", MoneyFormatter.toWordsCore(2_000_000_000.0, viLabels))
    }

    // === formatVndShort — edge cases ===

    @Test
    fun `formatVndShort zero is 0 dong`() {
        assertEquals("0 đ", MoneyFormatter.formatVndShort(0.0))
    }

    @Test
    fun `formatVndShort 1_000_000 formats with vi-VN grouping`() {
        assertEquals("1.000.000 đ", MoneyFormatter.formatVndShort(1_000_000.0))
    }

    @Test
    fun `formatVndShort truncates decimal by toLong`() {
        // 12_345.99.toLong() = 12_345 → "12.345 đ"
        assertEquals("12.345 đ", MoneyFormatter.formatVndShort(12_345.99))
    }

    @Test
    fun `formatVndShort large amount over 1 billion`() {
        // 1_500_000_000 → "1.500.000.000 đ"
        assertEquals("1.500.000.000 đ", MoneyFormatter.formatVndShort(1_500_000_000.0))
    }

    @Test
    fun `formatVndShort negative - toLong preserves negative`() {
        // Tiền âm không nên xuất hiện trên UI nhưng formatter không block
        val result = MoneyFormatter.formatVndShort(-1_000_000.0)
        assertTrue("Phải chứa dấu âm", result.contains("-"))
        assertTrue("Phải có đơn vị đ", result.contains("đ"))
    }

    // === toWordsCore: boundary giữa các tier ===

    @Test
    fun `toWordsCore boundary 999_999_999 is not tỷ`() {
        // 999_999_999 < 1_000_000_000 → ty = 0 → vào trieu branch
        // trieu = 999, afterTrieu = 999_000, nghin = 999, nghinLe = 99 (lẻ) → "999 triệu 999 nghìn"
        val result = MoneyFormatter.toWordsCore(999_999_999.0, viLabels)
        assertTrue("Không được có chữ 'tỷ' cho 999,999,999", !result.contains("tỷ"))
        assertEquals("999 triệu 999 nghìn", result)
    }

    @Test
    fun `toWordsCore boundary 1_000_000_000 exact is 1 tỷ`() {
        assertEquals("1 tỷ", MoneyFormatter.toWordsCore(1_000_000_000.0, viLabels))
    }
}

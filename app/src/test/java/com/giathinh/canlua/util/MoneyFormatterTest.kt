package com.giathinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests cho [MoneyFormatter.toWordsCore] — phần "đọc tiền" rút gọn tiếng Việt
 * hiển thị phía dưới con số trên phiếu cân.
 *
 * Critical test: nếu logic sai → nông dân đọc nhầm số tiền → khiếu nại.
 *
 * Tách core khỏi Context là chủ ý: test chạy thuần JVM, không cần Robolectric.
 */
class MoneyFormatterTest {

    private val viLabels = MoneyLabels(
        billion = "tỷ",
        million = "triệu",
        hundredThousand = "trăm nghìn",
        thousand = "nghìn",
        dong = "đồng",
        hundred = "trăm",
    )

    // === toán hạng zero / âm → trả rỗng ===

    @Test
    fun `zero returns empty string`() {
        assertEquals("", MoneyFormatter.toWordsCore(0.0, viLabels))
    }

    @Test
    fun `negative returns empty string`() {
        // Phiếu cân không có tiền âm; UI đang dùng chính kết quả này để hide Text.
        assertEquals("", MoneyFormatter.toWordsCore(-100.0, viLabels))
    }

    @Test
    fun `tiny positive rounds down to zero and returns empty`() {
        // 0.4.toLong() = 0 → guard raw <= 0L bắt được.
        assertEquals("", MoneyFormatter.toWordsCore(0.4, viLabels))
    }

    // === đồng (< 1_000) ===

    @Test
    fun `1 dong says 1 đồng`() {
        assertEquals("1 đồng", MoneyFormatter.toWordsCore(1.0, viLabels))
    }

    @Test
    fun `999 dong says 999 đồng`() {
        assertEquals("999 đồng", MoneyFormatter.toWordsCore(999.0, viLabels))
    }

    @Test
    fun `decimal fraction truncates to long`() {
        // 1_500.7.toLong() = 1500 → 1 nghìn 500 đồng? Không, 1500 = 1.5 nghìn → "1 nghìn" (rơi nhánh 1..999 nghìn).
        assertEquals("1 nghìn", MoneyFormatter.toWordsCore(1_500.7, viLabels))
    }

    // === nghìn (1_000 .. 999_999) ===

    @Test
    fun `1_000 says 1 nghìn`() {
        assertEquals("1 nghìn", MoneyFormatter.toWordsCore(1_000.0, viLabels))
    }

    @Test
    fun `50_000 says 50 nghìn`() {
        assertEquals("50 nghìn", MoneyFormatter.toWordsCore(50_000.0, viLabels))
    }

    @Test
    fun `800_000 says 8 trăm nghìn when round hundred thousand`() {
        // 800_000 nghìn = 800, tròn trăm → dùng label "trăm nghìn"
        assertEquals("8 trăm nghìn", MoneyFormatter.toWordsCore(800_000.0, viLabels))
    }

    @Test
    fun `500_000 says 5 trăm nghìn`() {
        assertEquals("5 trăm nghìn", MoneyFormatter.toWordsCore(500_000.0, viLabels))
    }

    @Test
    fun `999_000 falls through to nghìn branch with le fraction`() {
        // 999 nghìn, không tròn trăm (999 % 100 = 99) → "999 nghìn" thay vì "9 trăm nghìn"
        assertEquals("999 nghìn", MoneyFormatter.toWordsCore(999_000.0, viLabels))
    }

    @Test
    fun `900_000 says 9 trăm nghìn when round hundred`() {
        assertEquals("9 trăm nghìn", MoneyFormatter.toWordsCore(900_000.0, viLabels))
    }

    // === triệu (1_000_000 .. 999_999_999) ===

    @Test
    fun `1_000_000 says 1 triệu`() {
        assertEquals("1 triệu", MoneyFormatter.toWordsCore(1_000_000.0, viLabels))
    }

    @Test
    fun `5_000_000 says 5 triệu`() {
        assertEquals("5 triệu", MoneyFormatter.toWordsCore(5_000_000.0, viLabels))
    }

    @Test
    fun `1_200_000 says 1 triệu 2 (round hundred thousand)`() {
        // 1 triệu 200 nghìn = tròn trăm nghìn → rút gọn "1 triệu 2"
        assertEquals("1 triệu 2", MoneyFormatter.toWordsCore(1_200_000.0, viLabels))
    }

    @Test
    fun `1_250_000 falls through to 1 triệu 250 nghìn (le fraction)`() {
        // 250 nghìn lẻ (250 % 100 = 50) → không rút gọn
        assertEquals("1 triệu 250 nghìn", MoneyFormatter.toWordsCore(1_250_000.0, viLabels))
    }

    @Test
    fun `1_500_000 says 1 triệu 5 (round hundred thousand)`() {
        // 500 nghìn = tròn trăm → rút gọn "1 triệu 5"
        assertEquals("1 triệu 5", MoneyFormatter.toWordsCore(1_500_000.0, viLabels))
    }

    @Test
    fun `1_001_000 with le fraction says 1 triệu 1 nghìn`() {
        // 1 nghìn lẻ (1 % 100 = 1) → rơi nhánh else → "1 triệu 1 nghìn"
        assertEquals("1 triệu 1 nghìn", MoneyFormatter.toWordsCore(1_001_000.0, viLabels))
    }

    @Test
    fun `123_456_000 keeps full breakdown`() {
        // 123 triệu 456 nghìn (456 lẻ)
        assertEquals("123 triệu 456 nghìn", MoneyFormatter.toWordsCore(123_456_000.0, viLabels))
    }

    // === tỷ (≥ 1_000_000_000) ===

    @Test
    fun `1_000_000_000 says 1 tỷ`() {
        assertEquals("1 tỷ", MoneyFormatter.toWordsCore(1_000_000_000.0, viLabels))
    }

    @Test
    fun `5_000_000_000 says 5 tỷ`() {
        assertEquals("5 tỷ", MoneyFormatter.toWordsCore(5_000_000_000.0, viLabels))
    }

    @Test
    fun `1_200_000_000 with round hundred million uses tram-trieu shortcut`() {
        // 1 tỷ 200 triệu = tròn trăm triệu, afterTrieu == 0 → rút gọn "1 tỷ 2 trăm triệu"
        assertEquals("1 tỷ 2 trăm triệu", MoneyFormatter.toWordsCore(1_200_000_000.0, viLabels))
    }

    @Test
    fun `1_500_000_000 with round hundred million uses tram-trieu shortcut`() {
        assertEquals("1 tỷ 5 trăm triệu", MoneyFormatter.toWordsCore(1_500_000_000.0, viLabels))
    }

    @Test
    fun `1_234_000_000 with le million falls through to trieu branch`() {
        // 1 tỷ 234 triệu (không tròn trăm triệu) → "1 tỷ 234 triệu"
        assertEquals("1 tỷ 234 triệu", MoneyFormatter.toWordsCore(1_234_000_000.0, viLabels))
    }

    @Test
    fun `1_000_500_000 ty branch sets tỷ and trieu branch handles 500 nghìn le`() {
        // 1 tỷ 500 nghìn (trieu = 0) → vẫn phải có 1 tỷ và 500 nghìn
        // Đây là edge case: ty > 0 nhưng trieu == 0, afterTrieu = 500_000
        // Branch ty add "1 tỷ", return ngay nếu shortcut match (không match vì trieu=0 → vào nhánh trieu)
        // Trieu = 0 nên rơi xuống nghin branch: 500 nghìn tròn trăm → "5 trăm nghìn"
        assertEquals("1 tỷ 5 trăm nghìn", MoneyFormatter.toWordsCore(1_000_500_000.0, viLabels))
    }

    @Test
    fun `12_345_000_000 keeps full breakdown`() {
        // 12 tỷ 345 triệu (345 lẻ — không tròn trăm triệu)
        assertEquals("12 tỷ 345 triệu", MoneyFormatter.toWordsCore(12_345_000_000.0, viLabels))
    }

    // === formatVndShort (smoke test) ===

    @Test
    fun `formatVndShort uses vi-VN grouping`() {
        // vi-VN dùng dấu chấm làm phân cách hàng nghìn
        assertEquals("1.200.000 đ", MoneyFormatter.formatVndShort(1_200_000.0))
    }

    @Test
    fun `formatVndShort truncates decimal`() {
        // Không hiển thị xu/lẻ — luôn làm tròn xuống long
        assertEquals("999 đ", MoneyFormatter.formatVndShort(999.9))
    }

    // === i18n smoke ===

    @Test
    fun `toWordsCore works with any locale labels`() {
        val enLabels = MoneyLabels(
            billion = "b",
            million = "m",
            hundredThousand = "hk",
            thousand = "k",
            dong = "d",
            hundred = "h",
        )
        assertEquals("1.5".let { "1 m 250 k" }, MoneyFormatter.toWordsCore(1_250_000.0, enLabels))
    }

    // === guard về số âm lớn (sau khi abs) ===

    @Test
    fun `large negative returns empty string (abs guard not used but early-return saves it)`() {
        // amount.toLong() của số âm vẫn âm, raw <= 0L bắt được
        assertEquals("", MoneyFormatter.toWordsCore(-999_999_999_999.0, viLabels))
    }

    @Test
    fun `output is non-blank for typical farmer card amount`() {
        // Sanity: case thực tế trên phiếu cân — 5 tấn * 7000đ/kg = 35_000_000
        val out = MoneyFormatter.toWordsCore(35_000_000.0, viLabels)
        assertTrue("expected non-blank for 35tr, got '$out'", out.isNotBlank())
        assertEquals("35 triệu", out)
    }
}

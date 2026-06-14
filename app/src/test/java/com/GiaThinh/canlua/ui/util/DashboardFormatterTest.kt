package com.GiaThinh.canlua.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Unit tests cho [DashboardFormatter] — helper format số liệu compact trên dashboard.
 *
 * Pure logic, không phụ thuộc Android framework → chạy được trên JVM unit test.
 *
 * Lưu ý về locale:
 *  - `moneyFull` dùng `NumberFormat.getNumberInstance(vi-VN)` → output luôn dùng ".".
 *  - `money` / `weight` / `percent` dùng `String.format(...)` mà locale theo
 *    JVM default (KHÔNG hardcode vi-VN) → trên device thật thường là vi-VN
 *    nhưng trên JVM unit test có thể là khác.
 *  - Test này set `Locale.US` để JVM dùng "." làm decimal separator, khớp
 *    với behavior mong đợi trên thiết bị của user Việt Nam (vi-VN cũng dùng ",",
 *    nhưng ta test "." ở đây vì `String.format` mặc định theo JVM).
 *    Quan trọng: test phải ổn định bất kể máy dev nào.
 *
 * Edge cases cần cover:
 *  - Money compact: chuyển đổi đơn vị (đ → k → tr → tỷ) theo magnitude
 *  - Money: làm tròn đúng 1 chữ số thập phân cho "tr", 2 chữ số cho "tỷ"
 *  - Weight: < 1000kg → kg, ≥ 1000kg → tấn
 *  - deltaPercent: trả về null khi previous = 0 (chia cho 0)
 *  - formatDelta: hiển thị "+" với delta dương, "—" cho null
 */
class DashboardFormatterTest {

    @Before
    fun setUp() {
        // Pin JVM default locale để String.format output ổn định qua các máy dev.
        Locale.setDefault(Locale.US)
    }

    // === money ===

    @Test
    fun `money under 1000 uses dong suffix`() {
        assertEquals("500đ", DashboardFormatter.money(500.0))
    }

    @Test
    fun `money zero uses dong suffix`() {
        assertEquals("0đ", DashboardFormatter.money(0.0))
    }

    @Test
    fun `money 1500 formats as 2k`() {
        // 1500 / 1000 = 1.5 → %.0f → "2k"
        assertEquals("2k", DashboardFormatter.money(1500.0))
    }

    @Test
    fun `money 999999 formats as 1000k boundary before million`() {
        // 999999 < 1_000_000 → dùng "k" tier → %.0f = 1000
        assertEquals("1000k", DashboardFormatter.money(999_999.0))
    }

    @Test
    fun `money 1 million formats as 1_0 tr with one decimal`() {
        // %.1f → "1.0 tr"
        assertEquals("1.0 tr", DashboardFormatter.money(1_000_000.0))
    }

    @Test
    fun `money 1_5 million formats as 1_5 tr`() {
        assertEquals("1.5 tr", DashboardFormatter.money(1_500_000.0))
    }

    @Test
    fun `money 12_5 million formats as 12_5 tr`() {
        assertEquals("12.5 tr", DashboardFormatter.money(12_500_000.0))
    }

    @Test
    fun `money 1 billion formats as 1_00 ty with two decimals`() {
        // %.2f → "1.00 tỷ"
        assertEquals("1.00 tỷ", DashboardFormatter.money(1_000_000_000.0))
    }

    @Test
    fun `money 1_5 billion formats as 1_50 ty`() {
        assertEquals("1.50 tỷ", DashboardFormatter.money(1_500_000_000.0))
    }

    @Test
    fun `money negative value uses abs for tier selection but preserves sign`() {
        // Tier chọn theo abs, nhưng amount / unit giữ dấu âm.
        // -1_500_000 / 1_000_000 = -1.5 → "%.1f" → "-1.5 tr"
        assertEquals("-1.5 tr", DashboardFormatter.money(-1_500_000.0))
    }

    // === moneyFull ===

    @Test
    fun `moneyFull formats with vi-VN thousand separator - dot for vi-VN`() {
        // NumberFormat(vi-VN) dùng "." làm thousand separator, KHÔNG phụ thuộc JVM locale.
        // 1.200.000 là locale vi-VN.
        assertEquals("1.200.000đ", DashboardFormatter.moneyFull(1_200_000.0))
    }

    @Test
    fun `moneyFull handles zero`() {
        assertEquals("0đ", DashboardFormatter.moneyFull(0.0))
    }

    @Test
    fun `moneyFull truncates decimal portion - toLong on Double`() {
        // 1_999_999.99 → toLong() = 1_999_999 → "1.999.999đ"
        assertEquals("1.999.999đ", DashboardFormatter.moneyFull(1_999_999.99))
    }

    // === weight ===

    @Test
    fun `weight under 1000 formats as kg with no decimals`() {
        // %.0f → "500 kg"
        assertEquals("500 kg", DashboardFormatter.weight(500.0))
    }

    @Test
    fun `weight exactly 1000 boundary formats as ton`() {
        // 1000 / 1000 = 1.0 → "1.0 tấn"
        assertEquals("1.0 tấn", DashboardFormatter.weight(1000.0))
    }

    @Test
    fun `weight 1500 formats as 1_5 ton`() {
        assertEquals("1.5 tấn", DashboardFormatter.weight(1500.0))
    }

    @Test
    fun `weight 999 boundary still uses kg`() {
        assertEquals("999 kg", DashboardFormatter.weight(999.0))
    }

    @Test
    fun `weight zero formats as 0 kg`() {
        assertEquals("0 kg", DashboardFormatter.weight(0.0))
    }

    // === percent ===

    @Test
    fun `percent formats with one decimal and percent sign`() {
        // "%.1f%%" → "12.5%"
        assertEquals("12.5%", DashboardFormatter.percent(12.5))
    }

    @Test
    fun `percent integer values get one decimal`() {
        // 50.0 → "50.0%"
        assertEquals("50.0%", DashboardFormatter.percent(50.0))
    }

    @Test
    fun `percent zero`() {
        assertEquals("0.0%", DashboardFormatter.percent(0.0))
    }

    // === deltaPercent ===

    @Test
    fun `deltaPercent positive change`() {
        // (150 - 100) / 100 * 100 = 50
        assertEquals(50.0, DashboardFormatter.deltaPercent(150.0, 100.0)!!, 0.0001)
    }

    @Test
    fun `deltaPercent negative change`() {
        // (80 - 100) / 100 * 100 = -20
        assertEquals(-20.0, DashboardFormatter.deltaPercent(80.0, 100.0)!!, 0.0001)
    }

    @Test
    fun `deltaPercent no change returns zero`() {
        assertEquals(0.0, DashboardFormatter.deltaPercent(100.0, 100.0)!!, 0.0001)
    }

    @Test
    fun `deltaPercent returns null when previous is zero - prevents division by zero`() {
        assertNull(DashboardFormatter.deltaPercent(100.0, 0.0))
    }

    @Test
    fun `deltaPercent doubles value when current is twice previous`() {
        // (200 - 100) / 100 * 100 = 100
        assertEquals(100.0, DashboardFormatter.deltaPercent(200.0, 100.0)!!, 0.0001)
    }

    // === formatDelta ===

    @Test
    fun `formatDelta null returns em dash placeholder`() {
        assertEquals("—", DashboardFormatter.formatDelta(null))
    }

    @Test
    fun `formatDelta positive gets plus sign`() {
        // +15% (no decimal — "%.0f")
        assertEquals("+15%", DashboardFormatter.formatDelta(15.0))
    }

    @Test
    fun `formatDelta negative keeps minus sign inherent to number`() {
        // -8 → "%.0f" trên -8.0 → "-8%"
        assertEquals("-8%", DashboardFormatter.formatDelta(-8.0))
    }

    @Test
    fun `formatDelta zero is shown as plus zero - by design`() {
        // sign = if (>= 0) "+" → "0%" hiển thị là "+0%"
        // (Caller cần hiểu convention này — đã có ở code gốc.)
        assertEquals("+0%", DashboardFormatter.formatDelta(0.0))
    }

    @Test
    fun `formatDelta rounds one decimal to integer`() {
        // 15.6 → "%.0f" → "16%"
        assertEquals("+16%", DashboardFormatter.formatDelta(15.6))
    }

    /**
     * Edge case: delta rất nhỏ (0.4) làm tròn xuống 0 — caller có thể nhầm
     * "không đổi" thành "tăng nhẹ". Test document behavior hiện tại.
     */
    @Test
    fun `formatDelta sub-percent positive rounds to plus zero`() {
        // 0.4 → %.0f → "0" → "+0%"
        assertEquals("+0%", DashboardFormatter.formatDelta(0.4))
    }
}

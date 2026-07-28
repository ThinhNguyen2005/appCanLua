package com.giathinh.canlua.ui.screen.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.Locale

/**
 * Unit tests cho [MapFormatters] — helpers format số và ngày trên bản đồ.
 *
 * MapFormatters là `internal object` — có thể test từ cùng module (test source set).
 * NumberFormat dùng vi-VN locale (dấu "." làm thousand separator).
 * SimpleDateFormat dùng vi-VN locale → output ngày dạng dd/MM/yyyy.
 *
 * Lưu ý: `MapFormatters.date` phụ thuộc timezone của JVM test runner.
 * Sử dụng Date với timestamp cố định và verify format pattern thay vì giá trị cụ thể.
 */
class MapFormattersTest {

    @Before
    fun setUp() {
        // Đảm bảo String.format locale ổn định trên máy CI / dev khác nhau.
        Locale.setDefault(Locale.US)
    }

    // === number: integer values ===

    @Test
    fun `number - integer double formats without decimal`() {
        // 1000.0 % 1.0 == 0.0 → dùng vi-VN NumberFormat → "1.000"
        val result = MapFormatters.number(1000.0)
        assertEquals("1.000", result)
    }

    @Test
    fun `number - zero integer formats as 0`() {
        assertEquals("0", MapFormatters.number(0.0))
    }

    @Test
    fun `number - small integer no thousand separator`() {
        assertEquals("500", MapFormatters.number(500.0))
    }

    @Test
    fun `number - large integer formats with vi-VN thousand separator`() {
        // vi-VN uses "." as thousand separator
        assertEquals("1.000.000", MapFormatters.number(1_000_000.0))
    }

    @Test
    fun `number - negative integer formats correctly`() {
        // -1000.0 % 1.0 == 0.0 → integer branch
        val result = MapFormatters.number(-1000.0)
        assertEquals("-1.000", result)
    }

    // === number: decimal values ===

    @Test
    fun `number - decimal value uses 2 decimal places`() {
        // 1000.5 % 1.0 != 0 → "%.2f" branch
        assertEquals("1000.50", MapFormatters.number(1000.5))
    }

    @Test
    fun `number - small decimal`() {
        assertEquals("0.50", MapFormatters.number(0.5))
    }

    @Test
    fun `number - value with more than 2 decimal digits gets truncated to 2`() {
        // "%.2f" rounds to 2 decimal places
        assertEquals("10.13", MapFormatters.number(10.125))
    }

    @Test
    fun `number - exactly 2 decimal digits preserved`() {
        assertEquals("250.75", MapFormatters.number(250.75))
    }

    @Test
    fun `number - negative decimal formats correctly`() {
        // -0.5 % 1.0 != 0 → decimal branch
        assertTrue(MapFormatters.number(-0.5).contains("-"))
    }

    // === Boundary: is it integer or decimal? ===

    @Test
    fun `number - 0_1 is not integer`() {
        // 0.1 % 1.0 = 0.1 != 0 → decimal branch
        assertEquals("0.10", MapFormatters.number(0.1))
    }

    @Test
    fun `number - 1_0 is integer`() {
        // 1.0 % 1.0 = 0.0 → integer branch
        assertEquals("1", MapFormatters.number(1.0))
    }

    @Test
    fun `number - weight tonnage scenario 900 kg`() {
        // Thực tế: 900 kg tổng một cụm điểm → integer
        assertEquals("900", MapFormatters.number(900.0))
    }

    @Test
    fun `number - typical net weight with decimal 874_4`() {
        assertEquals("874.40", MapFormatters.number(874.4))
    }

    // === date format ===

    @Test
    fun `date returns string in dd-MM-yyyy pattern`() {
        // Date(0) = 1970-01-01 00:00:00 UTC. Kết quả phụ thuộc timezone JVM.
        // Chỉ verify pattern: 2 digits / 2 digits / 4 digits.
        val result = MapFormatters.date(Date(0L))
        assertTrue(
            "Format phải khớp dd/MM/yyyy (got: $result)",
            result.matches(Regex("""\d{2}/\d{2}/\d{4}"""))
        )
    }

    @Test
    fun `date - known timestamp formats consistently`() {
        // 2024-06-15 UTC: 1718409600000L
        val result = MapFormatters.date(Date(1718409600000L))
        assertTrue(
            "Format phải khớp dd/MM/yyyy (got: $result)",
            result.matches(Regex("""\d{2}/\d{2}/\d{4}"""))
        )
    }

    @Test
    fun `date - year part is always 4 digits`() {
        val result = MapFormatters.date(Date(System.currentTimeMillis()))
        val yearPart = result.substringAfterLast("/")
        assertEquals("Year phải có đúng 4 chữ số", 4, yearPart.length)
    }

    @Test
    fun `date - day and month are zero-padded`() {
        // Find a date where day < 10 and month < 10
        // Jan 5, 2024 UTC = 1704412800000L (approx)
        val result = MapFormatters.date(Date(1704067200000L + 4 * 86400000L)) // Jan 5 UTC
        val parts = result.split("/")
        assertEquals("Phải có 3 phần dd/MM/yyyy", 3, parts.size)
        assertTrue("Ngày phải có đúng 2 chữ số", parts[0].length == 2)
        assertTrue("Tháng phải có đúng 2 chữ số", parts[1].length == 2)
    }
}

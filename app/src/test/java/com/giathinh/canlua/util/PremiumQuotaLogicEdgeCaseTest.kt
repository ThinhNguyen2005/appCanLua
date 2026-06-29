package com.giathinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Tests bổ sung cho [PremiumQuotaLogic] — edge case và scenario tấn công bị bỏ qua.
 */
class PremiumQuotaLogicEdgeCaseTest {

    // === todayKey — edge cases ===

    @Test
    fun `todayKey January first formatted with zero padding`() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 1) }
        assertEquals("2026-01-01", PremiumQuotaLogic.todayKey(cal))
    }

    @Test
    fun `todayKey single digit day zero padded`() {
        val cal = Calendar.getInstance().apply { set(2024, Calendar.MARCH, 7) }
        assertEquals("2024-03-07", PremiumQuotaLogic.todayKey(cal))
    }

    @Test
    fun `todayKey leap year Feb 29 is valid`() {
        val cal = Calendar.getInstance().apply { set(2024, Calendar.FEBRUARY, 29) }
        assertEquals("2024-02-29", PremiumQuotaLogic.todayKey(cal))
    }

    @Test
    fun `todayKey no-arg uses system clock and produces valid format`() {
        val key = PremiumQuotaLogic.todayKey()
        assertTrue(
            "Format phải là yyyy-MM-dd (got: $key)",
            key.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
        )
    }

    @Test
    fun `todayKey year 2099 extreme future`() {
        val cal = Calendar.getInstance().apply { set(2099, Calendar.DECEMBER, 31) }
        assertEquals("2099-12-31", PremiumQuotaLogic.todayKey(cal))
    }

    // === rolloverDailyCount — edge cases ===

    @Test
    fun `rollover empty string savedDate treated as mismatch`() {
        // Empty string != todayKey → reset
        val (count, date) = PremiumQuotaLogic.rolloverDailyCount(
            savedDate = "",
            savedCount = 5,
            todayKey = "2026-06-14"
        )
        assertEquals(0, count)
        assertEquals("2026-06-14", date)
    }

    @Test
    fun `rollover savedCount zero stays zero same day`() {
        val (count, _) = PremiumQuotaLogic.rolloverDailyCount(
            savedDate = "2026-06-14",
            savedCount = 0,
            todayKey = "2026-06-14"
        )
        assertEquals(0, count)
    }

    @Test
    fun `rollover very large savedCount stays same day`() {
        // Premium counter có thể lớn về lý thuyết
        val (count, _) = PremiumQuotaLogic.rolloverDailyCount(
            savedDate = "2026-06-14",
            savedCount = Int.MAX_VALUE,
            todayKey = "2026-06-14"
        )
        assertEquals(Int.MAX_VALUE, count)
    }

    @Test
    fun `rollover returns todayKey as second element always`() {
        // Đảm bảo caller persist đúng key
        val today = "2026-06-14"
        val (_, dateToWrite) = PremiumQuotaLogic.rolloverDailyCount(
            savedDate = "old-date",
            savedCount = 99,
            todayKey = today
        )
        assertEquals(today, dateToWrite)
    }

    @Test
    fun `rollover with null savedCount=0 produces 0 and today`() {
        val (count, date) = PremiumQuotaLogic.rolloverDailyCount(
            savedDate = null,
            savedCount = 0,
            todayKey = "2026-01-01"
        )
        assertEquals(0, count)
        assertEquals("2026-01-01", date)
    }

    // === hasReachedFreeQuota — edge cases ===

    @Test
    fun `premium user with count 0 is not blocked`() {
        assertFalse(PremiumQuotaLogic.hasReachedFreeQuota(0, isPremium = true, maxPerDay = 3))
    }

    @Test
    fun `free user at count 0 is not blocked when quota is 1`() {
        assertFalse(PremiumQuotaLogic.hasReachedFreeQuota(0, isPremium = false, maxPerDay = 1))
    }

    @Test
    fun `free user at count 1 is blocked when quota is 1`() {
        assertTrue(PremiumQuotaLogic.hasReachedFreeQuota(1, isPremium = false, maxPerDay = 1))
    }

    @Test
    fun `free user with negative count is not blocked`() {
        // Không nên xảy ra nhưng defensive: -1 < 3 → không block
        assertFalse(PremiumQuotaLogic.hasReachedFreeQuota(-1, isPremium = false, maxPerDay = 3))
    }

    @Test
    fun `free user with very large maxPerDay is never blocked in practice`() {
        // maxPerDay = Int.MAX_VALUE → user free cần tạo 2 tỷ phiếu mới bị block
        assertFalse(
            PremiumQuotaLogic.hasReachedFreeQuota(
                currentCount = 1000,
                isPremium = false,
                maxPerDay = Int.MAX_VALUE
            )
        )
    }

    @Test
    fun `blocked state is idempotent - calling twice with same args gives same result`() {
        val r1 = PremiumQuotaLogic.hasReachedFreeQuota(3, false, 3)
        val r2 = PremiumQuotaLogic.hasReachedFreeQuota(3, false, 3)
        assertEquals(r1, r2)
    }

    // === Integration: full daily usage flow ===

    @Test
    fun `full flow - fresh install to quota exceeded`() {
        val today = "2026-06-14"
        val maxPerDay = 3

        // Fresh install: count = 0
        val (count0, _) = PremiumQuotaLogic.rolloverDailyCount(null, 0, today)
        assertFalse("Chưa tạo phiếu nào → chưa block",
            PremiumQuotaLogic.hasReachedFreeQuota(count0, false, maxPerDay))

        // Tạo phiếu 1, 2, 3 → mỗi lần tăng count
        assertFalse(PremiumQuotaLogic.hasReachedFreeQuota(1, false, maxPerDay))
        assertFalse(PremiumQuotaLogic.hasReachedFreeQuota(2, false, maxPerDay))
        assertTrue("Phiếu thứ 3 → đạt quota → block",
            PremiumQuotaLogic.hasReachedFreeQuota(3, false, maxPerDay))
    }

    @Test
    fun `full flow - next day resets count`() {
        val yesterday = "2026-06-13"
        val today = "2026-06-14"
        val maxPerDay = 3

        // Hôm qua dùng hết quota
        assertTrue(PremiumQuotaLogic.hasReachedFreeQuota(3, false, maxPerDay))

        // Hôm nay: rollover reset về 0
        val (newCount, _) = PremiumQuotaLogic.rolloverDailyCount(yesterday, 3, today)
        assertEquals(0, newCount)
        assertFalse("Sau midnight reset → có thể tạo phiếu lại",
            PremiumQuotaLogic.hasReachedFreeQuota(newCount, false, maxPerDay))
    }
}

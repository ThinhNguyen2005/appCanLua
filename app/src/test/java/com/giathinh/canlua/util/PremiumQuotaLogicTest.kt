package com.giathinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests cho [PremiumQuotaLogic] — chống gian lận quota free user.
 *
 * Critical test: nếu logic sai → user free tạo phiếu vô hạn → mất doanh thu.
 */
class PremiumQuotaLogicTest {

    // === todayKey ===

    @Test
    fun `todayKey formats yyyy-MM-dd with zero padding`() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.MAY, 5) }
        assertEquals("2026-05-05", PremiumQuotaLogic.todayKey(cal))
    }

    @Test
    fun `todayKey handles December correctly`() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.DECEMBER, 31) }
        assertEquals("2026-12-31", PremiumQuotaLogic.todayKey(cal))
    }

    @Test
    fun `todayKey same calendar produces same key`() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.JUNE, 15) }
        // Idempotent — gọi nhiều lần cho cùng calendar phải trả cùng key.
        val k1 = PremiumQuotaLogic.todayKey(cal)
        val k2 = PremiumQuotaLogic.todayKey(cal)
        assertEquals(k1, k2)
    }

    // === rolloverDailyCount ===

    @Test
    fun `rollover keeps count when saved date matches today`() {
        val (count, date) = PremiumQuotaLogic.rolloverDailyCount(
            savedDate = "2026-05-20",
            savedCount = 3,
            todayKey = "2026-05-20"
        )
        assertEquals(3, count)
        assertEquals("2026-05-20", date)
    }

    @Test
    fun `rollover resets count when saved date differs from today`() {
        val (count, date) = PremiumQuotaLogic.rolloverDailyCount(
            savedDate = "2026-05-19",
            savedCount = 99,
            todayKey = "2026-05-20"
        )
        assertEquals(0, count)
        assertEquals("2026-05-20", date)
    }

    @Test
    fun `rollover handles null saved date as fresh install`() {
        val (count, date) = PremiumQuotaLogic.rolloverDailyCount(
            savedDate = null,
            savedCount = 0,
            todayKey = "2026-05-20"
        )
        assertEquals(0, count)
        assertEquals("2026-05-20", date)
    }

    /**
     * Edge case: user chỉnh giờ máy lùi về hôm qua → todayKey đổi → count
     * bị reset. Đây là vector tấn công đã biết — sẽ chuyển counter lên server
     * trong sprint sau (xem comment KEY_DAILY_COUNT trong PremiumState).
     */
    @Test
    fun `rollover resets if user moves clock back to past day - documented attack`() {
        val (count, _) = PremiumQuotaLogic.rolloverDailyCount(
            savedDate = "2026-05-20",
            savedCount = 3,
            todayKey = "2026-05-19"  // user lùi clock
        )
        assertEquals(0, count) // Bug đã biết — phải fix server-side
    }

    // === hasReachedFreeQuota ===

    @Test
    fun `premium user never blocked even at high count`() {
        assertFalse(PremiumQuotaLogic.hasReachedFreeQuota(currentCount = 999, isPremium = true, maxPerDay = 3))
    }

    @Test
    fun `free user blocked at exact quota`() {
        assertTrue(PremiumQuotaLogic.hasReachedFreeQuota(currentCount = 3, isPremium = false, maxPerDay = 3))
    }

    @Test
    fun `free user not blocked below quota`() {
        assertFalse(PremiumQuotaLogic.hasReachedFreeQuota(currentCount = 2, isPremium = false, maxPerDay = 3))
    }

    @Test
    fun `free user blocked above quota - delete-and-recreate attack scenario`() {
        // User free đã tạo 5 phiếu → xoá 2 → còn 3 phiếu trong DB.
        // Counter SharedPref = 5 (chỉ tăng, không trừ).
        // Tạo phiếu mới → check: 5 >= 3 → block. Bug bypass đã đóng.
        assertTrue(PremiumQuotaLogic.hasReachedFreeQuota(currentCount = 5, isPremium = false, maxPerDay = 3))
    }

    @Test
    fun `quota of zero blocks immediately`() {
        // Edge case: future config có thể đặt 0 phiếu cho 1 tier.
        assertTrue(PremiumQuotaLogic.hasReachedFreeQuota(currentCount = 0, isPremium = false, maxPerDay = 0))
    }
}

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
    fun `todayKey formats yyyy-MM-dd with zero padding across boundary dates`() {
        data class Case(val year: Int, val month: Int, val day: Int, val expected: String)
        listOf(
            Case(2026, Calendar.JANUARY, 1, "2026-01-01"),
            Case(2024, Calendar.FEBRUARY, 29, "2024-02-29"),
            Case(2024, Calendar.MARCH, 7, "2024-03-07"),
            Case(2026, Calendar.MAY, 5, "2026-05-05"),
            Case(2026, Calendar.DECEMBER, 31, "2026-12-31"),
            Case(2099, Calendar.DECEMBER, 31, "2099-12-31")
        ).forEach { (year, month, day, expected) ->
            val cal = Calendar.getInstance().apply { set(year, month, day) }
            val k1 = PremiumQuotaLogic.todayKey(cal)
            val k2 = PremiumQuotaLogic.todayKey(cal)
            assertEquals(expected, k1)
            assertEquals("Idempotent cho cùng calendar", k1, k2)
        }
    }

    @Test
    fun `todayKey no-arg uses system clock and produces valid yyyy-MM-dd format`() {
        val key = PremiumQuotaLogic.todayKey()
        assertTrue(
            "Format phải là yyyy-MM-dd (got: $key)",
            key.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
        )
    }

    // === rolloverDailyCount ===

    @Test
    fun `rolloverDailyCount keeps count on same date and resets on mismatch or null`() {
        data class Case(
            val savedDate: String?,
            val savedCount: Int,
            val todayKey: String,
            val expectedCount: Int
        )
        listOf(
            Case("2026-05-20", 3, "2026-05-20", 3),
            Case("2026-06-14", 0, "2026-06-14", 0),
            Case("2026-06-14", Int.MAX_VALUE, "2026-06-14", Int.MAX_VALUE),
            Case("2026-05-19", 99, "2026-05-20", 0),
            Case(null, 0, "2026-05-20", 0),
            Case("", 5, "2026-06-14", 0),
            // Documented client-clock attack vector (moved clock backward → resets)
            Case("2026-05-20", 3, "2026-05-19", 0)
        ).forEach { (savedDate, savedCount, todayKey, expectedCount) ->
            val (newCount, persistedDate) = PremiumQuotaLogic.rolloverDailyCount(
                savedDate = savedDate,
                savedCount = savedCount,
                todayKey = todayKey
            )
            assertEquals("savedDate=$savedDate savedCount=$savedCount today=$todayKey", expectedCount, newCount)
            assertEquals(todayKey, persistedDate)
        }
    }

    // === hasReachedFreeQuota ===

    @Test
    fun `hasReachedFreeQuota enforces quota for free users and never blocks premium`() {
        data class Case(val count: Int, val isPremium: Boolean, val maxPerDay: Int, val expectedBlocked: Boolean)
        listOf(
            Case(0, isPremium = true, maxPerDay = 3, expectedBlocked = false),
            Case(999, isPremium = true, maxPerDay = 3, expectedBlocked = false),
            Case(-1, isPremium = false, maxPerDay = 3, expectedBlocked = false),
            Case(0, isPremium = false, maxPerDay = 1, expectedBlocked = false),
            Case(1, isPremium = false, maxPerDay = 1, expectedBlocked = true),
            Case(2, isPremium = false, maxPerDay = 3, expectedBlocked = false),
            Case(3, isPremium = false, maxPerDay = 3, expectedBlocked = true),
            // Delete-and-recreate scenario: monotonic counter stays >= limit
            Case(5, isPremium = false, maxPerDay = 3, expectedBlocked = true),
            Case(0, isPremium = false, maxPerDay = 0, expectedBlocked = true),
            Case(1000, isPremium = false, maxPerDay = Int.MAX_VALUE, expectedBlocked = false)
        ).forEach { (count, isPremium, maxPerDay, expectedBlocked) ->
            val r1 = PremiumQuotaLogic.hasReachedFreeQuota(count, isPremium, maxPerDay)
            val r2 = PremiumQuotaLogic.hasReachedFreeQuota(count, isPremium, maxPerDay)
            assertEquals("count=$count isPremium=$isPremium max=$maxPerDay", expectedBlocked, r1)
            assertEquals("Idempotent check", r1, r2)
        }
    }

    // === Integration flows ===

    @Test
    fun `full flow - fresh install to quota exceeded and next day reset`() {
        val yesterday = "2026-06-13"
        val today = "2026-06-14"
        val maxPerDay = 3

        val (count0, _) = PremiumQuotaLogic.rolloverDailyCount(null, 0, yesterday)
        assertFalse(PremiumQuotaLogic.hasReachedFreeQuota(count0, isPremium = false, maxPerDay = maxPerDay))
        assertFalse(PremiumQuotaLogic.hasReachedFreeQuota(1, isPremium = false, maxPerDay = maxPerDay))
        assertFalse(PremiumQuotaLogic.hasReachedFreeQuota(2, isPremium = false, maxPerDay = maxPerDay))
        assertTrue(PremiumQuotaLogic.hasReachedFreeQuota(3, isPremium = false, maxPerDay = maxPerDay))

        val (resetCount, _) = PremiumQuotaLogic.rolloverDailyCount(yesterday, 3, today)
        assertEquals(0, resetCount)
        assertFalse(PremiumQuotaLogic.hasReachedFreeQuota(resetCount, isPremium = false, maxPerDay = maxPerDay))
    }
}

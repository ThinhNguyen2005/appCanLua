package com.GiaThinh.canlua.util

/**
 * Logic pure cho Premium daily quota — extract khỏi `PremiumState` (vốn couple
 * với `Context` + `SharedPreferences`) để test được bằng JVM unit test, không
 * cần Robolectric.
 *
 * `PremiumState` gọi các function này rồi mới persist sang prefs.
 */
object PremiumQuotaLogic {

    /**
     * Tính daily count mới khi rollover. Nếu `savedDate != todayKey` → ngày
     * mới → reset về 0. Cùng ngày → giữ counter cũ.
     *
     * Trả về `Pair(newCount, dateToWrite)` để caller persist atomic.
     */
    fun rolloverDailyCount(savedDate: String?, savedCount: Int, todayKey: String): Pair<Int, String> {
        return if (savedDate != todayKey) {
            0 to todayKey
        } else {
            savedCount to todayKey
        }
    }

    /**
     * Format ngày "yyyy-MM-dd" theo local timezone — khoá rollover.
     * Dùng `Calendar` thay vì `LocalDate` để tránh phụ thuộc Java 8 desugar.
     */
    fun todayKey(now: java.util.Calendar = java.util.Calendar.getInstance()): String {
        return "%04d-%02d-%02d".format(
            now.get(java.util.Calendar.YEAR),
            now.get(java.util.Calendar.MONTH) + 1,
            now.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * User có vượt quota free không?
     * Premium → luôn `false` (không bao giờ block).
     * Free + đã đạt `FREE_CARDS_PER_DAY` → `true` → UI mở dialog upsell.
     */
    fun hasReachedFreeQuota(currentCount: Int, isPremium: Boolean, maxPerDay: Int): Boolean {
        if (isPremium) return false
        return currentCount >= maxPerDay
    }
}

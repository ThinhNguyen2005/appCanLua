package com.GiaThinh.canlua.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Trạng thái Premium toàn cục — Offline-First.
 *
 * Lưu trữ:
 *  - SharedPreferences thường ("premium_prefs") cho lần đầu cài app.
 *  - Đọc trở lại ngay khi gọi `init(context)` để StateFlow không "flicker false".
 *
 * Grace period 30 ngày: nếu thiết bị offline quá 30 ngày kể từ `lastVerifiedAt`,
 * Premium sẽ tạm tắt (yêu cầu kết nối lại để xác thực). Khi không có mạng nhưng
 * còn trong grace, app vẫn tin user là Premium → nông dân/thương lái đi đồng được.
 *
 * Đây là mock client-side — phiên bản production sẽ verify Google Play
 * BillingClient + Firebase rule. Hiện tại flip cờ `isPremium` ngay sau "thanh toán"
 * để demo flow ẩn quảng cáo reactive.
 */
object PremiumState {

    private const val PREFS = "premium_prefs"
    private const val KEY_IS_PREMIUM = "is_premium"
    private const val KEY_LAST_VERIFIED = "last_verified_at"
    private const val KEY_PREMIUM_SINCE = "premium_since_ms"
    private const val KEY_PLAN = "premium_plan"

    /**
     * Counter chống gian lận: số phiếu user tạo HÔM NAY (theo `KEY_DAILY_DATE`).
     * Chỉ tăng — xoá phiếu KHÔNG giảm counter → user không thể xoá bớt phiếu cũ
     * để tạo phiếu mới khi vượt FREE_CARDS_PER_DAY.
     *
     * Reset tự động khi sang ngày mới (so với `KEY_DAILY_DATE` = "yyyy-MM-dd").
     *
     * Hardening sau: chuyển counter lên Firestore (server-side timestamp) để
     * user clear app data cũng không reset được.
     */
    private const val KEY_DAILY_COUNT = "daily_created_count"
    private const val KEY_DAILY_DATE = "daily_counter_date"

    private const val GRACE_MS = 30L * 24L * 60L * 60L * 1000L // 30 ngày

    /**
     * Quota giới hạn cho user FREE — vượt quota mở dialog upsell Premium.
     * Số chọn 3 phiếu/ngày = đủ cho nông dân nhỏ test app, đủ thấy giá trị
     * Premium khi mùa vụ nhiều phiếu/ngày.
     */
    const val FREE_CARDS_PER_DAY = 3

    /** Snapshot trạng thái Premium cho UI (badge, status card). */
    data class Info(
        val isActive: Boolean,
        val plan: String? = null,        // VD: "Gói Cả Năm"
        val sinceMs: Long = 0L,          // Thời điểm bắt đầu Premium
        val lastVerifiedMs: Long = 0L    // Lần xác thực gần nhất với mạng
    )

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _info = MutableStateFlow(Info(isActive = false))
    val info: StateFlow<Info> = _info.asStateFlow()

    /** Số phiếu đã tạo hôm nay (counter chỉ tăng, không trừ khi xoá). */
    private val _dailyCreated = MutableStateFlow(0)
    val dailyCreated: StateFlow<Int> = _dailyCreated.asStateFlow()

    fun init(context: Context) {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getBoolean(KEY_IS_PREMIUM, false)
        val lastVerified = prefs.getLong(KEY_LAST_VERIFIED, 0L)
        val since = prefs.getLong(KEY_PREMIUM_SINCE, 0L)
        val plan = prefs.getString(KEY_PLAN, null)
        val now = System.currentTimeMillis()
        val withinGrace = lastVerified > 0L && (now - lastVerified) <= GRACE_MS
        val active = stored && withinGrace
        _isPremium.value = active
        _info.value = Info(
            isActive = active,
            plan = plan,
            sinceMs = since,
            lastVerifiedMs = lastVerified
        )
        // Sync counter — auto reset nếu hôm nay khác ngày lưu trong prefs.
        _dailyCreated.value = readAndRolloverDailyCount(context)
    }

    /**
     * Đọc daily counter, auto reset về 0 nếu sang ngày mới.
     * Idempotent — gọi nhiều lần trong cùng ngày trả cùng 1 số.
     * Logic rollover được delegate sang [PremiumQuotaLogic] để testable.
     */
    private fun readAndRolloverDailyCount(context: Context): Int {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = PremiumQuotaLogic.todayKey()
        val savedDate = prefs.getString(KEY_DAILY_DATE, null)
        val savedCount = prefs.getInt(KEY_DAILY_COUNT, 0)
        val (newCount, newDate) = PremiumQuotaLogic.rolloverDailyCount(
            savedDate = savedDate,
            savedCount = savedCount,
            todayKey = today
        )
        // Chỉ ghi prefs khi rollover thực sự đổi date (giảm IO).
        if (savedDate != newDate) {
            prefs.edit()
                .putString(KEY_DAILY_DATE, newDate)
                .putInt(KEY_DAILY_COUNT, newCount)
                .apply()
        }
        return newCount
    }

    /**
     * Tăng counter lên 1 sau khi user tạo thành công 1 phiếu cân.
     * Counter này chỉ tăng — xoá phiếu KHÔNG giảm. User không thể bypass quota
     * bằng cách xoá bớt phiếu cũ rồi tạo lại.
     *
     * Trả về số mới sau khi tăng.
     */
    fun incrementDailyCreated(context: Context): Int {
        // Rollover trước khi tăng — đảm bảo nếu đã qua 00:00 thì counter reset về 1.
        val current = readAndRolloverDailyCount(context)
        val next = current + 1
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DAILY_COUNT, next)
            .apply()
        _dailyCreated.value = next
        return next
    }

    fun setPremium(context: Context, value: Boolean, plan: String? = null) {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        // Giữ nguyên `since` cũ nếu user đã từng Premium — chỉ ghi lần đầu kích hoạt.
        val existingSince = prefs.getLong(KEY_PREMIUM_SINCE, 0L)
        val newSince = when {
            !value -> 0L                        // Hủy Premium → reset
            existingSince > 0L -> existingSince // Đã có sẵn → giữ
            else -> now                         // Lần đầu → ghi giờ
        }
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, value)
            .putLong(KEY_LAST_VERIFIED, if (value) now else 0L)
            .putLong(KEY_PREMIUM_SINCE, newSince)
            .apply { if (plan != null) putString(KEY_PLAN, plan) }
            .apply()

        _isPremium.value = value
        _info.value = Info(
            isActive = value,
            plan = if (value) plan ?: prefs.getString(KEY_PLAN, null) else null,
            sinceMs = newSince,
            lastVerifiedMs = if (value) now else 0L
        )
    }
}

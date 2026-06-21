package com.giathinh.canlua.util

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
 * Early Adopter: user cài app TRƯỚC `EARLY_ADOPTER_CUTOFF_MS` (2026-06-30 00:00:00 ICT)
 * sẽ được tự động nhận Premium vĩnh viễn — không giới hạn phiếu, không quảng cáo.
 * Bật/tắt feature qua Firebase Remote Config key `early_adopter_enabled`.
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
    private const val KEY_AI_DAILY_COUNT = "ai_daily_created_count"
    private const val KEY_AI_DAILY_DATE = "ai_daily_counter_date"

    /** Key prefs để user không bị apply early adopter nhiều lần. */
    private const val KEY_EARLY_ADOPTER_APPLIED = "early_adopter_applied"

    private const val GRACE_MS = 30L * 24L * 60L * 60L * 1000L // 30 ngày

    /**
     * Quota giới hạn cho user FREE — vượt quota mở dialog upsell Premium.
     * Số chọn 3 phiếu/ngày = đủ cho nông dân nhỏ test app, đủ thấy giá trị
     * Premium khi mùa vụ nhiều phiếu/ngày.
     */
    const val FREE_CARDS_PER_DAY = 3
    const val FREE_AI_QUERIES_PER_DAY = 3
    const val PREMIUM_AI_QUERIES_PER_DAY = 100

    /**
     * Timestamp cutoff cho Early Adopter — cài app TRƯỚC ngày này = nhận Premium free.
     * Mặc định: 2026-07-01 00:00:00 ICT (Indochina Time = UTC+7).
     *
     * Giá trị này bị override bởi Firebase Remote Config key
     * `early_adopter_cutoff_ms` khi feature enabled.
     *
     * ĐỔI NGÀY NÀY khi muốn đóng early adopter:
     *  - Set ngày release chính thức → user cài sau ngày đó không nhận.
     *  - VD: muốn đóng ngày 2026-08-01 ICT → 1785526800000L
     */
    const val EARLY_ADOPTER_CUTOFF_MS = 1782838800000L // 2026-07-01 00:00:00 ICT

    /** Plan name cho Early Adopter — hiển thị trên badge/profile. */
    const val EARLY_ADOPTER_PLAN = "Early Adopter"

    /** Snapshot trạng thái Premium cho UI (badge, status card). */
    data class Info(
        val isActive: Boolean,
        val plan: String? = null,
        val sinceMs: Long = 0L,
        val lastVerifiedMs: Long = 0L,
        val isEarlyAdopter: Boolean = false
    )

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _info = MutableStateFlow(Info(isActive = false))
    val info: StateFlow<Info> = _info.asStateFlow()

    /** Số phiếu đã tạo hôm nay (counter chỉ tăng, không trừ khi xoá). */
    private val _dailyCreated = MutableStateFlow(0)
    val dailyCreated: StateFlow<Int> = _dailyCreated.asStateFlow()

    /** Số lượt chat AI hôm nay (counter chỉ tăng). */
    private val _dailyAiQueries = MutableStateFlow(0)
    val dailyAiQueries: StateFlow<Int> = _dailyAiQueries.asStateFlow()

    /**
     * Kiểm tra và apply Early Adopter Premium nếu thỏa điều kiện.
     * Gọi 1 lần trong Application.onCreate() SAU khi PremiumState.init() đã chạy.
     *
     * @param firstInstallTimeMs thời điểm cài app (lấy từ PackageManager)
     * @param earlyAdopterEnabled feature có đang bật không (từ Firebase Remote Config)
     * @param remoteCutoffMs timestamp cutoff từ Remote Config (null = dùng default)
     */
    private fun getEncryptedPrefs(context: Context): android.content.SharedPreferences {
        val appCtx = context.applicationContext
        val basePrefs = try {
            val masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
            androidx.security.crypto.EncryptedSharedPreferences.create(
                PREFS,
                masterKeyAlias,
                appCtx,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Clear the corrupted/unreadable preferences file on creation failure
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    appCtx.deleteSharedPreferences(PREFS)
                } else {
                    appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
                }
            } catch (clearEx: Exception) {
                clearEx.printStackTrace()
            }
            try {
                val masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
                androidx.security.crypto.EncryptedSharedPreferences.create(
                    PREFS,
                    masterKeyAlias,
                    appCtx,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (fallbackEx: Exception) {
                // Fallback to clear text if Encrypted SharedPreferences is completely broken on this device
                appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            }
        }
        return SafeEncryptedSharedPreferences(basePrefs, appCtx, PREFS)
    }

    /**
     * Kiểm tra và apply Early Adopter Premium nếu thỏa điều kiện.
     * Gọi 1 lần trong Application.onCreate() SAU khi PremiumState.init() đã chạy.
     *
     * @param firstInstallTimeMs thời điểm cài app (lấy từ PackageManager)
     * @param earlyAdopterEnabled feature có đang bật không (từ Firebase Remote Config)
     * @param remoteCutoffMs timestamp cutoff từ Remote Config (null = dùng default)
     */
    fun applyEarlyAdopterIfEligible(
        context: Context,
        firstInstallTimeMs: Long,
        earlyAdopterEnabled: Boolean = true,
        remoteCutoffMs: Long? = null
    ) {
        // Feature bị tắt từ Remote Config → bỏ qua
        if (!earlyAdopterEnabled) return

        // CHECK GUEST MODE: Khách không được hưởng ưu đãi Early Adopter
        val isGuest = context.applicationContext
            .getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getBoolean("guest_mode", false)
        if (isGuest) return

        val prefs = getEncryptedPrefs(context)
        val cutoff = remoteCutoffMs ?: EARLY_ADOPTER_CUTOFF_MS
        val alreadyPremium = prefs.getBoolean(KEY_IS_PREMIUM, false)
        val isApplied = prefs.getBoolean(KEY_EARLY_ADOPTER_APPLIED, false)

        // Nếu thiết bị cài trước ngày cutoff
        if (firstInstallTimeMs <= cutoff) {
            // Nếu chưa Premium hoặc chưa lưu cờ đã apply → thực hiện apply/gia hạn
            if (!alreadyPremium || !isApplied) {
                val now = System.currentTimeMillis()
                prefs.edit()
                    .putBoolean(KEY_EARLY_ADOPTER_APPLIED, true)
                    .putBoolean(KEY_IS_PREMIUM, true)
                    .putLong(KEY_LAST_VERIFIED, now)
                    .putLong(KEY_PREMIUM_SINCE, now)
                    .putString(KEY_PLAN, EARLY_ADOPTER_PLAN)
                    .apply()

                _isPremium.value = true
                _info.value = Info(
                    isActive = true,
                    plan = EARLY_ADOPTER_PLAN,
                    sinceMs = now,
                    lastVerifiedMs = now,
                    isEarlyAdopter = true
                )

                AnalyticsHelper.setPremium(true, EARLY_ADOPTER_PLAN)
            }
        }
    }

    fun init(context: Context) {
        val prefs = getEncryptedPrefs(context)
        
        // CHECK GUEST MODE: Khách không bao giờ được hưởng các đặc quyền Premium
        val isGuest = context.applicationContext
            .getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getBoolean("guest_mode", false)

        val stored = if (isGuest) false else prefs.getBoolean(KEY_IS_PREMIUM, false)
        val lastVerified = prefs.getLong(KEY_LAST_VERIFIED, 0L)
        val since = prefs.getLong(KEY_PREMIUM_SINCE, 0L)
        val plan = if (isGuest) null else prefs.getString(KEY_PLAN, null)
        val isEarlyAdopterApplied = !isGuest && prefs.getBoolean(KEY_EARLY_ADOPTER_APPLIED, false)
        val now = System.currentTimeMillis()
        val withinGrace = lastVerified > 0L && (now - lastVerified) <= GRACE_MS
        val active = stored && withinGrace && !isGuest

        _isPremium.value = active
        _info.value = Info(
            isActive = active,
            plan = plan,
            sinceMs = since,
            lastVerifiedMs = lastVerified,
            isEarlyAdopter = isEarlyAdopterApplied && plan == EARLY_ADOPTER_PLAN
        )
        // Sync counter — auto reset nếu hôm nay khác ngày lưu trong prefs.
        _dailyCreated.value = readAndRolloverDailyCount(context)
        _dailyAiQueries.value = readAndRolloverDailyAiCount(context)
    }

    /**
     * Đọc daily counter, auto reset về 0 nếu sang ngày mới.
     * Idempotent — gọi nhiều lần trong cùng ngày trả cùng 1 số.
     * Logic rollover được delegate sang [PremiumQuotaLogic] để testable.
     */
    private fun readAndRolloverDailyCount(context: Context): Int {
        val prefs = getEncryptedPrefs(context)
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
        getEncryptedPrefs(context)
            .edit()
            .putInt(KEY_DAILY_COUNT, next)
            .apply()
        _dailyCreated.value = next
        return next
    }

    private fun readAndRolloverDailyAiCount(context: Context): Int {
        val prefs = getEncryptedPrefs(context)
        val today = PremiumQuotaLogic.todayKey()
        val savedDate = prefs.getString(KEY_AI_DAILY_DATE, null)
        val savedCount = prefs.getInt(KEY_AI_DAILY_COUNT, 0)
        val (newCount, newDate) = PremiumQuotaLogic.rolloverDailyCount(
            savedDate = savedDate,
            savedCount = savedCount,
            todayKey = today
        )
        if (savedDate != newDate) {
            prefs.edit()
                .putString(KEY_AI_DAILY_DATE, newDate)
                .putInt(KEY_AI_DAILY_COUNT, newCount)
                .apply()
        }
        return newCount
    }

    fun incrementDailyAiCreated(context: Context): Int {
        val current = readAndRolloverDailyAiCount(context)
        val next = current + 1
        getEncryptedPrefs(context)
            .edit()
            .putInt(KEY_AI_DAILY_COUNT, next)
            .apply()
        _dailyAiQueries.value = next
        return next
    }

    fun setPremium(context: Context, value: Boolean, plan: String? = null) {
        val prefs = getEncryptedPrefs(context)
        val now = System.currentTimeMillis()
        // Giữ nguyên `since` cũ nếu user đã từng Premium — chỉ ghi lần đầu kích hoạt.
        val existingSince = prefs.getLong(KEY_PREMIUM_SINCE, 0L)
        val newSince = when {
            !value -> 0L
            existingSince > 0L -> existingSince
            else -> now
        }
        val editor = prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, value)
            .putLong(KEY_LAST_VERIFIED, if (value) now else 0L)
            .putLong(KEY_PREMIUM_SINCE, newSince)
        if (plan != null) editor.putString(KEY_PLAN, plan)
        editor.apply()

        _isPremium.value = value
        val finalPlan = if (value) plan ?: prefs.getString(KEY_PLAN, null) else null
        val isEa = prefs.getBoolean(KEY_EARLY_ADOPTER_APPLIED, false) && finalPlan == EARLY_ADOPTER_PLAN
        _info.value = Info(
            isActive = value,
            plan = finalPlan,
            sinceMs = newSince,
            lastVerifiedMs = if (value) now else 0L,
            isEarlyAdopter = isEa
        )
        // Telemetry: track Premium state change để segment analytics theo Free/Premium.
        AnalyticsHelper.setPremium(value, finalPlan)
    }
}

class SafeEncryptedSharedPreferences(
    private val delegate: android.content.SharedPreferences,
    private val context: Context,
    private val name: String
) : android.content.SharedPreferences {

    private fun handleDecryptionError(e: Throwable) {
        e.printStackTrace()
        try {
            delegate.edit().clear().commit()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(name)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun getAll(): MutableMap<String, *> {
        return try {
            delegate.all
        } catch (e: Throwable) {
            handleDecryptionError(e)
            mutableMapOf<String, Any>()
        }
    }

    override fun getString(key: String?, defValue: String?): String? {
        return try {
            delegate.getString(key, defValue)
        } catch (e: Throwable) {
            handleDecryptionError(e)
            defValue
        }
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        return try {
            delegate.getStringSet(key, defValues)
        } catch (e: Throwable) {
            handleDecryptionError(e)
            defValues
        }
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return try {
            delegate.getInt(key, defValue)
        } catch (e: Throwable) {
            handleDecryptionError(e)
            defValue
        }
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return try {
            delegate.getLong(key, defValue)
        } catch (e: Throwable) {
            handleDecryptionError(e)
            defValue
        }
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return try {
            delegate.getFloat(key, defValue)
        } catch (e: Throwable) {
            handleDecryptionError(e)
            defValue
        }
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return try {
            delegate.getBoolean(key, defValue)
        } catch (e: Throwable) {
            handleDecryptionError(e)
            defValue
        }
    }

    override fun contains(key: String?): Boolean {
        return try {
            delegate.contains(key)
        } catch (e: Throwable) {
            handleDecryptionError(e)
            false
        }
    }

    override fun edit(): android.content.SharedPreferences.Editor {
        return delegate.edit()
    }

    override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {
        delegate.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {
        delegate.unregisterOnSharedPreferenceChangeListener(listener)
    }
}

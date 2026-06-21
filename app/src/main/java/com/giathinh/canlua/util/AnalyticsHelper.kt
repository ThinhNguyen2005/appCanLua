package com.giathinh.canlua.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Wrapper Firebase Analytics + Crashlytics — gom logic telemetry vào 1 chỗ
 * để code app gọi tên ngữ nghĩa thay vì raw event name.
 *
 * Quy ước:
 *  - Event name dạng `snake_case` (Firebase requirement, max 40 chars).
 *  - Param name dạng `snake_case` (max 40 chars).
 *  - Không log PII (số CCCD, SĐT, email user) — chỉ uid hash + business metric.
 *  - Crashlytics opt-out tự động ở debug build (Firebase BoM mặc định) — production
 *    enable. Có thể override qua `FirebaseCrashlytics.setCrashlyticsCollectionEnabled`.
 */
object AnalyticsHelper {

    private lateinit var analytics: FirebaseAnalytics
    private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

    fun init(context: Context) {
        // FirebaseAnalytics yêu cầu Context — init explicit ở Application.onCreate.
        // Crashlytics dùng getInstance() (singleton, không cần context).
        analytics = FirebaseAnalytics.getInstance(context.applicationContext)
        crashlytics // touch lazy
    }

    /** User vừa đăng nhập thành công — set userId cho Analytics + Crashlytics. */
    fun setUser(uid: String?, role: String?) {
        analytics.setUserId(uid)
        if (uid != null) crashlytics.setUserId(uid)
        if (role != null) {
            analytics.setUserProperty("role", role)
            crashlytics.setCustomKey("role", role)
        }
    }

    /** Premium state thay đổi — gắn property để filter analytics theo Premium/Free. */
    fun setPremium(isPremium: Boolean, plan: String?) {
        analytics.setUserProperty("is_premium", isPremium.toString())
        analytics.setUserProperty("premium_plan", plan ?: "")
        crashlytics.setCustomKey("is_premium", isPremium)
    }

    // ─── Business events ───

    fun cardCreated(role: String, hasGps: Boolean) = log("card_created") {
        putString("role", role)
        putString("has_gps", hasGps.toString())
    }

    fun cardLocked(role: String) = log("card_locked") { putString("role", role) }

    fun cardDeleted() = log("card_deleted")

    fun premiumGateShown(cardsToday: Int) = log("premium_gate_shown") {
        putInt("cards_today", cardsToday)
    }

    fun premiumPurchased(plan: String) = log("premium_purchased") {
        putString("plan", plan)
    }

    fun syncSuccess(durationMs: Long, cardCount: Int) = log("sync_success") {
        putLong("duration_ms", durationMs)
        putInt("card_count", cardCount)
    }

    fun syncFailed(stage: String, errorClass: String) = log("sync_failed") {
        putString("stage", stage)
        putString("error_class", errorClass)
    }

    fun roleRequested() = log("role_requested")
    fun roleApproved() = log("role_approved")

    // ─── Exception logging ───

    /**
     * Log non-fatal error tới Crashlytics (không crash app).
     * Dùng cho lỗi catch được nhưng đáng monitor: sync fail, permission denied,
     * Firestore timeout, parse error.
     *
     * @param tag dùng làm subject/group trong Crashlytics console
     */
    fun logNonFatal(throwable: Throwable, tag: String? = null) {
        if (tag != null) crashlytics.setCustomKey("tag", tag)
        crashlytics.recordException(throwable)
    }

    /** Log breadcrumb — debug context kèm vào next crash. */
    fun breadcrumb(message: String) {
        crashlytics.log(message)
    }

    private inline fun log(eventName: String, paramsBuilder: Bundle.() -> Unit = {}) {
        val params = Bundle().apply(paramsBuilder)
        analytics.logEvent(eventName, params)
    }
}

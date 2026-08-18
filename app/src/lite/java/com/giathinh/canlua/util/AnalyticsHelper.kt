package com.giathinh.canlua.util

import android.content.Context
import android.util.Log

object AnalyticsHelper {
    fun init(context: Context) {}
    fun setUser(uid: String?, role: String?) {}
    fun setPremium(isPremium: Boolean, plan: String?) {}
    fun cardCreated(role: String, hasGps: Boolean) {}
    fun cardLocked(role: String) {}
    fun cardDeleted() {}
    fun premiumGateShown(cardsToday: Int) {}
    fun premiumPurchased(plan: String) {}
    fun syncSuccess(durationMs: Long, cardCount: Int) {}
    fun syncFailed(stage: String, errorClass: String) {}
    fun logNonFatal(throwable: Throwable, tag: String? = null) {
        Log.e(tag ?: "AnalyticsHelper", "Non-fatal error", throwable)
    }
    fun breadcrumb(message: String) {}
}

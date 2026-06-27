package com.giathinh.canlua.util

import android.content.Context

object AnalyticsHelper {

    fun init(context: Context) {
        // No-op
    }

    fun setUser(uid: String?, role: String?) {
        // No-op
    }

    fun setPremium(isPremium: Boolean, plan: String?) {
        // No-op
    }

    fun cardCreated(role: String, hasGps: Boolean) {}

    fun cardLocked(role: String) {}

    fun cardDeleted() {}

    fun premiumGateShown(cardsToday: Int) {}

    fun premiumPurchased(plan: String) {}

    fun syncSuccess(durationMs: Long, cardCount: Int) {}

    fun syncFailed(stage: String, errorClass: String) {}

    fun logNonFatal(throwable: Throwable, tag: String? = null) {
        // No-op
    }

    fun breadcrumb(message: String) {
        // No-op
    }
}

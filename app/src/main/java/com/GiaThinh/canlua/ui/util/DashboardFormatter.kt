package com.GiaThinh.canlua.ui.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Helper format số liệu cho dashboard — tối ưu cho hiển thị compact.
 *
 * Tránh số quá dài làm tràn UI:
 *  - 1,500,000 đ → "1.5tr"
 *  - 1,500,000,000 đ → "1.5 tỷ"
 *  - Cân nặng > 1000kg → "X.X tấn"
 */
object DashboardFormatter {

    private val viNumber = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))

    /** Money compact: 12.5tr / 1.4 tỷ / 850k. */
    fun money(amount: Double): String {
        val abs = abs(amount)
        return when {
            abs >= 1_000_000_000.0 -> "%.2f tỷ".format(amount / 1_000_000_000.0)
            abs >= 1_000_000.0 -> "%.1f tr".format(amount / 1_000_000.0)
            abs >= 1_000.0 -> "%.0fk".format(amount / 1_000.0)
            else -> "${viNumber.format(amount.toLong())}đ"
        }
    }

    /** Money full với đơn vị "đ" — dùng khi có chỗ. */
    fun moneyFull(amount: Double): String {
        return "${viNumber.format(amount.toLong())}đ"
    }

    /** Cân nặng: < 1000kg → kg, ≥ 1000kg → tấn. */
    fun weight(kg: Double): String {
        return if (kg >= 1_000.0) {
            "%.1f tấn".format(kg / 1_000.0)
        } else {
            "%.0f kg".format(kg)
        }
    }

    /** Tỉ lệ phần trăm với 1 chữ số sau dấu phẩy. */
    fun percent(value: Double): String = "%.1f%%".format(value)

    /**
     * Tính delta % giữa current và previous.
     * Trả về null nếu previous = 0 (không tính được tỉ lệ thay đổi).
     */
    fun deltaPercent(current: Double, previous: Double): Double? {
        if (previous == 0.0) return null
        return ((current - previous) / previous) * 100.0
    }

    /** Format delta thành "+15%" / "-8%" / "0%". */
    fun formatDelta(delta: Double?): String {
        if (delta == null) return "—"
        val sign = if (delta >= 0) "+" else ""
        return "$sign%.0f%%".format(delta)
    }
}

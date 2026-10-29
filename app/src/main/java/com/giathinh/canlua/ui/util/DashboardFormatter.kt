package com.giathinh.canlua.ui.util

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.giathinh.canlua.R
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class DashboardFormatter(private val res: Resources? = null) {
    private val locale: Locale = res?.configuration?.locales?.get(0) ?: Locale.forLanguageTag("vi-VN")
    private val compactNf = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }
    private val intNf = NumberFormat.getIntegerInstance(locale)

    fun money(amount: Double): String {
        val r = res ?: return Companion.money(amount)
        val a = abs(amount)
        return when {
            a >= 999_950_000 -> r.getString(R.string.compact_billion, compactNf.format(amount / 1e9))
            a >= 999_500 -> r.getString(R.string.compact_million, compactNf.format(amount / 1e6))
            a >= 1_000 -> r.getString(R.string.compact_thousand, intNf.format(amount / 1e3))
            else -> moneyFull(amount)
        }
    }

    fun moneyFull(amount: Double): String {
        val r = res ?: return Companion.moneyFull(amount)
        return r.getString(R.string.card_list_money_vnd, intNf.format(amount))
    }

    fun weight(kg: Double): String {
        val r = res ?: return Companion.weight(kg)
        return if (kg >= 999.5) r.getString(R.string.compact_ton, compactNf.format(kg / 1000))
        else r.getString(R.string.card_list_weight_kg, intNf.format(kg))
    }

    fun percent(value: Double): String {
        val r = res ?: return Companion.percent(value)
        return "${compactNf.format(value)}%"
    }

    fun deltaPercent(current: Double, previous: Double?): Double? =
        Companion.deltaPercent(current, previous)

    fun formatDelta(delta: Double?): String =
        Companion.formatDelta(delta)

    companion object {
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
        fun deltaPercent(current: Double, previous: Double?): Double? {
            if (previous == null || previous == 0.0) return null
            return ((current - previous) / previous) * 100.0
        }

        /** Format delta thành "+15%" / "-8%" / "0%". */
        fun formatDelta(delta: Double?): String {
            if (delta == null) return "—"
            val sign = if (delta >= 0) "+" else ""
            return "$sign%.0f%%".format(delta)
        }
    }
}

@Composable
fun rememberDashboardFormatter(): DashboardFormatter {
    val res = LocalContext.current.resources
    return remember(res.configuration) { DashboardFormatter(res) }
}
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

class DashboardFormatter(private val res: Resources) {
    private val locale: Locale = res.configuration.locales[0]
    private val compactNf = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }
    private val intNf = NumberFormat.getIntegerInstance(locale)

    fun money(amount: Double): String {
        val a = abs(amount)
        return when {
            a >= 999_950_000 -> res.getString(R.string.compact_billion, compactNf.format(amount / 1e9))
            a >= 999_500 -> res.getString(R.string.compact_million, compactNf.format(amount / 1e6))
            a >= 1_000 -> res.getString(R.string.compact_thousand, intNf.format(amount / 1e3))
            else -> moneyFull(amount)
        }
    }

    fun moneyFull(amount: Double): String =
        res.getString(R.string.card_list_money_vnd, intNf.format(amount))

    fun weight(kg: Double): String =
        if (kg >= 999.5) res.getString(R.string.compact_ton, compactNf.format(kg / 1000))
        else res.getString(R.string.card_list_weight_kg, intNf.format(kg))

    fun percent(value: Double): String = "${compactNf.format(value)}%"

    fun deltaPercent(current: Double, previous: Double): Double? {
        if (previous == 0.0) return null
        return ((current - previous) / previous) * 100.0
    }

    fun formatDelta(delta: Double?): String {
        if (delta == null || !delta.isFinite()) return "—"
        val r = delta.roundToInt()
        return if (r > 0) "+$r%" else "$r%"
    }
}

@Composable
fun rememberDashboardFormatter(): DashboardFormatter {
    val res = LocalContext.current.resources
    return remember(res.configuration) { DashboardFormatter(res) }
}
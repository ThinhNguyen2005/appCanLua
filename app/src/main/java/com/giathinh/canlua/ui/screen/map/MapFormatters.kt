package com.giathinh.canlua.ui.screen.map

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helpers format tập trung — tránh duplicate giữa MapContent (snippet) và
 * CardSummaryBottomSheet (display). Object để dùng được trong data class
 * `RiceClusterItem` (không thể gọi top-level fn từ companion).
 */
internal object MapFormatters {
    private val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))

    fun number(value: Double): String =
        if (value % 1.0 == 0.0) numberFormat.format(value.toLong())
        else "%.2f".format(value)

    fun date(date: Date): String = dateFormat.format(date)
}

package com.giathinh.canlua.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Format số liệu để đưa vào prompt AI. Cố định locale vi-VN và dùng số đầy đủ,
 * không phụ thuộc ngôn ngữ giao diện. Tạo NumberFormat mỗi lần gọi vì NumberFormat
 * không thread-safe (hàm này chạy trên Dispatchers.Default).
 */
object PromptFormat {
    private val locale: Locale = Locale.forLanguageTag("vi-VN")

    private fun int(v: Double): String =
        NumberFormat.getIntegerInstance(locale).format(v)

    private fun oneDecimal(v: Double): String =
        NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 0
        }.format(v)

    fun weight(kg: Double): String = "${int(kg)} kg"
    fun money(amount: Double): String = "${int(amount)} đ"
    fun percent(value: Double): String = "${oneDecimal(value)}%"

    fun delta(delta: Double): String {
        if (!delta.isFinite()) return "—"
        val r = delta.roundToInt()
        return if (r > 0) "+$r%" else "$r%"
    }
}
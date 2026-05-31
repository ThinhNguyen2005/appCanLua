package com.GiaThinh.canlua.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

/**
 * Format số nguyên kiểu tiền Việt: `12000` → `12.000`.
 *
 * - Raw value (giữ trong state) chỉ chứa digit, dễ parse `.toLongOrNull()`.
 * - OffsetMapping tính lại vị trí cursor sao cho cảm giác gõ tự nhiên,
 *   không nhảy lung tung khi insert/delete dấu chấm.
 *
 * Sử dụng:
 * ```
 * OutlinedTextField(
 *     value = priceStr,
 *     onValueChange = { priceStr = it.filter { c -> c.isDigit() } },
 *     visualTransformation = MoneyVisualTransformation,
 *     keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
 * )
 * ```
 */
object MoneyVisualTransformation : VisualTransformation {

    private val formatter: NumberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        if (digits.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }
        // Loại trừ leading zero (vd: "00012" → "12") để không hiển thị "0.0012"
        val normalized = digits.trimStart('0').ifEmpty { "0" }
        val long = normalized.toLongOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
        val formatted = formatter.format(long)

        // Đếm dấu phân cách giữa các digit để build offset mapping.
        // Locale vi-VN dùng `.` làm thousand separator.
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                // Số digit từ phải sang đến offset = digits.length - offset (nếu offset là cuối)
                // Đơn giản hơn: với offset trên raw, đếm số dot phải insert TRƯỚC offset.
                val digitsBeforeOffset = offset.coerceAtMost(digits.length)
                val totalDigits = digits.length
                val digitsFromRight = totalDigits - digitsBeforeOffset
                // Dot count = (totalDigits - 1) / 3, dotsAfterOffset = digitsFromRight / 3
                val totalDots = (totalDigits - 1) / 3
                val dotsAfter = digitsFromRight / 3
                val dotsBefore = totalDots - dotsAfter
                return (digitsBeforeOffset + dotsBefore).coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                // Đếm số dot trước offset trong formatted, raw offset = transformed - dots
                val sub = formatted.take(offset.coerceAtMost(formatted.length))
                val dots = sub.count { it == '.' }
                return (offset - dots).coerceAtMost(digits.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

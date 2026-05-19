package com.GiaThinh.canlua.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Parser markdown tối giản: chỉ xử lý `**bold**`, `*italic*`, và `` `code` ``.
 * Thiết kế để render lại text từ AI vốn hay dùng Markdown bold.
 *
 * Không phải parser đầy đủ — đủ tốt cho chat bubble, không edge-case xa xôi.
 */
fun parseInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val rest = text.substring(i)
        when {
            // Bold ** ... **  (kiểm tra trước italic vì 2 sao lồng 1 sao)
            rest.startsWith("**") -> {
                val end = text.indexOf("**", startIndex = i + 2)
                if (end > i + 2) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(i + 2, end))
                    pop()
                    i = end + 2
                } else {
                    append(text[i]); i++
                }
            }
            // Italic * ... *  (single, không bị nuốt bởi bold)
            rest.startsWith("*") -> {
                val end = text.indexOf("*", startIndex = i + 1)
                if (end > i + 1 && !text.substring(i + 1, end).contains("\n")) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append(text[i]); i++
                }
            }
            // Inline code `...`
            rest.startsWith("`") -> {
                val end = text.indexOf("`", startIndex = i + 1)
                if (end > i + 1) {
                    pushStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append(text[i]); i++
                }
            }
            else -> {
                append(text[i]); i++
            }
        }
    }
}

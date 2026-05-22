package com.GiaThinh.canlua.ui.util

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin

/**
 * Parser markdown tối giản: chỉ xử lý `**bold**`, `*italic*`, và `` `code` ``.
 * Thiết kế để render lại text từ AI vốn hay dùng Markdown bold.
 *
 * Không phải parser đầy đủ — đủ tốt cho chat bubble, không edge-case xa xôi.
 */
@Composable
fun AiMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color,
    linkColor: Color = textColor,
    textSizeSp: Float = 14f
) {
    val context = LocalContext.current
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(TablePlugin.create(context))
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColor.toArgb())
                setLinkTextColor(linkColor.toArgb())
                textSize = textSizeSp
                movementMethod = LinkMovementMethod.getInstance()
                setLineSpacing(0f, 1.1f)
            }
        },
        update = { view ->
            view.setTextColor(textColor.toArgb())
            view.setLinkTextColor(linkColor.toArgb())
            view.textSize = textSizeSp
            markwon.setMarkdown(view, markdown)
        }
    )
}

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

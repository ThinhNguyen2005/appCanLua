package com.GiaThinh.canlua.util

import android.text.Html
import java.security.MessageDigest

/**
 * Strip thẻ HTML khỏi mô tả RSS và decode entities (vd: &amp; → &).
 * Trả về plain text gọn gàng, max [maxChars].
 */
fun stripHtml(input: String, maxChars: Int = 240): String {
    if (input.isEmpty()) return ""
    val text = Html.fromHtml(input, Html.FROM_HTML_MODE_LEGACY).toString()
        .replace("\u00A0", " ")    // nbsp
        .replace(Regex("\\s+"), " ")
        .trim()
    return if (text.length > maxChars) text.take(maxChars - 1).trimEnd() + "…" else text
}

/**
 * Tìm URL ảnh đầu tiên trong description HTML hoặc enclosure tag.
 * Hỗ trợ cả `src="..."` và `src='...'`.
 */
fun extractFirstImg(html: String?): String? {
    if (html.isNullOrEmpty()) return null
    val pattern = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    return pattern.find(html)?.groupValues?.getOrNull(1)
}

/** SHA-256 hash dạng hex 64 chars — dùng làm primary key dedupe theo URL. */
fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

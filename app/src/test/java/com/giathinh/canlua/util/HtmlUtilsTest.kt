package com.giathinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests cho các helper trong HtmlUtils.kt.
 *
 * Lưu ý quan trọng:
 *  - [stripHtml] dùng `android.text.Html.fromHtml` — đây là Android framework API
 *    không chạy trên JVM thuần trong unit test (cần Robolectric / instrumented test).
 *    Tài liệu hoá behavior qua [stripHtmlContract] thay thế.
 *  - [extractFirstImg] dùng Regex thuần → chạy được trên JVM.
 *  - [sha256] dùng MessageDigest thuần → chạy được trên JVM.
 */
class HtmlUtilsTest {

    // === stripHtml: behavior contract (chỉ tài liệu hoá, không gọi trực tiếp) ===

    /**
     * Hàm này mirror lại logic chính của `stripHtml` (không gọi `android.text.Html`).
     * Dùng để verify các đảm bảo pure-JVM mà implementation phải giữ:
     *  - Trim whitespace đầu/cuối
     *  - Collapse nhiều space/newline thành 1 space
     *  - Replace \u00A0 (nbsp) thành space
     *  - Truncate tại maxChars-1 rồi nối "…"
     */
    @Test
    fun `stripHtml contract - empty input returns empty`() {
        assertEquals("", stripHtmlContract(""))
    }

    @Test
    fun `stripHtml contract - trims leading and trailing whitespace`() {
        assertEquals("content", stripHtmlContract("   content   "))
    }

    @Test
    fun `stripHtml contract - collapses multiple whitespace to single space`() {
        assertEquals("a b c", stripHtmlContract("a   b\n\nc"))
    }

    @Test
    fun `stripHtml contract - replaces nbsp with regular space`() {
        assertEquals("a b", stripHtmlContract("a\u00A0b"))
    }

    @Test
    fun `stripHtml contract - truncates at maxChars with ellipsis`() {
        val input = "a".repeat(300)
        val result = stripHtmlContract(input, maxChars = 100)
        assertTrue("Phải kết thúc bằng dấu …", result.endsWith("…"))
        assertEquals(100, result.length)
    }

    @Test
    fun `stripHtml contract - text shorter than maxChars no ellipsis`() {
        assertEquals("short", stripHtmlContract("short", maxChars = 100))
    }

    @Test
    fun `stripHtml contract - exactly maxChars no ellipsis`() {
        val input = "a".repeat(240)
        val result = stripHtmlContract(input, maxChars = 240)
        assertEquals(240, result.length)
        assertEquals(input, result)
    }

    @Test
    fun `stripHtml contract - truncation replaces tail with ellipsis when text exceeds maxChars`() {
        // 200 'a' chars > 100 → take(99) = "a"*99 → + "…" → length 100
        val input = "a".repeat(200)
        val result = stripHtmlContract(input, maxChars = 100)
        assertTrue(result.endsWith("…"))
        assertEquals(100, result.length)
        // 99 chars 'a' + 1 char '…'
        assertEquals("a".repeat(99) + "…", result)
    }

    @Test
    fun `stripHtml contract - whitespace at truncation point gets collapsed then trimmed before ellipsis`() {
        // Sau khi collapse \s+ → "a"*99 + " " (1 space), .trim() bỏ space → length 99 → không truncate
        // (minh chứng rằng 2 spaces liên tiếp cuối chuỗi bị clean sạch trước khi check độ dài.)
        val input = "a".repeat(99) + "  "
        val result = stripHtmlContract(input, maxChars = 100)
        assertEquals("a".repeat(99), result)
    }

    /**
     * Mirror logic post-`Html.fromHtml` (text đã được HTML strip trước).
     * Implementation gốc thêm `Html.fromHtml(...)` ở trước bước này
     * — bước đó cần Android runtime, không cover được ở đây.
     */
    private fun stripHtmlContract(text: String, maxChars: Int = 240): String {
        val cleaned = text
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (cleaned.length > maxChars) cleaned.take(maxChars - 1).trimEnd() + "…" else cleaned
    }

    // === extractFirstImg ===

    @Test
    fun `extractFirstImg null returns null`() {
        assertNull(extractFirstImg(null))
    }

    @Test
    fun `extractFirstImg empty returns null`() {
        assertNull(extractFirstImg(""))
    }

    @Test
    fun `extractFirstImg no img tag returns null`() {
        assertNull(extractFirstImg("<p>just text</p>"))
    }

    @Test
    fun `extractFirstImg with double-quoted src`() {
        assertEquals("https://x.com/a.jpg", extractFirstImg("""<img src="https://x.com/a.jpg" alt="x"/>"""))
    }

    @Test
    fun `extractFirstImg with single-quoted src`() {
        assertEquals("https://x.com/b.jpg", extractFirstImg("""<img src='https://x.com/b.jpg' alt="x"/>"""))
    }

    @Test
    fun `extractFirstImg with extra attributes before src`() {
        val html = """<img class="thumb" data-id="42" src="https://cdn.example.com/img.png" alt="x"/>"""
        assertEquals("https://cdn.example.com/img.png", extractFirstImg(html))
    }

    @Test
    fun `extractFirstImg is case insensitive on tag name`() {
        assertEquals("a.jpg", extractFirstImg("""<IMG SRC="a.jpg"/>"""))
    }

    @Test
    fun `extractFirstImg returns first img when multiple present`() {
        val html = """first<img src="1.jpg"/>middle<img src="2.jpg"/>end"""
        assertEquals("1.jpg", extractFirstImg(html))
    }

    // === sha256 ===

    @Test
    fun `sha256 output is 64 hex characters`() {
        val hash = sha256("hello")
        assertEquals(64, hash.length)
        assertTrue("Phải là hex lowercase", hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `sha256 empty string has known constant hash`() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256("")
        )
    }

    @Test
    fun `sha256 of abc matches known value`() {
        // SHA-256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256("abc")
        )
    }

    @Test
    fun `sha256 is deterministic for same input`() {
        val a = sha256("canlua-rice")
        val b = sha256("canlua-rice")
        assertEquals(a, b)
    }

    @Test
    fun `sha256 differs for different input - one byte change fully changes hash`() {
        val a = sha256("https://example.com/article/1")
        val b = sha256("https://example.com/article/2")
        assertNotEquals(a, b)
    }

    @Test
    fun `sha256 handles unicode input consistently`() {
        val a = sha256("Cân lúa Đồng bằng sông Cửu Long")
        val b = sha256("Cân lúa Đồng bằng sông Cửu Long")
        assertEquals(a, b)
        assertEquals(64, a.length)
    }

    @Test
    fun `sha256 url with diacritics differs from url-encode version`() {
        // Đảm bảo: URL có dấu và URL đã URL-encode cho hash KHÁC nhau
        // (caller dùng cho dedupe — phải normalize URL trước khi hash).
        val raw = sha256("https://x.com/bài-viết-1")
        val encoded = sha256("https://x.com/b%C3%A0i-vi%E1%BA%BFt-1")
        assertNotEquals(raw, encoded)
    }
}

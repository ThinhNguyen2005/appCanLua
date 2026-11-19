package com.giathinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests cho các helper thuần JVM trong `HtmlUtils.kt` ([extractFirstImg], [sha256]).
 *
 * Lưu ý: [stripHtml] phụ thuộc `android.text.Html.fromHtml` nên thuộc phạm vi instrumented test,
 * không giữ hàm giả lập/mirror trên JVM unit test.
 */
class HtmlUtilsTest {

    // === extractFirstImg ===

    @Test
    fun `extractFirstImg handles html variations cleanly`() {
        data class Case(val html: String?, val expected: String?)
        listOf(
            Case(null, null),
            Case("", null),
            Case("<p>just text</p>", null),
            Case("""<img src="https://x.com/a.jpg" alt="x"/>""", "https://x.com/a.jpg"),
            Case("""<img src='https://x.com/b.jpg' alt="x"/>""", "https://x.com/b.jpg"),
            Case("""<img class="thumb" data-id="42" src="https://cdn.example.com/img.png" alt="x"/>""", "https://cdn.example.com/img.png"),
            Case("""<IMG SRC="a.jpg"/>""", "a.jpg"),
            Case("""first<img src="1.jpg"/>middle<img src="2.jpg"/>end""", "1.jpg")
        ).forEach { (html, expected) ->
            assertEquals("html=$html", expected, extractFirstImg(html))
        }
    }

    // === sha256 ===

    @Test
    fun `sha256 matches known test vectors and formatting`() {
        listOf(
            "" to "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            "abc" to "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        ).forEach { (input, expected) ->
            val hash = sha256(input)
            assertEquals(expected, hash)
            assertEquals(64, hash.length)
            assertTrue("Phải là hex lowercase", hash.all { it in '0'..'9' || it in 'a'..'f' })
        }
    }

    @Test
    fun `sha256 is deterministic and sensitive to single byte or encoding changes`() {
        val unicodeText = "Cân lúa Đồng bằng sông Cửu Long"
        val unicodeHash = sha256(unicodeText)
        assertEquals(64, unicodeHash.length)
        assertEquals(unicodeHash, sha256(unicodeText))

        assertNotEquals(
            sha256("https://example.com/article/1"),
            sha256("https://example.com/article/2")
        )
        assertNotEquals(
            sha256("https://x.com/bài-viết-1"),
            sha256("https://x.com/b%C3%A0i-vi%E1%BA%BFt-1")
        )
    }
}

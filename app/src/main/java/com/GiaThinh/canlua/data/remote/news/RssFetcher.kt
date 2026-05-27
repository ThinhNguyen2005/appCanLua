package com.GiaThinh.canlua.data.remote.news

import android.util.Xml
import com.GiaThinh.canlua.util.extractFirstImg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetcher RSS XML qua OkHttp + XmlPullParser (Android có sẵn, không cần dep mới).
 *
 * Hỗ trợ:
 *  - RSS 2.0 chuẩn `<item>` với `<title>`, `<link>`, `<description>`, `<pubDate>`
 *  - `media:thumbnail`, `media:content`, `enclosure` cho ảnh
 *  - Date format RFC-822 phổ biến của các báo VN và Google News
 *
 * Bỏ qua bài lỗi parse — không throw để 1 item hỏng không phá toàn bộ feed.
 */
@Singleton
class RssFetcher @Inject constructor(
    private val client: OkHttpClient
) {

    private val userAgent =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 CanLua/1.0"

    suspend fun fetch(url: String): List<RssItem> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .addHeader("User-Agent", userAgent)
            .addHeader("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            resp.body?.byteStream()?.use(::parse).orEmpty()
        }
    }

    /** Parse stream RSS → list items. Lỗi parse 1 item → bỏ qua, không throw. */
    private fun parse(input: InputStream): List<RssItem> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(input, null)
        }
        val items = mutableListOf<RssItem>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                runCatching { parseItem(parser) }.getOrNull()?.let(items::add)
            }
            event = parser.next()
        }
        return items
    }

    private fun parseItem(parser: XmlPullParser): RssItem? {
        var title = ""
        var link = ""
        var description = ""
        var pubDate = ""
        var mediaThumb: String? = null

        while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == "item")) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> title = readTextSafe(parser, "title").trim()
                    "link" -> link = readTextSafe(parser, "link").trim()
                    "description" -> description = readTextSafe(parser, "description")
                    "pubDate" -> pubDate = readTextSafe(parser, "pubDate").trim()
                    "thumbnail", "content" -> {
                        // media:thumbnail url="..." hoặc media:content url="..."
                        val ns = parser.namespace
                        if (ns?.contains("media", ignoreCase = true) == true) {
                            val url = parser.getAttributeValue(null, "url")
                            if (!url.isNullOrBlank() && mediaThumb == null) mediaThumb = url
                        }
                    }
                    "enclosure" -> {
                        val type = parser.getAttributeValue(null, "type").orEmpty()
                        if (type.startsWith("image", ignoreCase = true) && mediaThumb == null) {
                            mediaThumb = parser.getAttributeValue(null, "url")
                        }
                    }
                }
            }
            parser.next()
        }
        if (title.isBlank() || link.isBlank()) return null
        return RssItem(
            title = title,
            link = link,
            description = description,
            pubDateMs = parseRssDate(pubDate),
            // ensureHttps: ép cleartext http:// → https:// để Coil load được trên Android
            // (manifest mặc định cấm cleartext, nhiều RSS source vẫn trả http://)
            thumbnail = ensureHttps(mediaThumb ?: extractFirstImg(description))
        )
    }

    /**
     * Ép URL về https:// để Coil không bị NetworkSecurityPolicy chặn cleartext.
     * Hỗ trợ cả URL bắt đầu bằng `//` (protocol-relative) trả về dạng https://.
     */
    private fun ensureHttps(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.substring(7)
            trimmed.startsWith("//") -> "https:" + trimmed
            else -> trimmed
        }
    }

    /**
     * Đọc text + CDATA bên trong tag hiện tại một cách an toàn, bỏ qua mọi thẻ con
     * lồng nhau (HTML inline trong description, link tracker bọc trong link, v.v.).
     *
     * Khác với `parser.nextText()` — vốn ném XmlPullParserException ngay khi gặp
     * START_TAG con — hàm này chỉ thoát khi gặp END_TAG đúng `tagName`. Một item
     * RSS với description chứa `<a><img/></a>` sẽ không còn bị runCatching nuốt
     * âm thầm thành null.
     */
    private fun readTextSafe(parser: XmlPullParser, tagName: String): String {
        val sb = StringBuilder()
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == tagName)) {
            when (event) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(parser.text)
                XmlPullParser.END_DOCUMENT -> return sb.toString()
            }
            event = parser.next()
        }
        return sb.toString()
    }

    /** RFC-822 phổ biến: "Tue, 19 May 2026 09:30:00 GMT" / "+0700". */
    private fun parseRssDate(raw: String): Long {
        if (raw.isBlank()) return System.currentTimeMillis()
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss Z"
        )
        for (p in patterns) {
            runCatching {
                val sdf = SimpleDateFormat(p, Locale.ENGLISH).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                return sdf.parse(raw)?.time ?: 0L
            }
        }
        return System.currentTimeMillis()
    }
}

package com.giathinh.canlua.data.remote.news

import android.text.Html
import android.util.Xml
import com.giathinh.canlua.util.extractFirstImg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetcher RSS XML qua OkHttp + XmlPullParser (Android có sẵn, không cần dep mới).
 *
 * Hỗ trợ:
 *  - RSS 2.0 chuẩn `<item>` với `<title>`, `<link>`, `<description>`, `<pubDate>`
 *  - Atom `<entry>` với `<link href="...">` (Google News format)
 *  - `media:thumbnail`, `media:content`, `enclosure` cho ảnh
 *  - `content:encoded` (WordPress/custom) và `summary` (Atom)
 *  - Redirect following cho Google News articles URLs
 *  - Date format RFC-822 phổ biến của các báo VN và Google News
 *
 * Bỏ qua bài lỗi parse — không throw để 1 item hỏng không phá toàn bộ feed.
 */
@Singleton
class RssFetcher @Inject constructor(
    private val client: OkHttpClient
) {

    suspend fun fetch(url: String): List<RssItem> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    android.util.Log.w("RssFetcher", "HTTP error fetching RSS from $url: ${resp.code}")
                    return@withContext emptyList()
                }
                resp.body?.byteStream()?.use { parse(it, url) }.orEmpty()
            }
        } catch (e: org.xmlpull.v1.XmlPullParserException) {
            android.util.Log.w("RssFetcher", "XML parsing error fetching RSS from $url: ${e.message}")
            emptyList()
        } catch (e: java.net.ProtocolException) {
            android.util.Log.e("RssFetcher", "Protocol exception fetching RSS from $url: ${e.message}")
            emptyList()
        } catch (e: java.io.IOException) {
            android.util.Log.e("RssFetcher", "Network/IO exception fetching RSS from $url: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            android.util.Log.e("RssFetcher", "Unexpected error fetching RSS from $url: ${e.message}")
            emptyList()
        }
    }

    /**
     * Parse stream RSS → list items. Lỗi parse 1 item → bỏ qua, không throw.
     * @param feedUrl để detect Google News feeds (cần redirect following cho thumbnails)
     */
    private fun parse(input: InputStream, feedUrl: String): List<RssItem> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(input, null)
        }
        val items = mutableListOf<RssItem>()
        val isGoogleNews = feedUrl.contains("news.google.com", ignoreCase = true)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && (parser.name == "item" || parser.name == "entry")) {
                runCatching { parseItem(parser, parser.name, isGoogleNews) }.getOrNull()?.let(items::add)
            }
            event = parser.next()
        }
        return items
    }

    /**
     * Parse 1 `<item>` (RSS 2.0) hoặc `<entry>` (Atom) thành RssItem.
     *
     * Các nguồn RSS khác nhau có cấu trúc khác nhau:
     *  - Báo VN (VnExpress, Tuổi Trẻ…): description + img trong description, ít khi có media tags
     *  - Google News: description ngắn (snippet), link trỏ news.google.com → cần redirect để lấy ảnh
     */
    private fun parseItem(parser: XmlPullParser, tagName: String, isGoogleNews: Boolean): RssItem? {
        var title = ""
        var link = ""
        var description = ""
        var pubDate = ""
        var mediaThumb: String? = null
        var resolvedUrl: String? = null // lưu URL sau redirect

        while (!(parser.eventType == XmlPullParser.END_TAG && parser.name == tagName)) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> title = readTextSafe(parser, "title").trim()
                    "link" -> {
                        // RSS 2.0: <link>URL</link>
                        link = readTextSafe(parser, "link").trim()
                    }
                    "description" -> description = readTextSafe(parser, "description")
                    "summary" -> {
                        // Atom summary (thường ngắn hơn content)
                        if (description.isEmpty()) description = readTextSafe(parser, "summary")
                    }
                    "content" -> {
                        // Atom content — thường dài hơn summary
                        if (description.isEmpty()) description = readTextSafe(parser, "content")
                    }
                    "content:encoded" -> {
                        // WordPress/custom blogs — HTML đầy đủ
                        if (description.isEmpty()) description = readTextSafe(parser, "content:encoded")
                    }
                    "pubDate", "published", "updated" -> {
                        if (pubDate.isEmpty()) pubDate = readTextSafe(parser, parser.name).trim()
                    }
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
                // Atom: <link href="..." rel="alternate"/>
                if (parser.name == "link" && parser.namespace?.contains("atom", ignoreCase = true) == true) {
                    val href = parser.getAttributeValue(null, "href")
                    val rel = parser.getAttributeValue(null, "rel")
                    if (!href.isNullOrBlank() && (rel == null || rel == "alternate")) {
                        link = href.trim()
                    }
                }
            }
            parser.next()
        }
        if (title.isBlank() || link.isBlank()) return null

        // Extract ảnh từ description HTML (báo VN thường embed ảnh trong description)
        val descThumb = extractFirstImg(description)
        // Ưu tiên mediaThumb từ tag > descThumb
        val thumbnail = ensureHttps(mediaThumb ?: descThumb)

        return RssItem(
            title = title,
            link = link,
            description = description,
            pubDateMs = parseRssDate(pubDate),
            thumbnail = thumbnail,
            enrichedLink = null
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
            "dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
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

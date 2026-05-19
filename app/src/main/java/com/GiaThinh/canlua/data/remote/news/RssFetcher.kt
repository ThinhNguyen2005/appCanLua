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
class RssFetcher @Inject constructor() {

    // OkHttp riêng — User-Agent browser-like để tránh 403 từ vài site có anti-bot.
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

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
                    "title" -> title = parser.nextText().orEmpty().trim()
                    "link" -> link = parser.nextText().orEmpty().trim()
                    "description" -> description = parser.nextText().orEmpty()
                    "pubDate" -> pubDate = parser.nextText().orEmpty().trim()
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
            thumbnail = mediaThumb ?: extractFirstImg(description)
        )
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

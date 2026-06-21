package com.giathinh.canlua.repository

import com.giathinh.canlua.data.dao.NewsArticleDao
import com.giathinh.canlua.data.model.NewsArticle
import com.giathinh.canlua.data.model.NewsTopic
import com.giathinh.canlua.data.remote.news.NewsSource
import com.giathinh.canlua.data.remote.news.RssFetcher
import com.giathinh.canlua.data.remote.news.RssItem
import com.giathinh.canlua.util.stripHtml
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository bài báo nông nghiệp — offline-first dùng Room làm cache.
 *
 * Kiến trúc mới (GAS-powered với Local RSS Fallback):
 *   - Ưu tiên kéo dữ liệu sạch được crawl từ GAS lưu trên Firestore.
 *   - Nếu Firestore trống hoặc gặp lỗi mạng, app tự động fallback dùng RssFetcher
 *     để cào trực tiếp từ 11 nguồn báo VN.
 *   - Cả GAS và App đều băm link bằng MD5 để làm ID bài viết, tránh trùng lặp dữ liệu trong Room.
 *   - Google News sources: resolve redirect + enrich og:image từ trang gốc.
 */
@Singleton
class NewsRepository @Inject constructor(
    private val dao: NewsArticleDao,
    private val firestore: FirebaseFirestore,
    private val rssFetcher: RssFetcher,
    private val httpClient: OkHttpClient // for enrichment requests
) {
    private val newsCollection get() = firestore.collection("news_articles")

    /** Stream bài theo topic từ Room (offline-first); null = tất cả. */
    fun observe(topic: NewsTopic? = null, limit: Int = 30): Flow<List<NewsArticle>> =
        dao.observeByTopic(topic?.name, limit)

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Fetch bài mới nhất từ Firestore về Room.
     * Nếu không có dữ liệu trên Firestore hoặc lỗi mạng, tự động cào tin từ RSS cục bộ.
     *
     * @return số bài unique đã upsert vào Room
     */
    suspend fun refresh(forceLocalScrape: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val cutoffMs = now - TimeUnit.DAYS.toMillis(14)
            var fetchedCount = 0

            // 1. Thử kéo dữ liệu từ Firestore trước
            val firestoreResult = runCatching {
                val snapshot = newsCollection
                    .whereGreaterThan("publishedAt", cutoffMs)
                    .orderBy("publishedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(60)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        NewsArticle(
                            id          = doc.getString("id") ?: doc.id,
                            title       = doc.getString("title") ?: return@mapNotNull null,
                            description = doc.getString("description") ?: "",
                            link        = doc.getString("link") ?: return@mapNotNull null,
                            source      = doc.getString("source") ?: "",
                            thumbnail   = doc.getString("thumbnail"),
                            publishedAt = doc.getLong("publishedAt") ?: now,
                            topic       = doc.getString("topic") ?: NewsTopic.MARKET.name,
                            cachedAt    = doc.getLong("cachedAt") ?: now
                        )
                    }.getOrNull()
                }
            }

            val cloudArticles = firestoreResult.getOrNull().orEmpty()
            val shouldScrapeLocal = forceLocalScrape || cloudArticles.isEmpty()

            val allArticles = mutableListOf<NewsArticle>()
            allArticles.addAll(cloudArticles)

            if (shouldScrapeLocal) {
                // 2. Fallback hoặc chủ động cào tin từ RSS các trang báo
                val localArticles = mutableListOf<NewsArticle>()
                val jobs = NewsSource.entries.map { src ->
                    async {
                        runCatching {
                            val items = rssFetcher.fetch(src.rssUrl)
                            items.map { item ->
                                mapToArticle(item, src, now)
                            }
                        }.getOrNull().orEmpty()
                    }
                }

                val results = jobs.awaitAll()
                results.forEach { localArticles.addAll(it) }

                // Enrich descriptions + thumbnails cho Google News items
                val googleNewsItems = localArticles.filter { isGoogleNewsSource(it.source) }
                val enrichedArticles = if (googleNewsItems.isNotEmpty()) {
                    enrichGoogleNewsItems(googleNewsItems, now)
                } else emptyList()

                // Merge: giữ original, thay thế bằng enriched nếu có cải thiện
                val enrichedMap = enrichedArticles.associateBy { it.id }
                val merged = localArticles.map { article ->
                    enrichedMap[article.id] ?: article
                }

                val filteredLocal = merged
                    .filter { it.publishedAt >= cutoffMs }
                    .sortedByDescending { it.publishedAt }
                    .take(60)

                allArticles.addAll(filteredLocal)
            }

            if (allArticles.isNotEmpty()) {
                dao.upsertAll(allArticles)
                dao.deleteOlderThan(cutoffMs)
                fetchedCount = allArticles.distinctBy { it.id }.size
            }

            fetchedCount
        }
    }

    /**
     * Map 1 RssItem → NewsArticle.
     * Dùng enrichedLink (actual article URL sau redirect) làm ID nếu có,
     * để match với GAS crawler (GAS resolve redirect trước khi tạo ID).
     */
    private fun cleanDescription(desc: String, title: String): String {
        val clean = desc
            .replace("Comprehensive up-to-date news coverage, aggregated from sources all over the world by Google News.", "", ignoreCase = true)
            .replace("Comprehensive up-to-date news coverage, aggregated from sources all over the world by Google News", "", ignoreCase = true)
            .trim()
        return if (clean.equals(title, ignoreCase = true)) "" else clean
    }

    private fun mapToArticle(item: RssItem, src: NewsSource, now: Long): NewsArticle {
        val articleUrl = item.enrichedLink ?: item.link
        val cleanDesc = cleanDescription(stripHtml(item.description, 300), item.title)
        val finalTopic = NewsSource.classify(item.title + " " + cleanDesc, src.defaultTopic)
        return NewsArticle(
            id          = articleUrl.md5(),
            title       = item.title,
            description = cleanDesc,
            link        = articleUrl,
            source      = src.displayName,
            thumbnail   = item.thumbnail,
            publishedAt = item.pubDateMs,
            topic       = finalTopic.name,
            cachedAt    = now
        )
    }

    /** Detect Google News sources — cần enrich từ trang gốc. */
    private fun isGoogleNewsSource(sourceName: String): Boolean {
        return sourceName.contains("Lúa Gạo", ignoreCase = true) ||
               sourceName.contains("ĐBSCL", ignoreCase = true) ||
               sourceName.contains("Thời Tiết", ignoreCase = true) ||
               sourceName.contains("Google", ignoreCase = true)
    }

    /**
     * Enrich Google News articles: resolve og:image + meta description từ actual article page.
     * Tránh fetch article page trong hot path — chỉ enrich khi:
     *   - description < 30 chars (snippet ngắn)
     *   - thumbnail == null
     */
    private suspend fun enrichGoogleNewsItems(articles: List<NewsArticle>, now: Long): List<NewsArticle> {
        val needsEnrichment = articles.filter {
            it.description.length < 30 || it.thumbnail.isNullOrBlank()
        }
        if (needsEnrichment.isEmpty()) return articles

        return coroutineScope {
            needsEnrichment.map { article ->
                async(Dispatchers.IO) {
                    enrichFromArticlePage(article, now)
                }
            }.awaitAll()
        }
    }

    /** Fetch actual article page → trích xuất og:image + meta description. */
    private fun enrichFromArticlePage(article: NewsArticle, now: Long): NewsArticle {
        var thumbnail = article.thumbnail
        var description = article.description

        val req = Request.Builder()
            .url(article.link)
            .addHeader("Accept", "text/html,application/xhtml+xml,*/*")
            .addHeader("User-Agent", "Mozilla/5.0 (compatible; CanLua/1.0)")
            .build()

        try {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val html = resp.body?.string() ?: return@use

                // og:image
                if (thumbnail.isNullOrBlank()) {
                    thumbnail = extractOgImage(html)
                }

                // Meta description — chỉ khi description hiện tại quá ngắn
                if (description.length < 30) {
                    val metaDesc = extractMetaDescription(html)
                    if (metaDesc.length > 30) {
                        description = cleanDescription(stripHtml(metaDesc, 300), article.title)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("NewsRepo", "Enrich failed for ${article.link}: ${e.message}")
        }

        return article.copy(thumbnail = thumbnail, description = description, cachedAt = now)
    }

    private fun extractOgImage(html: String): String? {
        // <meta property="og:image" content="...">
        val ogMatch = Regex("""<meta[^>]+\bproperty=["']og:image["'][^>]+\bcontent=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)
        if (!ogMatch.isNullOrBlank()) return ogMatch

        // Hoặc ngược lại: content trước property
        val ogMatch2 = Regex("""<meta[^>]+\bcontent=["']([^"']+)["'][^>]+\bproperty=["']og:image["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)
        if (!ogMatch2.isNullOrBlank()) return ogMatch2

        // twitter:image
        val twitterMatch = Regex("""<meta[^>]+\bname=["']twitter:image["'][^>]+\bcontent=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)
        if (!twitterMatch.isNullOrBlank()) return twitterMatch

        // First meaningful <img> src
        val imgMatches = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(html)
        for (match in imgMatches) {
            val src = match.groupValues[1]
            if (src.length > 50 && !src.contains("logo", ignoreCase = true) &&
                !src.contains("banner", ignoreCase = true) &&
                !src.contains("ads", ignoreCase = true) &&
                !src.contains("tracking", ignoreCase = true)) {
                return src
            }
        }
        return null
    }

    private fun extractMetaDescription(html: String): String {
        // <meta name="description" content="...">
        val descMatch = Regex("""<meta[^>]+\bname=["']description["'][^>]+\bcontent=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)
        if (!descMatch.isNullOrBlank()) return descMatch

        // og:description
        val ogMatch = Regex("""<meta[^>]+\bproperty=["']og:description["'][^>]+\bcontent=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)
        return ogMatch ?: ""
    }

    /** Cache cũ hơn [maxAgeMs] hoặc rỗng → cần refresh. */
    suspend fun isStale(maxAgeMs: Long = TimeUnit.HOURS.toMillis(1)): Boolean =
        withContext(Dispatchers.IO) {
            val newest = dao.getNewestCachedAt() ?: return@withContext true
            System.currentTimeMillis() - newest > maxAgeMs
        }

    suspend fun isEmpty(): Boolean = withContext(Dispatchers.IO) {
        dao.count() == 0
    }
}

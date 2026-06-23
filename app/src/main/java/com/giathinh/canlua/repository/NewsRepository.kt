package com.giathinh.canlua.repository

import com.giathinh.canlua.data.dao.NewsArticleDao
import com.giathinh.canlua.data.model.NewsArticle
import com.giathinh.canlua.data.model.NewsTopic
import com.giathinh.canlua.data.remote.news.NewsSource
import com.giathinh.canlua.data.remote.news.RssFetcher
import com.giathinh.canlua.data.remote.news.RssItem
import com.giathinh.canlua.util.stripHtml
import com.giathinh.canlua.data.remote.SupabaseClient
import com.google.gson.annotations.SerializedName
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
 * Chiến lược: RSS-first.
 *   - Fetch trực tiếp từ 11+ nguồn báo VN qua RssFetcher.
 *   - Kết quả upsert vào Room (cache offline).
 *   - Google News sources: resolve redirect + enrich og:image từ trang gốc.
 *   - Không còn phụ thuộc Firestore cho bài báo.
 */
@Singleton
class NewsRepository @Inject constructor(
    private val dao: NewsArticleDao,
    private val rssFetcher: RssFetcher,
    private val httpClient: OkHttpClient, // for og:image enrichment
    private val supabase: SupabaseClient,
    @Named("supabaseUrl") private val supabaseUrl: String,
    @Named("supabaseAnonKey") private val supabaseAnonKey: String
) {

    /** Stream bài theo topic từ Room (offline-first); null = tất cả. */
    fun observe(topic: NewsTopic? = null, limit: Int = 30): Flow<List<NewsArticle>> =
        dao.observeByTopic(topic?.name, limit)

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Fetch fresh articles from RSS sources → upsert into Room.
     *
     * @return number of unique articles upserted
     */
    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val cutoffMs = now - TimeUnit.DAYS.toMillis(14)

            // 1. Fetch from Supabase first
            val supabaseArticles = fetchFromSupabase().getOrDefault(emptyList())
            if (supabaseArticles.isNotEmpty()) {
                dao.upsertAll(supabaseArticles)
            }

            // 2. Parallel fetch from all RSS sources
            val localArticles = mutableListOf<NewsArticle>()
            val jobs = coroutineScope {
                NewsSource.entries.map { src ->
                    async {
                        runCatching {
                            rssFetcher.fetch(src.rssUrl).map { item -> mapToArticle(item, src, now) }
                        }.getOrNull().orEmpty()
                    }
                }
            }
            jobs.awaitAll().forEach { localArticles.addAll(it) }

            // Filter by time limit, sort by date, and take top 60 first.
            val candidateArticles = localArticles
                .filter { it.publishedAt >= cutoffMs }
                .sortedByDescending { it.publishedAt }
                .take(60)

            // Split into Google News candidates vs local candidates
            val googleNewsCandidates = candidateArticles.filter { isGoogleNewsSource(it.source) }
            val localCandidates = candidateArticles.filter { !isGoogleNewsSource(it.source) }

            // Resolve and enrich Google News items concurrently with a Semaphore limit of 5
            val semaphore = Semaphore(5)
            val resolvedGoogleNews = coroutineScope {
                googleNewsCandidates.map { article ->
                    async {
                        semaphore.withPermit {
                            resolveAndEnrichGoogleNews(article, now)
                        }
                    }
                }.awaitAll()
            }

            // Merge back, sort again, distinct by ID, and take top 60
            val articles = (localCandidates + resolvedGoogleNews)
                .sortedByDescending { it.publishedAt }
                .distinctBy { it.id }
                .take(60)

            if (articles.isNotEmpty()) {
                dao.upsertAll(articles)
                dao.deleteOlderThan(cutoffMs)
            }

            articles.size + supabaseArticles.size
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

    /** Tracing redirect URL của Google News RSS items dùng HEAD requests. */
    private fun resolveRedirect(url: String): String {
        if (!url.contains("news.google.com", ignoreCase = true)) return url
        
        var currentUrl = url
        val redirectLessClient = httpClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
            
        var redirectCount = 0
        val maxRedirects = 3
        
        while (redirectCount < maxRedirects) {
            val req = Request.Builder()
                .url(currentUrl)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
                
            try {
                redirectLessClient.newCall(req).execute().use { resp ->
                    if (resp.code in 300..399) {
                        val location = resp.header("Location")
                        if (!location.isNullOrBlank()) {
                            val nextUrl = if (location.startsWith("/")) {
                                val uri = java.net.URI(currentUrl)
                                "${uri.scheme}://${uri.host}$location"
                            } else {
                                location
                            }
                            currentUrl = nextUrl
                            redirectCount++
                            continue
                        }
                    } else if (resp.code == 405) {
                        // Fallback sang GET nếu HEAD bị chặn/không hỗ trợ
                        val getReq = Request.Builder()
                            .url(currentUrl)
                            .get()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .build()
                        redirectLessClient.newCall(getReq).execute().use { getResp ->
                            if (getResp.code in 300..399) {
                                val location = getResp.header("Location")
                                if (!location.isNullOrBlank()) {
                                    val nextUrl = if (location.startsWith("/")) {
                                        val uri = java.net.URI(currentUrl)
                                        "${uri.scheme}://${uri.host}$location"
                                    } else {
                                        location
                                    }
                                    currentUrl = nextUrl
                                    redirectCount++
                                    return@use
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("NewsRepo", "Redirect resolve failed for $currentUrl: ${e.message}")
                break
            }
            break
        }
        return currentUrl
    }

    private suspend fun resolveAndEnrichGoogleNews(article: NewsArticle, now: Long): NewsArticle = withContext(Dispatchers.IO) {
        val resolvedUrl = resolveRedirect(article.link)
        
        var updatedArticle = if (resolvedUrl != article.link) {
            article.copy(
                id = resolvedUrl.md5(),
                link = resolvedUrl
            )
        } else {
            article
        }
        
        if (updatedArticle.description.length < 30 || updatedArticle.thumbnail.isNullOrBlank()) {
            updatedArticle = enrichFromArticlePage(updatedArticle, now)
        }
        
        updatedArticle
    }

    /** Fetch actual article page → trích xuất og:image + meta description. */
    private fun enrichFromArticlePage(article: NewsArticle, now: Long): NewsArticle {
        var thumbnail = article.thumbnail
        var description = article.description

        val req = Request.Builder()
            .url(article.link)
            .addHeader("Accept", "text/html,application/xhtml+xml,*/*")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
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

    /**
     * Fetch the latest news articles from Supabase.
     */
    suspend fun fetchFromSupabase(): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        runCatching {
            val type = com.google.gson.reflect.TypeToken
                .getParameterized(List::class.java, NewsArticleDto::class.java).type
            val dtos = supabase.getList<NewsArticleDto>(
                baseUrl = supabaseUrl,
                anonKey = supabaseAnonKey,
                table = "news_articles",
                params = mapOf(
                    "order" to "published_at.desc",
                    "limit" to "60"
                ),
                type = type
            )
            android.util.Log.d("NewsRepo", "Fetched ${dtos.size} articles from Supabase")
            dtos.map { it.toNewsArticle() }
        }.onFailure { e ->
            android.util.Log.w("NewsRepo", "Error fetching news from Supabase: ${e.message}")
        }
    }

    /**
     * Fetch from Supabase and cache/upsert to Room.
     */
    suspend fun refreshFromSupabase(): Result<Int> = withContext(Dispatchers.IO) {
        fetchFromSupabase().map { articles ->
            if (articles.isNotEmpty()) {
                dao.upsertAll(articles)
            }
            articles.size
        }
    }
}

/** DTO matching the Supabase `news_articles` table column names (snake_case). */
data class NewsArticleDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String? = "",
    @SerializedName("link") val link: String = "",
    @SerializedName("source") val source: String? = "",
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("published_at") val published_at: Long = 0L,
    @SerializedName("topic") val topic: String = "MARKET",
    @SerializedName("cached_at") val cached_at: Long = 0L
) {
    fun toNewsArticle() = NewsArticle(
        id          = id,
        title       = title,
        description = description.orEmpty(),
        link        = link,
        source      = source.orEmpty(),
        thumbnail   = thumbnail,
        publishedAt = published_at,
        topic       = topic,
        cachedAt    = if (cached_at > 0L) cached_at else System.currentTimeMillis()
    )
}

package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.dao.NewsArticleDao
import com.GiaThinh.canlua.data.model.NewsArticle
import com.GiaThinh.canlua.data.model.NewsTopic
import com.GiaThinh.canlua.data.remote.news.NewsSource
import com.GiaThinh.canlua.data.remote.news.RssFetcher
import com.GiaThinh.canlua.data.remote.news.RssItem
import com.GiaThinh.canlua.util.sha256
import com.GiaThinh.canlua.util.stripHtml
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregator + cache cho NewsFeed.
 *
 * Strategy:
 *  - `observe(topic)` → Flow từ Room (offline-first)
 *  - `refresh()` parallel fetch tất cả [NewsSource], dedupe theo URL hash, upsert
 *  - `isStale(maxAgeMs)` → cho UI quyết định auto-refresh
 *
 * Fail-soft: 1 source lỗi không phá toàn bộ refresh — chỉ source đó trả empty.
 */
@Singleton
class NewsRepository @Inject constructor(
    private val dao: NewsArticleDao,
    private val rssFetcher: RssFetcher
) {

    /** Stream bài theo topic; null = tất cả. */
    fun observe(topic: NewsTopic? = null, limit: Int = 30): Flow<List<NewsArticle>> =
        dao.observeByTopic(topic?.name, limit)

    /**
     * Fetch song song mọi nguồn, dedupe theo hash(link), upsert Room, dọn bài > 14 ngày.
     * @return số bài unique đã cache
     */
    suspend fun refresh(): Result<Int> = runCatching {
        val now = System.currentTimeMillis()
        coroutineScope {
            val perSource = NewsSource.values().map { src ->
                async {
                    runCatching { rssFetcher.fetch(src.rssUrl) }
                        .getOrDefault(emptyList())
                        .mapNotNull { item -> item.toArticle(src, now) }
                }
            }.awaitAll().flatten()

            val unique = perSource
                .distinctBy { it.id }
                // bỏ bài quá cũ (> 30 ngày) ngay từ tầng repo
                .filter { now - it.publishedAt < TimeUnit.DAYS.toMillis(30) }

            if (unique.isNotEmpty()) {
                dao.upsertAll(unique)
                dao.deleteOlderThan(now - TimeUnit.DAYS.toMillis(14))
            }
            unique.size
        }
    }

    /** Cache cũ hơn [maxAgeMs] hoặc rỗng → cần refresh. */
    suspend fun isStale(maxAgeMs: Long = TimeUnit.HOURS.toMillis(1)): Boolean {
        val newest = dao.getNewestCachedAt() ?: return true
        return System.currentTimeMillis() - newest > maxAgeMs
    }

    suspend fun isEmpty(): Boolean = dao.count() == 0

    private fun RssItem.toArticle(src: NewsSource, now: Long): NewsArticle? {
        val link = link.takeIf { it.isNotBlank() } ?: return null
        val cleanDesc = stripHtml(description, maxChars = 240)
        val topic = NewsSource.classify(
            text = "$title $cleanDesc",
            fallback = src.defaultTopic
        )
        return NewsArticle(
            id = sha256(link),
            title = title.ifBlank { return null },
            description = cleanDesc,
            link = link,
            source = src.displayName,
            thumbnail = thumbnail,
            publishedAt = pubDateMs,
            topic = topic.name,
            cachedAt = now
        )
    }
}

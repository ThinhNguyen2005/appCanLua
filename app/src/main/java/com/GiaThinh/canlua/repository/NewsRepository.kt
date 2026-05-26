package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.data.dao.NewsArticleDao
import com.GiaThinh.canlua.data.model.NewsArticle
import com.GiaThinh.canlua.data.model.NewsTopic
import com.GiaThinh.canlua.data.remote.news.NewsSource
import com.GiaThinh.canlua.data.remote.news.RssFetcher
import com.GiaThinh.canlua.util.stripHtml
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
 */
@Singleton
class NewsRepository @Inject constructor(
    private val dao: NewsArticleDao,
    private val firestore: FirebaseFirestore,
    private val rssFetcher: RssFetcher
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
    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
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
            if (cloudArticles.isNotEmpty()) {
                dao.upsertAll(cloudArticles)
                dao.deleteOlderThan(cutoffMs)
                fetchedCount = cloudArticles.size
            } else {
                // 2. Fallback: Cào trực tiếp từ RSS các trang báo nếu Firestore trống/lỗi
                val localArticles = mutableListOf<NewsArticle>()
                val jobs = NewsSource.entries.map { src ->
                    async {
                        runCatching {
                            val items = rssFetcher.fetch(src.rssUrl)
                            items.map { item ->
                                val cleanDesc = stripHtml(item.description, 240)
                                val finalTopic = NewsSource.classify(item.title + " " + cleanDesc, src.defaultTopic)
                                NewsArticle(
                                    id          = item.link.md5(),
                                    title       = item.title,
                                    description = cleanDesc,
                                    link        = item.link,
                                    source      = src.displayName,
                                    thumbnail   = item.thumbnail,
                                    publishedAt = item.pubDateMs,
                                    topic       = finalTopic.name,
                                    cachedAt    = now
                                )
                            }
                        }.getOrNull().orEmpty()
                    }
                }

                jobs.forEach { job ->
                    localArticles.addAll(job.await())
                }

                val filteredLocal = localArticles
                    .filter { it.publishedAt >= cutoffMs }
                    .sortedByDescending { it.publishedAt }
                    .take(60)

                if (filteredLocal.isNotEmpty()) {
                    dao.upsertAll(filteredLocal)
                    dao.deleteOlderThan(cutoffMs)
                    fetchedCount = filteredLocal.size
                }
            }

            fetchedCount
        }
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

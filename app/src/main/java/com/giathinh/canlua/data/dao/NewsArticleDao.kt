package com.giathinh.canlua.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.giathinh.canlua.data.model.NewsArticle
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsArticleDao {

    /** Quan sát bài theo topic, sắp xếp mới nhất trước. topic = null → tất cả. */
    @Query(
        """
        SELECT * FROM news_articles
        WHERE (:topic IS NULL OR topic = :topic)
        ORDER BY publishedAt DESC
        LIMIT :limit
        """
    )
    fun observeByTopic(topic: String?, limit: Int): Flow<List<NewsArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NewsArticle>)

    @Query("DELETE FROM news_articles WHERE cachedAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)

    @Query("SELECT MAX(cachedAt) FROM news_articles")
    suspend fun getNewestCachedAt(): Long?

    @Query("SELECT COUNT(*) FROM news_articles")
    suspend fun count(): Int
}

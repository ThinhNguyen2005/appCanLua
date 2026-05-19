package com.GiaThinh.canlua.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bài báo nông nghiệp được fetch từ RSS feeds.
 * id = SHA-256 hash của link để chống dup khi cùng 1 bài xuất hiện nhiều nguồn.
 */
@Entity(
    tableName = "news_articles",
    indices = [Index(value = ["topic", "publishedAt"], name = "idx_news_topic_published")]
)
data class NewsArticle(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val link: String,
    val source: String,
    val thumbnail: String?,
    val publishedAt: Long,
    val topic: String,
    val cachedAt: Long
)

enum class NewsTopic(val displayName: String) {
    RICE("Lúa"),
    GRAIN("Gạo"),
    WEATHER("Thời tiết"),
    MARKET("Thị trường")
}

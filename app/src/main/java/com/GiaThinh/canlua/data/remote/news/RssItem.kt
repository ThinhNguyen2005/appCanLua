package com.GiaThinh.canlua.data.remote.news

/**
 * Item RSS đã parse — chỉ chứa fields cần dùng để map sang [NewsArticle].
 * Strip HTML / extract image được thực hiện ở repository layer để giữ class này thuần data.
 */
data class RssItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDateMs: Long,
    val thumbnail: String?
)

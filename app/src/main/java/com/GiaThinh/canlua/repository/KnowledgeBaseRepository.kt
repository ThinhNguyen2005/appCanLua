package com.GiaThinh.canlua.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Knowledge Base nội bộ — RAG-lite cho AI Khuyến Nông.
 *
 * Load JSON tĩnh từ `assets/agronomy_knowledge.json` 1 lần (lazy), sau đó
 * search bằng keyword đơn giản (không cần vector DB) khi user gõ câu hỏi.
 *
 * Match strategy: lowercase + bỏ dấu, đếm số keyword trùng → rank theo score.
 * Đủ tốt cho ~10-30 chủ đề. Khi mở rộng > 100 → cân nhắc TF-IDF hoặc embedding.
 */
@Singleton
class KnowledgeBaseRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class KnowledgeEntry(
        val id: String,
        val title: String,
        val keywords: List<String>,
        val content: String
    )

    private data class KbFile(
        @SerializedName("entries") val entries: List<KnowledgeEntry> = emptyList()
    )

    private val entries: List<KnowledgeEntry> by lazy { loadFromAssets() }

    private fun loadFromAssets(): List<KnowledgeEntry> = try {
        val raw = context.assets.open("agronomy_knowledge.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        Gson().fromJson(raw, KbFile::class.java)?.entries.orEmpty()
    } catch (e: Exception) {
        // Asset thiếu hoặc malformed — fallback về list rỗng để AI vẫn chạy được.
        emptyList()
    }

    /**
     * Trả về [maxResults] entry liên quan nhất với câu hỏi user.
     * Dùng làm "tài liệu tham khảo" inject vào system prompt.
     */
    fun search(query: String, maxResults: Int = 2): List<KnowledgeEntry> {
        if (query.isBlank() || entries.isEmpty()) return emptyList()
        val normQuery = normalize(query)
        val tokens = normQuery.split(Regex("\\s+")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return emptyList()

        return entries
            .map { it to scoreEntry(it, normQuery, tokens) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(maxResults)
            .map { it.first }
    }

    private fun scoreEntry(entry: KnowledgeEntry, normQuery: String, tokens: List<String>): Int {
        var score = 0
        // Keyword match — trọng số cao nhất.
        for (kw in entry.keywords) {
            val nKw = normalize(kw)
            if (nKw.isBlank()) continue
            if (normQuery.contains(nKw)) score += 5
            else if (tokens.any { it == nKw || nKw.contains(it) }) score += 2
        }
        // Title match.
        val nTitle = normalize(entry.title)
        if (tokens.any { nTitle.contains(it) }) score += 1
        return score
    }

    /** Bỏ dấu Tiếng Việt + lowercase → tăng độ recall khi user gõ không dấu. */
    private fun normalize(s: String): String {
        val lowered = s.lowercase()
        val noDiacritics = java.text.Normalizer.normalize(lowered, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("đ", "d")
            .replace("Đ", "d")
        return noDiacritics
    }
}

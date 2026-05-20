package com.GiaThinh.canlua.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Knowledge Base nội bộ — RAG-lite cho AI Chat.
 *
 * Tách 2 corpus theo audience:
 *  - [Audience.FARMER] → `assets/agronomy_knowledge.json` (kỹ thuật canh tác).
 *  - [Audience.TRADER] → `assets/trader_knowledge.json` (giá, logistics, đàm phán).
 *
 * Match strategy: lowercase + bỏ dấu, đếm số keyword trùng → rank theo score.
 * Đủ tốt cho ~10-30 chủ đề mỗi corpus. Khi mở rộng > 100 → cân nhắc embedding.
 */
@Singleton
class KnowledgeBaseRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    enum class Audience { FARMER, TRADER }

    data class KnowledgeEntry(
        val id: String,
        val title: String,
        val keywords: List<String>,
        val content: String
    )

    private data class KbFile(
        @SerializedName("entries") val entries: List<KnowledgeEntry> = emptyList()
    )

    private val farmerEntries: List<KnowledgeEntry> by lazy {
        loadFromAssets("agronomy_knowledge.json")
    }
    private val traderEntries: List<KnowledgeEntry> by lazy {
        loadFromAssets("trader_knowledge.json")
    }

    private fun loadFromAssets(filename: String): List<KnowledgeEntry> = try {
        val raw = context.assets.open(filename)
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
    fun search(
        query: String,
        audience: Audience = Audience.FARMER,
        maxResults: Int = 2
    ): List<KnowledgeEntry> {
        val entries = when (audience) {
            Audience.FARMER -> farmerEntries
            Audience.TRADER -> traderEntries
        }
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
        for (kw in entry.keywords) {
            val nKw = normalize(kw)
            if (nKw.isBlank()) continue
            if (normQuery.contains(nKw)) score += 5
            else if (tokens.any { it == nKw || nKw.contains(it) }) score += 2
        }
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

package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.BuildConfig
import com.GiaThinh.canlua.data.model.Profile
import com.GiaThinh.canlua.data.model.RicePrice
import com.GiaThinh.canlua.data.model.WeatherInfo
import com.GiaThinh.canlua.data.remote.HttpClient
import com.GiaThinh.canlua.data.remote.ai.ChatMessage
import com.GiaThinh.canlua.data.remote.ai.OpenRouterRequest
import com.GiaThinh.canlua.data.remote.ai.OpenRouterResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI chat qua OpenRouter (OpenAI-compatible). Dùng model free-tier để tiết kiệm chi phí.
 *
 * RAG-lite chia 4 nguồn context, chèn lần lượt vào system prompt:
 *  1. Hồ sơ user (tên, vai trò FARMER/TRADER, vùng canh tác)  → cá nhân hoá xưng hô.
 *  2. Thời tiết hiện tại (vị trí + nhiệt độ + ẩm + cảnh báo)  → AI khuyến nghị theo điều kiện thực.
 *  3. Bảng giá lúa hôm nay                                    → AI tra cứu khi user hỏi giá.
 *  4. Knowledge base nội bộ (tài liệu khuyến nông tin cậy)    → ngăn AI bịa thông tin về bệnh/lịch bón.
 */
@Singleton
class AiChatRepository @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private const val MODEL = "openrouter/free"
        private const val URL = "https://openrouter.ai/api/v1/chat/completions"

        private const val FARMER_PROMPT_BASE = """
Bạn là Trợ Lý Khuyến Nông cho nông dân trồng lúa tại Đồng bằng sông Cửu Long, Việt Nam.
Luôn trả lời bằng Tiếng Việt, ngắn gọn, ưu tiên tính thực tế và dễ áp dụng.
Khi đưa ra giải pháp kỹ thuật, ghi rõ: 1) Nguyên nhân, 2) Cách xử lý, 3) Phòng ngừa.

QUY TẮC TUYỆT ĐỐI:
- Khi user hỏi về GIÁ LÚA: CHỈ dùng số liệu trong "BẢNG GIÁ LÚA HÔM NAY". Nếu giống không có, nói thẳng "chưa có dữ liệu".
- Khi tư vấn kỹ thuật: ưu tiên trích từ "TÀI LIỆU KHUYẾN NÔNG NỘI BỘ" được cung cấp. Không bịa tên thuốc/liều lượng.
- Tận dụng "THỜI TIẾT HIỆN TẠI" để khuyến nghị (vd: trời mưa → hoãn phun thuốc).
- Cá nhân hoá xưng hô theo "HỒ SƠ NGƯỜI DÙNG" (vd: gọi tên + vai trò Nông dân/Thương lái).
- Nếu không chắc, khuyên hỏi cán bộ khuyến nông địa phương.
"""

        private const val TRADER_PROMPT_BASE = """
Bạn là Chuyên Gia Thị Trường Lúa Gạo cho thương lái thu mua tại Đồng bằng sông Cửu Long, Việt Nam.
Luôn trả lời bằng Tiếng Việt, ngắn gọn, ưu tiên số liệu và góc nhìn kinh doanh.
Khi tư vấn: 1) Phân tích thị trường, 2) Rủi ro cần lưu ý, 3) Hành động đề xuất.

QUY TẮC TUYỆT ĐỐI:
- Khi user hỏi GIÁ LÚA: CHỈ dùng số liệu trong "BẢNG GIÁ LÚA HÔM NAY". Nếu giống không có, nói thẳng "chưa có dữ liệu".
- Khi tư vấn kỹ thuật thu mua/kiểm định/logistics: ưu tiên trích "TÀI LIỆU THỊTRƯỜNG NỘI BỘ". Không bịa số liệu.
- Tận dụng "THỜI TIẾT HIỆN TẠI" để cảnh báo (vd: mưa kéo dài → rủi ro ẩm lúa cao, nên hoãn thu mua hoặc ép sấy).
- Cá nhân hoá xưng hô theo "HỒ SƠ NGƯỜI DÙNG" (vai trò Thương lái).
- Khuyến nghị biên lợi nhuận hợp lý theo thực tế 50-150 đ/kg, không vẽ lợi nhuận phí lý.
- Nếu không chắc, khuyên tham khảo thêm nhà máy xay xuất khẩu hoặc HTX.
"""
    }

    /**
     * Tham số context đầy đủ cho 1 lượt chat. Field nào null/rỗng sẽ bỏ qua khi build prompt.
     *
     * @param history       Lịch sử hội thoại (đã lọc error).
     * @param profile       Hồ sơ user (Room) — cá nhân hoá.
     * @param weather       Thời tiết hiện tại từ WeatherRepository.
     * @param ricePrices    Bảng giá hôm nay từ MarketRepository.
     * @param knowledgeHits Top entries match keyword từ KnowledgeBaseRepository.search(query).
     */
    suspend fun chat(
        history: List<ChatMessage>,
        profile: Profile? = null,
        weather: WeatherInfo? = null,
        ricePrices: List<RicePrice> = emptyList(),
        knowledgeHits: List<KnowledgeBaseRepository.KnowledgeEntry> = emptyList(),
        audience: KnowledgeBaseRepository.Audience = KnowledgeBaseRepository.Audience.FARMER
    ): Result<String> = withContext(Dispatchers.IO) {
        if (BuildConfig.OPENROUTER_API_KEY.isEmpty()) {
            return@withContext Result.failure(IllegalStateException(
                "Thiếu OPENROUTER_API_KEY trong local.properties"
            ))
        }
        try {
            val systemPrompt = buildSystemPrompt(profile, weather, ricePrices, knowledgeHits, audience)
            val req = OpenRouterRequest(
                model = MODEL,
                messages = listOf(ChatMessage("system", systemPrompt)) + history
            )
            val resp: OpenRouterResponse = httpClient.postJson(
                url = URL,
                body = req,
                headers = mapOf(
                    "Authorization" to "Bearer ${BuildConfig.OPENROUTER_API_KEY}",
                    "HTTP-Referer" to "https://canlua.app",
                    "X-Title" to "CanLua"
                )
            )
            resp.error?.message?.let { return@withContext Result.failure(RuntimeException(it)) }
            val answer = resp.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            if (answer.isEmpty()) {
                Result.failure(RuntimeException("AI không trả về nội dung"))
            } else {
                Result.success(answer)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Phân tích mùa vụ — gọi AI với prompt tối ưu cho dashboard insights.
     *
     * Khác `chat()`: không cần history, không cần knowledge base.
     * Input là 1 block summary đã build sẵn từ DashboardViewModel.
     *
     * Mục tiêu output: 4 mục chính
     *  1. Đánh giá tổng quan vụ (tốt/trung bình/cần cải thiện + lý do số liệu)
     *  2. So sánh với vụ trước (delta nào đáng chú ý)
     *  3. Phân bổ giống lúa (giống nào hiệu quả nhất)
     *  4. Khuyến nghị thực tiễn cho vụ tới (≤ 3 ý)
     */
    suspend fun analyzeSeason(
        seasonSummary: String,
        profile: Profile? = null,
        weather: WeatherInfo? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (BuildConfig.OPENROUTER_API_KEY.isEmpty()) {
            return@withContext Result.failure(IllegalStateException(
                "Thiếu OPENROUTER_API_KEY trong local.properties"
            ))
        }
        try {
            val systemPrompt = buildSeasonAnalysisPrompt(profile, weather)
            val userMsg = ChatMessage(role = "user", content = seasonSummary)

            val req = OpenRouterRequest(
                model = MODEL,
                messages = listOf(ChatMessage("system", systemPrompt), userMsg)
            )
            val resp: OpenRouterResponse = httpClient.postJson(
                url = URL,
                body = req,
                headers = mapOf(
                    "Authorization" to "Bearer ${BuildConfig.OPENROUTER_API_KEY}",
                    "HTTP-Referer" to "https://canlua.app",
                    "X-Title" to "CanLua"
                )
            )
            resp.error?.message?.let { return@withContext Result.failure(RuntimeException(it)) }
            val answer = resp.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            if (answer.isEmpty()) Result.failure(RuntimeException("AI không trả về nội dung"))
            else Result.success(answer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSeasonAnalysisPrompt(profile: Profile?, weather: WeatherInfo?): String {
        val parts = mutableListOf(
            """
Bạn là chuyên gia phân tích nông nghiệp lúa nước cho nông dân ĐBSCL Việt Nam.
Nhiệm vụ: phân tích bảng số liệu mùa vụ mà người dùng cung cấp, đưa ra insights ngắn, dễ hiểu.

ĐỊNH DẠNG OUTPUT (Markdown thuần, ngắn gọn, tổng cộng < 350 từ):

## 📊 Tổng quan vụ
[1–2 câu đánh giá: vụ tốt / trung bình / cần cải thiện, dựa trên số liệu cụ thể]

## 📈 So sánh với vụ trước
[Liệt kê 2–3 thay đổi quan trọng nhất. Có % delta. Nếu không có vụ trước thì ghi "Đây là vụ đầu tiên có dữ liệu"]

## 🌾 Giống lúa hiệu quả
[Giống nào chiếm tỉ trọng lớn nhất? Có gợi ý đa dạng hóa hay tập trung?]

## 💡 Khuyến nghị vụ tới
[Tối đa 3 gạch đầu dòng. Thực tiễn, áp dụng được ngay]

QUY TẮC:
- Trả lời 100% Tiếng Việt.
- KHÔNG bịa số liệu — chỉ dùng số có trong input.
- Khi delta là +/-, gọi đúng "tăng" / "giảm".
- Nếu input thiếu data (vd: tổng số phiếu = 0), nói rõ "chưa đủ dữ liệu để phân tích".
- Tránh thuật ngữ kỹ thuật phức tạp.
""".trim()
        )
        profile?.let { parts += buildProfileBlock(it) }
        weather?.let { parts += buildWeatherBlock(it) }
        return parts.joinToString("\n\n")
    }

    private fun buildSystemPrompt(
        profile: Profile?,
        weather: WeatherInfo?,
        prices: List<RicePrice>,
        kbHits: List<KnowledgeBaseRepository.KnowledgeEntry>,
        audience: KnowledgeBaseRepository.Audience
    ): String {
        val base = when (audience) {
            KnowledgeBaseRepository.Audience.TRADER -> TRADER_PROMPT_BASE.trim()
            KnowledgeBaseRepository.Audience.FARMER -> FARMER_PROMPT_BASE.trim()
        }
        val parts = mutableListOf(base)
        parts += buildContextHeader()
        profile?.let { parts += buildProfileBlock(it) }
        weather?.let { parts += buildWeatherBlock(it) }
        if (prices.isNotEmpty()) parts += buildPriceContextBlock(prices)
        if (kbHits.isNotEmpty()) parts += buildKnowledgeBlock(kbHits, audience)
        return parts.joinToString("\n\n")
    }

    private fun buildContextHeader(): String {
        val now = SimpleDateFormat("EEEE, dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi"))
            .format(Date())
        return "🕐 BỐI CẢNH CUỘC HỘI THOẠI:\nThời điểm: $now"
    }

    private fun buildProfileBlock(p: Profile): String {
        val role = when (p.role.uppercase()) {
            "TRADER" -> "Thương lái"
            else -> "Nông dân"
        }
        val sb = StringBuilder("👤 HỒ SƠ NGƯỜI DÙNG:\n")
        sb.append("- Tên: ${p.name.ifBlank { "Chưa cung cấp" }}\n")
        sb.append("- Vai trò: $role\n")
        if (p.region.isNotBlank()) sb.append("- Vùng canh tác: ${p.region}\n")
        if (p.note.isNotBlank()) sb.append("- Ghi chú: ${p.note}\n")
        return sb.toString().trimEnd()
    }

    private fun buildWeatherBlock(w: WeatherInfo): String {
        val sb = StringBuilder("🌦️ THỜI TIẾT HIỆN TẠI (theo GPS):\n")
        sb.append("- Vị trí: ${w.location}\n")
        sb.append("- ${w.condition} · ${w.temperature}°C (cảm giác ${w.feelsLike}°C)\n")
        sb.append("- Độ ẩm ${w.humidity}% · gió ${"%.1f".format(w.windSpeed)} m/s · mây ${w.rainChance}%\n")
        if (!w.advisory.isNullOrBlank()) sb.append("- Cảnh báo: ${w.advisory}\n")
        return sb.toString().trimEnd()
    }

    private fun buildPriceContextBlock(prices: List<RicePrice>): String {
        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi"))
        val now = System.currentTimeMillis()
        val sb = StringBuilder("📊 BẢNG GIÁ LÚA HÔM NAY (đơn vị: VNĐ/kg lúa tươi tại ruộng):\n")
        prices
            .sortedByDescending { it.priceAvg7d }
            .take(8)
            .forEach { p ->
                val trendIcon = when (p.trend) {
                    "UP" -> "↑"
                    "DOWN" -> "↓"
                    else -> "→"
                }
                val freshness = formatFreshness(now - p.updatedAt)
                sb.append(
                    "- ${p.variety}: ${nf.format(p.priceMin.toLong())} - " +
                        "${nf.format(p.priceMax.toLong())} (TB tuần: " +
                        "${nf.format(p.priceAvg7d.toLong())}) $trendIcon, $freshness\n"
                )
            }
        sb.append("Nguồn: ${prices.firstOrNull()?.region ?: "ĐBSCL"} · " +
            "Tổng số giống đang theo dõi: ${prices.size}")
        return sb.toString()
    }

    private fun buildKnowledgeBlock(
        hits: List<KnowledgeBaseRepository.KnowledgeEntry>,
        audience: KnowledgeBaseRepository.Audience
    ): String {
        val header = when (audience) {
            KnowledgeBaseRepository.Audience.TRADER -> "📚 TÀI LIỆU THỊTRƯỜNG NỘI BỘ (ưu tiên trích dẫn):"
            KnowledgeBaseRepository.Audience.FARMER -> "📚 TÀI LIỆU KHUYẾN NÔNG NỘI BỘ (ưu tiên trích dẫn):"
        }
        val sb = StringBuilder(header).append("\n")
        hits.forEachIndexed { idx, e ->
            sb.append("\n[${idx + 1}] ${e.title}\n")
            sb.append(e.content)
            sb.append("\n")
        }
        return sb.toString().trimEnd()
    }

    private fun formatFreshness(ageMs: Long): String {
        val mins = TimeUnit.MILLISECONDS.toMinutes(ageMs)
        return when {
            mins < 1 -> "vừa cập nhật"
            mins < 60 -> "$mins phút trước"
            mins < 60 * 24 -> "${mins / 60} giờ trước"
            else -> "${mins / (60 * 24)} ngày trước"
        }
    }
}

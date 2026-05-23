package com.GiaThinh.canlua.repository

import com.GiaThinh.canlua.BuildConfig
import com.GiaThinh.canlua.data.model.Profile
import com.GiaThinh.canlua.data.model.RicePrice
import com.GiaThinh.canlua.data.model.WeatherInfo
import com.GiaThinh.canlua.data.remote.HttpClient
import com.GiaThinh.canlua.data.remote.ai.ChatMessage
import com.GiaThinh.canlua.data.remote.ai.OpenRouterRequest
import com.GiaThinh.canlua.data.remote.ai.OpenRouterResponse
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        // Chuỗi model fallback theo thứ tự ưu tiên. Lý do:
        //  - openrouter/auto: OpenRouter tự chọn model rẻ/nhanh nhất phù hợp.
        //  - deepseek-v4-flash:free: backup nhanh khi auto router quá tải.
        //  - minimax-m2.5:free: backup cuối, đặc biệt tốt cho tiếng Trung/Việt.
        // Khi 1 model fail (network / 5xx / empty), ta thử model kế tiếp.
        private val MODEL_FALLBACKS = listOf(
            "openrouter/auto",
            "deepseek/deepseek-v4-flash:free",
            "minimax/minimax-m2.5:free"
        )
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

NGÔN NGỮ — RẤT QUAN TRỌNG (đối tượng đọc là bà con nông dân, nhiều người lớn tuổi):
- Thông tin phải CHÍNH XÁC nhưng diễn đạt phải GẦN GŨI, dễ hiểu như đang nói chuyện ngoài đồng.
- TUYỆT ĐỐI tránh thuật ngữ khoa học khô khan: "hoạt chất", "phổ tác động", "kháng sinh thực vật", "vi sinh đối kháng", "pH", "EC", "NPK tỷ lệ"... Nếu bắt buộc dùng, phải giải thích ngay trong ngoặc bằng từ dân dã (vd: "đạm (urê — phân màu trắng hạt nhỏ)", "lân (super lân)", "kali (kali clorua — phân hạt đỏ)").
- Tên thuốc/phân: gọi tên thương mại quen thuộc tại ĐBSCL trước, tên hoạt chất ghi sau trong ngoặc nếu cần.
- Đo lường: ưu tiên đơn vị bà con hay dùng — "công" (1.000 m²), "giạ", "bao 50kg", "bình 16 lít", "thùng phuy", thay vì hecta/lít/kg trừ khi cần chính xác.
- Câu văn ngắn (≤ 20 chữ/câu). Tránh câu phức nhiều mệnh đề.
- Xưng "bà con" / "anh/chú/cô" tự nhiên, không dùng "quý khách", "người dùng", "bạn".
- Giải thích bệnh/sâu bằng dấu hiệu mắt thường thấy được (vd: "lá vàng từ chóp xuống", "thân có đốm nâu") trước khi nêu tên bệnh.
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

NGÔN NGỮ — RẤT QUAN TRỌNG:
- Thông tin phải CHÍNH XÁC, số liệu rõ ràng, nhưng cách diễn đạt vẫn phải GẦN GŨI như đang trao đổi ngoài bến ghe.
- Tránh thuật ngữ tài chính khó: "biên gộp", "ROI", "hedging", "futures"... Nếu cần dùng phải giải thích ngay (vd: "biên lợi nhuận (lời thực sau khi trừ chi phí vận chuyển, hao hụt)").
- Dùng đơn vị thực tế: "đ/kg", "ghe", "bao 50kg", "tấn", "công". Khi nói khối lượng lớn, ưu tiên "tấn" cho gọn.
- Câu ngắn, dứt khoát, có con số. Tránh câu mơ hồ kiểu "có thể, tùy thuộc, dao động".
- Xưng "anh/chú" thay vì "quý khách", "bạn".
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
        val systemPrompt = buildSystemPrompt(profile, weather, ricePrices, knowledgeHits, audience)
        val messages = listOf(ChatMessage("system", systemPrompt)) + history
        callWithFallback(messages, context = "chat")
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
        val systemPrompt = buildSeasonAnalysisPrompt(profile, weather)
        val messages = listOf(
            ChatMessage("system", systemPrompt),
            ChatMessage("user", seasonSummary)
        )
        callWithFallback(messages, context = "analyzeSeason")
    }

    /**
     * Gọi OpenRouter với chuỗi fallback model. Lý do dùng fallback:
     *  - Free-tier models hay 429 / 5xx / empty content khi quota cạn.
     *  - Auto router thỉnh thoảng pick model lỗi → cần retry với model cụ thể.
     *
     * Quy tắc:
     *  - Thử lần lượt MODEL_FALLBACKS, dừng khi có model trả nội dung non-empty.
     *  - Mỗi lần fail/cắt: log Crashlytics non-fatal (kèm model, finish_reason).
     *  - `finish_reason == "length"` nhưng có nội dung → vẫn trả nội dung +
     *    suffix "..." để user biết bị cắt do max_tokens.
     */
    private fun callWithFallback(messages: List<ChatMessage>, context: String): Result<String> {
        val crashlytics = runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
        var lastError: Throwable? = null

        for ((idx, model) in MODEL_FALLBACKS.withIndex()) {
            try {
                val req = OpenRouterRequest(model = model, messages = messages)
                val resp: OpenRouterResponse = httpClient.postJson(
                    url = URL,
                    body = req,
                    headers = mapOf(
                        "Authorization" to "Bearer ${BuildConfig.OPENROUTER_API_KEY}",
                        "HTTP-Referer" to "https://canlua.app",
                        "X-Title" to "CanLua"
                    )
                )
                resp.error?.message?.let { errMsg ->
                    crashlytics?.log("AI[$context] model=$model error=$errMsg")
                    lastError = RuntimeException("$model: $errMsg")
                    continue
                }
                val choice = resp.choices.firstOrNull()
                val rawContent = choice?.message?.content?.trim().orEmpty()
                val finishReason = choice?.finish_reason ?: "unknown"
                crashlytics?.log(
                    "AI[$context] model=$model finish_reason=$finishReason len=${rawContent.length}"
                )

                if (rawContent.isEmpty()) {
                    lastError = RuntimeException("$model: empty content (finish=$finishReason)")
                    continue
                }
                // Có nội dung — nhưng nếu bị cắt do length, append "…" để báo user
                val finalAnswer = if (finishReason == "length") {
                    "$rawContent…\n\n_(Trả lời bị cắt do độ dài. Bà con thử hỏi gọn hơn.)_"
                } else {
                    rawContent
                }
                if (idx > 0) {
                    // Log để theo dõi tần suất fallback
                    crashlytics?.log("AI[$context] used fallback model #$idx ($model)")
                }
                return Result.success(finalAnswer)
            } catch (e: Exception) {
                crashlytics?.log("AI[$context] model=$model exception=${e.javaClass.simpleName}: ${e.message}")
                lastError = e
                // Không recordException ở đây — chỉ record cuối khi tất cả model fail
            }
        }

        // Tất cả model fail → ghi non-fatal vào Crashlytics
        val finalError = lastError ?: RuntimeException("AI không trả về nội dung")
        crashlytics?.recordException(
            RuntimeException("AI[$context] all ${MODEL_FALLBACKS.size} models failed: ${finalError.message}", finalError)
        )
        return Result.failure(finalError)
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
- KHÔNG bịa số liệu — chỉ dùng số có trong input. Số liệu phải CHÍNH XÁC tuyệt đối.
- Khi delta là +/-, gọi đúng "tăng" / "giảm".
- Nếu input thiếu data (vd: tổng số phiếu = 0), nói rõ "chưa đủ dữ liệu để phân tích".

NGÔN NGỮ — đối tượng đọc là bà con nông dân ĐBSCL:
- Diễn đạt GẦN GŨI, dễ hiểu như nói chuyện ngoài đồng. Tránh giọng văn báo cáo.
- KHÔNG dùng thuật ngữ khô khan: "hiệu suất canh tác", "biên lợi nhuận", "ROI", "tỷ suất", "delta", "biến động"... Thay bằng: "lúa được mùa hơn", "lời nhiều/ít hơn", "chênh lệch so với vụ trước".
- Đơn vị dùng quen thuộc: "công" (1.000 m²), "bao", "giạ", "tấn" thay vì "ha", "tạ" nếu được.
- Câu ngắn, không quá 20 chữ. Tránh câu phức nhiều mệnh đề lồng nhau.
- Xưng "bà con" / "anh/chú" tự nhiên.
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

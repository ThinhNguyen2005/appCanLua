package com.GiaThinh.canlua.data.remote.news

import com.GiaThinh.canlua.data.model.NewsTopic

/**
 * Các nguồn RSS được app fetch định kỳ.
 *
 * Google News RSS query format:
 *   https://news.google.com/rss/search?q=<keywords>&hl=vi&gl=VN&ceid=VN:vi
 *
 * Đã URL-encode keyword tiếng Việt UTF-8 sẵn trong URL để tránh bug khi mã hoá runtime.
 *
 * Khi 1 source sập, [NewsRepository.refresh] vẫn fetch các source khác (parallel + catch).
 */
enum class NewsSource(
    val displayName: String,
    val rssUrl: String,
    val defaultTopic: NewsTopic
) {
    /**
     * Google News chuyên đề "giá lúa" — tin ngách chính xác cho nông dân,
     * NHƯNG feed Google News strip ảnh thumbnail. Bài sẽ rơi về Article icon.
     */
    GOOGLE_RICE_PRICE(
        displayName = "Tin Lúa Gạo",
        rssUrl = "https://news.google.com/rss/search?q=%22gi%C3%A1+l%C3%BAa%22+OR+%22thu+mua+l%C3%BAa%22&hl=vi&gl=VN&ceid=VN:vi",
        defaultTopic = NewsTopic.RICE
    ),

    /**
     * VnExpress Kinh doanh — feed có `<enclosure type="image/jpeg" url="..."/>`
     * và description CDATA chứa `<img>` HTTPS sẵn → ảnh load tốt qua Coil.
     */
    VNEXPRESS_BUSINESS(
        displayName = "VnExpress Kinh Doanh",
        rssUrl = "https://vnexpress.net/rss/kinh-doanh.rss",
        defaultTopic = NewsTopic.MARKET
    ),

    /**
     * Tuổi Trẻ Kinh doanh — description CDATA bọc `<img>` HTTPS, nhiều bài
     * nông nghiệp/giá nông sản hơn VnExpress.
     */
    TUOITRE_BUSINESS(
        displayName = "Tuổi Trẻ Kinh Doanh",
        rssUrl = "https://tuoitre.vn/rss/kinh-doanh.rss",
        defaultTopic = NewsTopic.MARKET
    ),

    /**
     * Thanh Niên Kinh tế — feed có namespace `media:` và CDATA `<img>`,
     * cập nhật nhanh tin xuất khẩu gạo, biến động giá.
     */
    THANHNIEN_ECONOMY(
        displayName = "Thanh Niên Kinh Tế",
        rssUrl = "https://thanhnien.vn/rss/kinh-te.rss",
        defaultTopic = NewsTopic.GRAIN
    ),

    /**
     * CafeF Thị trường chứng khoán — bao gồm cả tin nông sản, lúa gạo
     * dưới góc nhìn thị trường. CDN cafefcdn.com phục vụ ảnh HTTPS.
     */
    CAFEF_MARKET(
        displayName = "CafeF Thị Trường",
        rssUrl = "https://cafef.vn/thi-truong-chung-khoan.rss",
        defaultTopic = NewsTopic.MARKET
    );

    companion object {
        /** Heuristic phân loại topic từ title/description vào enum cụ thể. */
        fun classify(text: String, fallback: NewsTopic): NewsTopic {
            val lower = text.lowercase()
            return when {
                lower.contains("thời tiết") || lower.contains("mưa") ||
                    lower.contains("bão") || lower.contains("nắng nóng") ||
                    lower.contains("hạn hán") || lower.contains("lũ") ||
                    lower.contains("xâm nhập mặn") -> NewsTopic.WEATHER
                lower.contains("xuất khẩu") || lower.contains("giá gạo") ||
                    lower.contains("gạo xuất") -> NewsTopic.GRAIN
                lower.contains("giá lúa") || lower.contains("thu mua lúa") ||
                    lower.contains("lúa st25") || lower.contains("vụ đông xuân") ||
                    lower.contains("vụ hè thu") -> NewsTopic.RICE
                lower.contains("thị trường") || lower.contains("nông sản") -> NewsTopic.MARKET
                else -> fallback
            }
        }
    }
}

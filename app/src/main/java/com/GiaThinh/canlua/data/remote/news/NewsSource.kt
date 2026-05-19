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
    /** "giá lúa" OR "thu mua lúa" — chuyên giá thu mua trong nước. */
    GOOGLE_RICE_PRICE(
        displayName = "Tin Lúa Gạo",
        rssUrl = "https://news.google.com/rss/search?q=%22gi%C3%A1+l%C3%BAa%22+OR+%22thu+mua+l%C3%BAa%22&hl=vi&gl=VN&ceid=VN:vi",
        defaultTopic = NewsTopic.RICE
    ),

    /** "xuất khẩu gạo" OR "giá gạo" — gạo xuất khẩu, biến động giá quốc tế. */
    GOOGLE_GRAIN_EXPORT(
        displayName = "Xuất Khẩu Gạo",
        rssUrl = "https://news.google.com/rss/search?q=%22xu%E1%BA%A5t+kh%E1%BA%A9u+g%E1%BA%A1o%22+OR+%22gi%C3%A1+g%E1%BA%A1o%22&hl=vi&gl=VN&ceid=VN:vi",
        defaultTopic = NewsTopic.GRAIN
    ),

    /** "thời tiết ĐBSCL" OR "mưa lũ miền Tây" — cảnh báo nông nghiệp ĐBSCL. */
    GOOGLE_WEATHER_MEKONG(
        displayName = "Thời Tiết Nông Nghiệp",
        rssUrl = "https://news.google.com/rss/search?q=%22th%E1%BB%9Di+ti%E1%BA%BFt+%C4%90BSCL%22+OR+%22m%C6%B0a+l%C5%A9+mi%E1%BB%81n+T%C3%A2y%22&hl=vi&gl=VN&ceid=VN:vi",
        defaultTopic = NewsTopic.WEATHER
    ),

    /** "thị trường nông sản" — biến động giá tổng quát. */
    GOOGLE_AGRI_MARKET(
        displayName = "Thị Trường Nông Sản",
        rssUrl = "https://news.google.com/rss/search?q=%22th%E1%BB%8B+tr%C6%B0%E1%BB%9Dng+n%C3%B4ng+s%E1%BA%A3n%22&hl=vi&gl=VN&ceid=VN:vi",
        defaultTopic = NewsTopic.MARKET
    ),

    /** Báo Nông Nghiệp Việt Nam — kinh tế nông nghiệp chính thống. */
    NONGNGHIEP_VN(
        displayName = "Báo Nông Nghiệp",
        rssUrl = "https://nongnghiep.vn/rss/kinh-te-nong-nghiep.rss",
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

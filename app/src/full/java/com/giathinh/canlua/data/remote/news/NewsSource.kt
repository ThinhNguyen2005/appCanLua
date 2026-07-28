package com.giathinh.canlua.data.remote.news

import com.giathinh.canlua.data.model.NewsTopic

/**
 * Các nguồn RSS được app fetch định kỳ.
 *
 * Google News RSS query format:
 *   https://news.google.com/rss/search?q=<keywords>&hl=vi&gl=VN&ceid=VN:vi
 *
 * Đã URL-encode keyword tiếng Việt UTF-8 sẵn trong URL để tránh bug khi mã hoá runtime.
 *
 * Khi 1 source sập, [NewsRepository.refresh] vẫn fetch các source khác (parallel + catch).
 *
 * Lựa chọn nguồn: ưu tiên feed có description CDATA chứa `<img>` HTTPS (Coil load
 * được) và tốc độ cập nhật nhanh. Mỗi nguồn cover một góc nhìn khác nhau để bà con
 * không bị "trùng tin": Google News (đa nguồn), báo địa phương (ĐBSCL), báo
 * chuyên ngành (Nông Nghiệp VN, Dân Việt), báo kinh tế (VnExpress, Tuổi Trẻ,
 * Thanh Niên, Vietnamnet, VOV, CafeF).
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
     * Google News "nông nghiệp" — bao quát tin canh tác, mùa vụ, kỹ thuật,
     * chính sách nông nghiệp. Mở rộng phạm vi ngoài chỉ giá lúa.
     */
    GOOGLE_AGRICULTURE(
        displayName = "Tin Nông Nghiệp",
        rssUrl = "https://news.google.com/rss/search?q=%22n%C3%B4ng+nghi%E1%BB%87p%22+OR+%22n%C3%B4ng+d%C3%A2n%22&hl=vi&gl=VN&ceid=VN:vi",
        defaultTopic = NewsTopic.MARKET
    ),

    /**
     * Google News "xuất khẩu gạo" — tin xuất khẩu, đơn hàng, giá quốc tế
     * ảnh hưởng trực tiếp giá thu mua nội địa.
     */
    GOOGLE_RICE_EXPORT(
        displayName = "Xuất Khẩu Gạo",
        rssUrl = "https://news.google.com/rss/search?q=%22xu%E1%BA%A5t+kh%E1%BA%A9u+g%E1%BA%A1o%22&hl=vi&gl=VN&ceid=VN:vi",
        defaultTopic = NewsTopic.GRAIN
    ),

    /**
     * Google News "ĐBSCL nông sản" — tin vùng Mekong Delta — vùng trọng điểm
     * trồng lúa của Việt Nam. Bao gồm xâm nhập mặn, thuỷ sản, mùa vụ.
     */
    GOOGLE_MEKONG(
        displayName = "Tin ĐBSCL",
        rssUrl = "https://news.google.com/rss/search?q=%22%C4%90BSCL%22+OR+%22%C4%91%E1%BB%93ng+b%E1%BA%B1ng+s%C3%B4ng+C%E1%BB%ADu+Long%22+n%C3%B4ng&hl=vi&gl=VN&ceid=VN:vi",
        defaultTopic = NewsTopic.MARKET
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
     * Nông Nghiệp Môi Trường — chuyên trang nông nghiệp & môi trường.
     */
    NONGNGHIEPMOITRUONG(
        displayName = "Nông Nghiệp Môi Trường",
        rssUrl = "https://nongnghiepmoitruong.vn/nong-nghiep.rss",
        defaultTopic = NewsTopic.RICE
    ),

    /**
     * CafeF Thị trường chứng khoán — bao gồm cả tin nông sản, lúa gạo
     * dưới góc nhìn thị trường. CDN cafefcdn.com phục vụ ảnh HTTPS.
     */
    CAFEF_MARKET(
        displayName = "CafeF Thị Trường",
        rssUrl = "https://cafef.vn/thi-truong-chung-khoan.rss",
        defaultTopic = NewsTopic.MARKET
    ),

    /**
     * Báo Dân Việt — chuyên trang Nhà Nông, gốc Hội Nông dân Việt Nam,
     * tin canh tác, giống mới, mô hình hợp tác xã, chính sách hỗ trợ.
     */
    DANVIET_AGRICULTURE(
        displayName = "Dân Việt Nhà Nông",
        rssUrl = "https://danviet.vn/rss/nha-nong-552.rss",
        defaultTopic = NewsTopic.RICE
    ),

    /**
     * Vietnamnet Kinh doanh — đưa tin chính sách, ngân hàng, vay vốn nông dân,
     * thị trường nông sản xuất khẩu.
     */
    VIETNAMNET_BUSINESS(
        displayName = "Vietnamnet Kinh Doanh",
        rssUrl = "https://vietnamnet.vn/rss/kinh-doanh.rss",
        defaultTopic = NewsTopic.MARKET
    ),

    /**
     * VOV Kinh tế — Đài Tiếng nói Việt Nam, đăng tin nhanh hợp đồng xuất khẩu,
     * khuyến nông, dự báo mùa vụ. Có nhiều bài về giá nông sản.
     */
    VOV_ECONOMY(
        displayName = "VOV Kinh Tế",
        rssUrl = "https://vov.vn/rss/kinh-te-83.rss",
        defaultTopic = NewsTopic.MARKET
    );

    companion object {
        /**
         * Heuristic phân loại topic từ title/description vào enum cụ thể.
         * Sắp xếp ưu tiên từ specific → generic để bài "giá lúa ST25" không bị
         * route nhầm vào MARKET trước khi qua RICE.
         */
        fun classify(text: String, fallback: NewsTopic): NewsTopic {
            val lower = text.lowercase()
            return when {
                // Thời tiết / thiên tai — ảnh hưởng trực tiếp mùa vụ, phân loại vào thị trường
                lower.contains("thời tiết") || lower.contains("mưa") ||
                    lower.contains("bão") || lower.contains("nắng nóng") ||
                    lower.contains("hạn hán") || lower.contains("lũ") ||
                    lower.contains("xâm nhập mặn") || lower.contains("ngập mặn") ||
                    lower.contains("triều cường") -> NewsTopic.MARKET

                // Gạo & xuất khẩu — thị trường quốc tế, đơn hàng, giá FOB
                lower.contains("xuất khẩu") || lower.contains("giá gạo") ||
                    lower.contains("gạo xuất") || lower.contains("gạo việt") ||
                    lower.contains("nhập khẩu gạo") || lower.contains("hợp đồng gạo") ||
                    lower.contains("đơn hàng gạo") -> NewsTopic.GRAIN

                // Lúa — canh tác, giống, mùa vụ, thu mua tận ruộng
                lower.contains("giá lúa") || lower.contains("thu mua lúa") ||
                    lower.contains("lúa st25") || lower.contains("lúa st24") ||
                    lower.contains("vụ đông xuân") || lower.contains("vụ hè thu") ||
                    lower.contains("vụ thu đông") || lower.contains("vụ mùa") ||
                    lower.contains("trồng lúa") || lower.contains("ruộng lúa") ||
                    lower.contains("giống lúa") -> NewsTopic.RICE

                // Thị trường & nông sản nói chung — fallback có hướng dẫn
                lower.contains("thị trường") || lower.contains("nông sản") ||
                    lower.contains("nông nghiệp") || lower.contains("nông dân") ||
                    lower.contains("đbscl") || lower.contains("đồng bằng sông cửu long") ||
                    lower.contains("hợp tác xã") || lower.contains("khuyến nông") ||
                    lower.contains("phân bón") || lower.contains("thuốc bảo vệ thực vật") -> NewsTopic.MARKET

                else -> fallback
            }
        }
    }
}

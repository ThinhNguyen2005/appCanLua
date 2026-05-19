package com.GiaThinh.canlua.data.model

import java.util.Calendar
import java.util.Date

/**
 * Domain models cho tính năng Thống Kê Mùa Vụ.
 *
 * Lý do tách Raw vs UI model:
 *  - Room aggregate trả về nullable cho SUM/AVG khi table empty → SeasonStatsRaw
 *    chứa nullable fields, mapping sang SeasonStats với default 0.0 ở Repository.
 *  - UI dùng SeasonStats sạch, không cần handle nullable mỗi chỗ.
 */

/** Raw data từ Room aggregate query — fields nullable do SUM/AVG có thể null. */
data class SeasonStatsRaw(
    val cardCount: Int = 0,
    val totalNetWeight: Double? = null,
    val totalRevenue: Double? = null,
    val totalPaid: Double? = null,
    val totalRemaining: Double? = null,
    val avgPrice: Double? = null,
    val avgMoisture: Double? = null,
    val totalBags: Int = 0
) {
    fun toDomain(season: String): SeasonStats = SeasonStats(
        season = season,
        cardCount = cardCount,
        totalNetWeight = totalNetWeight ?: 0.0,
        totalRevenue = totalRevenue ?: 0.0,
        totalPaid = totalPaid ?: 0.0,
        totalRemaining = totalRemaining ?: 0.0,
        avgPricePerKg = avgPrice ?: 0.0,
        avgMoisture = avgMoisture ?: 0.0,
        totalBags = totalBags
    )
}

/** Stats sạch dùng trong UI/ViewModel. */
data class SeasonStats(
    val season: String,
    val cardCount: Int,
    val totalNetWeight: Double,
    val totalRevenue: Double,
    val totalPaid: Double,
    val totalRemaining: Double,
    val avgPricePerKg: Double,
    val avgMoisture: Double,
    val totalBags: Int
) {
    val isEmpty: Boolean get() = cardCount == 0
}

/** Phân bổ giống lúa trong 1 vụ. */
data class VarietyStat(
    val variety: String,
    val weight: Double,
    val count: Int
)

/** Top thương lái mua trong 1 vụ. */
data class TraderStat(
    val traderName: String,
    val revenue: Double,
    val deals: Int
)

/**
 * 1 dòng lịch sử thương lái đã mua ruộng — aggregate từ bảng cards.
 * Dùng cho FarmerProfileScreen → "Lịch sử thu mua".
 */
data class TraderHistoryItem(
    val traderName: String,
    val traderPhone: String,
    val deals: Int,
    val totalRevenue: Double,
    val totalWeight: Double,
    val lastDealDate: Long
)

/**
 * Helper: 3 vụ chuẩn miền Tây Nam Bộ.
 *
 * Lịch nông vụ truyền thống:
 *  - Đông Xuân:  gieo sạ Tháng 11–12, thu hoạch Tháng 2–4 (vụ chính, năng suất cao nhất)
 *  - Hè Thu:     gieo sạ Tháng 4–5, thu hoạch Tháng 7–8
 *  - Thu Đông:   gieo sạ Tháng 8–9, thu hoạch Tháng 11–12 (vụ phụ, không phải nơi nào cũng làm)
 */
enum class SeasonType(val displayName: String) {
    DONG_XUAN("Đông Xuân"),
    HE_THU("Hè Thu"),
    THU_DONG("Thu Đông");

    /** Format thành label chuẩn: "Đông Xuân 2026". */
    fun toLabel(year: Int): String = "$displayName $year"
}

object SeasonHelper {

    /** Suggest vụ phù hợp dựa trên ngày hiện tại — dùng cho default value của season picker. */
    fun suggestFromDate(date: Date = Date()): String {
        val cal = Calendar.getInstance().apply { time = date }
        val month = cal.get(Calendar.MONTH) + 1  // 1..12
        val year = cal.get(Calendar.YEAR)

        return when (month) {
            // Tháng 11–12 → Đông Xuân của năm sau (vì gieo cuối năm, thu hoạch đầu năm sau)
            11, 12 -> SeasonType.DONG_XUAN.toLabel(year + 1)
            // Tháng 1–4 → Đông Xuân năm hiện tại
            1, 2, 3, 4 -> SeasonType.DONG_XUAN.toLabel(year)
            // Tháng 5–8 → Hè Thu
            5, 6, 7, 8 -> SeasonType.HE_THU.toLabel(year)
            // Tháng 9–10 → Thu Đông
            else -> SeasonType.THU_DONG.toLabel(year)
        }
    }

    /** Tạo list 3 options (có sẵn năm phù hợp) cho dropdown trong CreateCardDialog. */
    fun currentSeasonOptions(date: Date = Date()): List<String> {
        val cal = Calendar.getInstance().apply { time = date }
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        // Đông Xuân: nếu đang ở Tháng 11–12 → vụ năm sau, ngược lại năm hiện tại
        val dxYear = if (month >= 11) year + 1 else year

        return listOf(
            SeasonType.DONG_XUAN.toLabel(dxYear),
            SeasonType.HE_THU.toLabel(year),
            SeasonType.THU_DONG.toLabel(year)
        )
    }
}

package com.giathinh.canlua.util

import androidx.annotation.StringRes
import com.giathinh.canlua.R
import com.giathinh.canlua.data.model.SeasonType
import java.util.Calendar
import java.util.Date

/**
 * Đại diện cho một gợi ý vụ mùa (loại vụ mùa + năm).
 * Tách biệt dữ liệu nghiệp vụ để UI tự chịu trách nhiệm định dạng và đa ngôn ngữ (Localization).
 */
data class SeasonOption(
    val type: SeasonType,
    val year: Int
) {
    @get:StringRes
    val nameRes: Int
        get() = when (type) {
            SeasonType.DONG_XUAN -> R.string.season_dong_xuan
            SeasonType.HE_THU -> R.string.season_he_thu
            SeasonType.THU_DONG -> R.string.season_thu_dong
        }
}

object SeasonUtil {

    /**
     * Gợi ý 3 vụ mùa chuẩn theo lịch nông vụ miền Tây dựa trên mốc thời gian cung cấp.
     */
    fun currentSeasonOptions(date: Date = Date()): List<SeasonOption> {
        val cal = Calendar.getInstance().apply { time = date }
        val month = cal.get(Calendar.MONTH) + 1 // 1..12
        val year = cal.get(Calendar.YEAR)

        // Đông Xuân: gieo sạ cuối năm (Tháng 11–12) thu hoạch đầu năm sau -> năm vụ = year + 1
        val dxYear = if (month >= 11) year + 1 else year

        return listOf(
            SeasonOption(SeasonType.DONG_XUAN, dxYear),
            SeasonOption(SeasonType.HE_THU, year),
            SeasonOption(SeasonType.THU_DONG, year)
        )
    }
}

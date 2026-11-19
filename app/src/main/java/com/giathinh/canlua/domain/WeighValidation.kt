package com.giathinh.canlua.domain

/**
 * Validation tập trung cho chức năng Cân lúa.
 *
 * Mọi đường ghi (UI, ViewModel, Repository) dùng chung bộ quy tắc này để không
 * còn tình trạng UI ép giá trị sai thành 0 hoặc Repository nhận NaN/Infinity.
 * Parser phân biệt rõ ô trống ([ParsedNumber.Blank]) và dữ liệu sai
 * ([ParsedNumber.Invalid]) — không bao giờ biến lỗi thành 0.
 */
object WeighValidation {

    /** Giới hạn khối lượng một dòng cân (kg), giữ nguyên nghiệp vụ hiện tại. */
    const val MAX_BAG_KG = 200.0

    /** Độ ẩm chuẩn ngành lúa gạo (%). */
    const val STANDARD_MOISTURE = 14.0

    /** Kết quả parse một ô số thô do người dùng nhập. */
    sealed interface ParsedNumber {
        data class Value(val value: Double) : ParsedNumber
        data object Blank : ParsedNumber
        data object Invalid : ParsedNumber
    }

    /**
     * Parse chuỗi số người dùng. Chấp nhận dấu phẩy HOẶC dấu chấm thập phân
     * (`18,5` == `18.5`). Từ chối chuỗi trộn cả hai (`1.2,3`), nhiều dấu thập
     * phân, NaN/Infinity. Chuỗi rỗng/khoảng trắng → [ParsedNumber.Blank].
     */
    fun parseNumber(raw: String?): ParsedNumber {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return ParsedNumber.Blank
        val hasComma = trimmed.contains(',')
        val hasDot = trimmed.contains('.')
        if (hasComma && hasDot) return ParsedNumber.Invalid
        val normalized = trimmed.replace(',', '.')
        if (normalized.count { it == '.' } > 1) return ParsedNumber.Invalid
        val value = normalized.toDoubleOrNull() ?: return ParsedNumber.Invalid
        if (!value.isFinite()) return ParsedNumber.Invalid
        return ParsedNumber.Value(value)
    }

    /** Convenience: trả giá trị hợp lệ hoặc null (gộp Blank/Invalid). */
    fun parseOrNull(raw: String?): Double? =
        (parseNumber(raw) as? ParsedNumber.Value)?.value

    /** Dòng cân hợp lệ: hữu hạn, dương, không vượt [MAX_BAG_KG]. */
    fun isValidWeight(kg: Double): Boolean =
        kg.isFinite() && kg > 0.0 && kg <= MAX_BAG_KG

    /** Độ ẩm hợp lệ: [0, 100). 0 = chưa nhập (không quy đổi). */
    fun isValidMoisture(percent: Double): Boolean =
        percent.isFinite() && percent >= 0.0 && percent < 100.0

    /** Tạp chất: kg hữu hạn không âm; nếu theo % thì thêm cận trên 100. */
    fun isValidImpurity(value: Double, isPercent: Boolean): Boolean =
        value.isFinite() && value >= 0.0 && (!isPercent || value <= 100.0)

    /** Giá / cọc: hữu hạn, không âm. */
    fun isValidNonNegativeMoney(value: Double): Boolean =
        value.isFinite() && value >= 0.0

    /** Thanh toán mới người dùng nhập: hữu hạn, dương thực sự. */
    fun isValidPositivePayment(value: Double): Boolean =
        value.isFinite() && value > 0.0

    /** Số bao mẫu phải nguyên dương khi chọn phương pháp lấy mẫu / tỷ lệ. */
    fun isValidSampleCount(count: Int): Boolean = count > 0
}
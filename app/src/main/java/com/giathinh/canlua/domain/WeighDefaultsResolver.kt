package com.giathinh.canlua.domain

/**
 * Chuẩn hóa quy tắc bao bì về một bộ trường nhất quán.
 *
 * Lỗi cũ: đổi tỷ lệ "10 bao = 1kg" chỉ cập nhật `bagSampleCount` nhưng chế độ
 * không lấy mẫu vẫn tính bằng `bagWeight` cũ → trừ bì sai. Ở đây mọi đường thay
 * đổi quy tắc đều gọi [normalizeBagRule] để cập nhật ĐỒNG THỜI `bagWeight` cùng
 * mode/count/sampleTotal.
 *
 * - Không lấy mẫu: `bagWeight = 1 / count` (count bao đổi được 1kg bì).
 * - Lấy mẫu: `bagWeight = sampleTotalWeight / count` (kg bì trung bình mỗi bao).
 *
 * Legacy `count <= 0`: giữ [fallbackBagWeight] hiện có, không tự đổi hàng loạt.
 */
object WeighDefaultsResolver {

    data class BagRule(
        val bagWeight: Double,
        val isSampling: Boolean,
        val sampleCount: Int,
        val sampleTotalWeight: Double
    )

    fun normalizeBagRule(
        isSampling: Boolean,
        sampleCount: Int,
        sampleTotalWeight: Double,
        fallbackBagWeight: Double
    ): BagRule {
        if (sampleCount <= 0) {
            return BagRule(
                bagWeight = fallbackBagWeight,
                isSampling = isSampling,
                sampleCount = sampleCount,
                sampleTotalWeight = sampleTotalWeight
            )
        }
        val unit = if (isSampling) {
            sampleTotalWeight.coerceAtLeast(0.0) / sampleCount
        } else {
            1.0 / sampleCount
        }
        return BagRule(
            bagWeight = unit,
            isSampling = isSampling,
            sampleCount = sampleCount,
            sampleTotalWeight = if (isSampling) sampleTotalWeight else 1.0
        )
    }
}
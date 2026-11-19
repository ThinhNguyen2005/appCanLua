package com.giathinh.canlua.domain

import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.util.RiceSettlement
import kotlin.math.roundToLong

/**
 * Mapper DUY NHẤT từ dữ liệu phiếu → [RiceSettlement.Input].
 *
 * Cả Repository (khi recalc để ghi DB) và ViewModel (khi tính preview) đều đi
 * qua đây, bảo đảm preview và quyết toán không lệch nhau. Điểm mấu chốt: truyền
 * **trọng lượng từng bao thật** (`entries.map { it.weight }`), KHÔNG dựng bao
 * trung bình `totalRaw / bagCount` rồi làm tròn từng bao như logic preview cũ.
 */
object CardSettlement {

    /**
     * Dựng input từ card + danh sách dòng cân + tổng ledger đã làm tròn sang VND.
     * Repository truyền tổng ledger thực tế; preview truyền tổng từ aggregate của card.
     */
    fun toInput(
        card: Card,
        entries: List<WeightEntry>,
        paidVnd: Long,
        depositVnd: Long
    ): RiceSettlement.Input = RiceSettlement.Input(
        entryWeightsKg = entries.map { it.weight },
        bagWeightKg = card.bagWeight,
        bagMethodIsSampling = card.bagMethodIsSampling,
        bagSampleCount = card.bagSampleCount,
        bagSampleTotalWeightKg = card.bagSampleTotalWeight,
        impurityValue = card.impurityWeight,
        impurityIsPercent = card.impurityIsPercent,
        moisturePercent = card.moisturePercent,
        pricePerKg = card.pricePerKg,
        paidVnd = paidVnd,
        depositVnd = depositVnd
    )

    /** Input cho preview: paid/deposit lấy từ aggregate hiện tại của card. */
    fun inputFromCard(card: Card, entries: List<WeightEntry>): RiceSettlement.Input =
        toInput(
            card = card,
            entries = entries,
            paidVnd = card.paidAmount.roundToLong(),
            depositVnd = card.depositAmount.roundToLong()
        )

    /**
     * Tính preview đầy đủ. UI đọc [RiceSettlement.Settlement.totalNetKg] và
     * [RiceSettlement.Settlement.totalAmountVnd] từ cùng kết quả này, không tự
     * nhân tiền bằng Double ở Composable.
     */
    fun preview(card: Card, entries: List<WeightEntry>): RiceSettlement.Settlement =
        RiceSettlement.compute(inputFromCard(card, entries))
}
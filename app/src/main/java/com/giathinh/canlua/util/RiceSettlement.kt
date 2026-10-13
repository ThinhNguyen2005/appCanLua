package com.giathinh.canlua.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Tính toán phiếu cân "không rơi rớt":
 *  - Khối lượng nội bộ tính bằng Long, 1 unit = 0,01 kg  → không lệch dấu phẩy động.
 *  - Bì / tạp chất / KL thực của từng dòng được CHIA từ tổng bằng phương pháp
 *    phần dư lớn nhất → Σ dòng == tổng phiếu tuyệt đối.
 *  - Tiền tính bằng Long VND, làm tròn đúng 1 lần ở "thành tiền".
 *  - Trả dư được tách riêng (overpaid) thay vì bị clamp về 0.
 *  - [Settlement.verify] kiểm tra toàn bộ bất biến; rỗng = đúng.
 *
 * Giả định: mỗi dòng cân hợp lệ (weight > 0) = 1 bao (giống code hiện tại).
 */
object RiceSettlement {

    private const val UNIT = 100.0 // 1 kg = 100 unit (0.01 kg)

    data class Input(
        val entryWeightsKg: List<Double>,
        val bagWeightKg: Double = 0.0,
        val bagMethodIsSampling: Boolean = false,
        val bagSampleCount: Int = 0,
        val bagSampleTotalWeightKg: Double = 0.0,
        val impurityValue: Double = 0.0,
        val impurityIsPercent: Boolean = false,
        val moisturePercent: Double = 0.0,
        val standardMoisture: Double = 14.0,
        /**
         * true  = cộng thêm KL khi lúa khô hơn chuẩn (ẩm < standardMoisture).
         * false = chỉ trừ KL khi ẩm > chuẩn; ẩm ≤ chuẩn giữ nguyên.
         */
        val addWeightWhenDry: Boolean = true,
        val pricePerKg: Double = 0.0,
        val paidVnd: Long = 0L,
        val depositVnd: Long = 0L
    )

    data class Settlement(
        /** KL thô (units) theo từng dòng hợp lệ (weight > 0), theo thứ tự xuất hiện. */
        val rawUnits: List<Long>,
        /** Bì (units) theo từng dòng — Σ == totalBagUnits tuyệt đối. */
        val bagUnits: List<Long>,
        /** Tạp chất (units) theo từng dòng — Σ == totalImpurityUnits tuyệt đối. */
        val impurityUnits: List<Long>,
        /** KL thực (units) theo từng dòng — Σ == totalNetUnits tuyệt đối. */
        val netUnits: List<Long>,
        val totalRawUnits: Long,
        val totalBagUnits: Long,
        val totalImpurityUnits: Long,
        /** Phần điều chỉnh do quy ẩm (âm = ẩm cao hơn chuẩn → KL giảm). */
        val moistureAdjUnits: Long,
        val totalNetUnits: Long,
        val totalAmountVnd: Long,
        val paidVnd: Long,
        val depositVnd: Long,
        /** Còn phải trả (≥ 0). */
        val remainingVnd: Long,
        /** Trả dư — số tiền đã trả vượt quá thành tiền (≥ 0). */
        val overpaidVnd: Long,
        val warnings: List<String>
    ) {
        fun kg(units: Long): Double = units / UNIT
        val totalNetKg get() = kg(totalNetUnits)

        /**
         * Trả về danh sách vi phạm bất biến.
         * **Rỗng = mọi thứ khớp** và phiếu có thể được ghi vào DB.
         */
        fun verify(): List<String> = buildList {
            if (rawUnits.sum() != totalRawUnits) add("Σ thô dòng ≠ tổng thô")
            if (bagUnits.sum() != totalBagUnits) add("Σ bì dòng ≠ tổng bì")
            if (impurityUnits.sum() != totalImpurityUnits) add("Σ tạp chất dòng ≠ tổng tạp chất")
            if (netUnits.sum() != totalNetUnits) add("Σ KL thực dòng ≠ KL thực phiếu")
            if (totalRawUnits - totalBagUnits - totalImpurityUnits + moistureAdjUnits != totalNetUnits)
                add("Thô − bì − tạp chất + điều chỉnh ẩm ≠ KL thực")
            if (totalBagUnits < 0 || totalImpurityUnits < 0 || totalNetUnits < 0) add("Có tổng bị âm")
            if (netUnits.any { it < 0 }) add("Có dòng KL thực âm")
            if (remainingVnd < 0 || overpaidVnd < 0) add("Còn lại / trả dư âm")
            if (remainingVnd - overpaidVnd != totalAmountVnd - paidVnd - depositVnd)
                add("Còn lại − trả dư ≠ thành tiền − đã trả − cọc")
        }
    }

    fun compute(i: Input): Settlement {
        val warnings = mutableListOf<String>()
        fun fmtKg(u: Long) = "%.2f".format(u / UNIT)

        // ── 1. Thô ──────────────────────────────────────────────────────────
        val raw = i.entryWeightsKg.filter { it > 0.0 }.map { toUnits(it) }
        val n = raw.size
        val totalRaw = raw.sum()

        // ── 2. Bì ───────────────────────────────────────────────────────────
        val bagKg = RiceCalculator.calcTotalBagWeight(
            bagCount = n,
            bagWeight = i.bagWeightKg.coerceAtLeast(0.0),
            methodIsSampling = i.bagMethodIsSampling,
            sampleCount = i.bagSampleCount,
            sampleTotalWeight = i.bagSampleTotalWeightKg.coerceAtLeast(0.0)
        )
        var totalBag = toUnits(bagKg)
        if (totalBag > totalRaw) {
            warnings += "Tổng bì ${fmtKg(totalBag)} kg lớn hơn tổng KL cân ${fmtKg(totalRaw)} kg — kiểm tra lại số liệu"
            totalBag = totalRaw
        }
        // Chia bì đều theo số bao (weights = 1.0 mỗi bao, vì 1 dòng = 1 bao)
        val bag = allocate(totalBag, List(n) { 1.0 })
        if (raw.indices.any { bag[it] > raw[it] }) {
            warnings += "Có dòng cân nhẹ hơn bì của nó — kiểm tra lại dòng cân"
        }

        // ── 3. Tạp chất (chia theo tỷ lệ KL sau trừ bì) ────────────────────
        val afterBag = raw.indices.map { raw[it] - bag[it] }
        val totalAfterBag = totalRaw - totalBag

        val impV = i.impurityValue.coerceAtLeast(0.0)
        if (i.impurityIsPercent && impV > 100.0) warnings += "Tạp chất > 100%"
        var totalImp = if (i.impurityIsPercent) {
            (totalAfterBag * impV / 100.0).roundToLong()
        } else {
            toUnits(impV)
        }
        if (totalImp > totalAfterBag) {
            warnings += "Tạp chất ${fmtKg(totalImp)} kg vượt KL sau trừ bì ${fmtKg(totalAfterBag)} kg"
            totalImp = totalAfterBag
        }
        // Chia tạp chất theo tỷ lệ KL sau bì của từng dòng
        val imp = allocate(totalImp, afterBag.map { it.toDouble() })

        // ── 4. Quy ẩm ───────────────────────────────────────────────────────
        val gross = raw.indices.map { afterBag[it] - imp[it] }
        val totalGross = totalAfterBag - totalImp

        val std = i.standardMoisture
        val m = i.moisturePercent
        val factor = when {
            m <= 0.0 -> {
                if (totalRaw > 0) warnings += "Chưa nhập độ ẩm — không quy đổi, KL giữ nguyên"
                1.0
            }
            m >= 100.0 || std <= 0.0 || std >= 100.0 -> {
                warnings += "Độ ẩm không hợp lệ ($m%) — không quy đổi"
                1.0
            }
            m < std && !i.addWeightWhenDry -> 1.0
            else -> (100.0 - m) / (100.0 - std)
        }
        val totalNet = (totalGross * factor).roundToLong().coerceAtLeast(0L)
        // Chia KL thực theo tỷ lệ gross từng dòng
        val net = allocate(totalNet, gross.map { it.toDouble() })

        // ── 5. Tiền (Long VND, làm tròn 1 lần) ─────────────────────────────
        val amount = BigDecimal(totalNet)
            .multiply(BigDecimal.valueOf(i.pricePerKg.coerceAtLeast(0.0)))
            .divide(BigDecimal.valueOf(100L), 0, RoundingMode.HALF_UP)
            .toLong()
        val diff = amount - i.paidVnd - i.depositVnd
        val remaining = if (diff > 0) diff else 0L
        val overpaid  = if (diff < 0) -diff else 0L
        if (overpaid > 0) warnings += "Trả dư $overpaid đ"

        return Settlement(
            rawUnits          = raw,
            bagUnits          = bag,
            impurityUnits     = imp,
            netUnits          = net,
            totalRawUnits     = totalRaw,
            totalBagUnits     = totalBag,
            totalImpurityUnits = totalImp,
            moistureAdjUnits  = totalNet - totalGross,
            totalNetUnits     = totalNet,
            totalAmountVnd    = amount,
            paidVnd           = i.paidVnd,
            depositVnd        = i.depositVnd,
            remainingVnd      = remaining,
            overpaidVnd       = overpaid,
            warnings          = warnings
        )
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** 1 kg = 10 units (làm tròn gần nhất). */
    private fun toUnits(kg: Double): Long = (kg * UNIT).roundToLong()

    /**
     * Chia [total] cho n phần theo [weights] sao cho **Σ == total tuyệt đối**
     * (Largest Remainder Method).
     * - weights âm được coi như 0.
     * - Khi tất cả weights = 0, chia đều.
     * - Khi [total] ≤ 0, trả danh sách toàn 0.
     */
    internal fun allocate(total: Long, weights: List<Double>): List<Long> {
        val n = weights.size
        if (n == 0) return emptyList()
        if (total <= 0L) return List(n) { 0L }
        val w = weights.map { it.coerceAtLeast(0.0) }
        val sumW = w.sum()
        val ideal = DoubleArray(n) { if (sumW > 0.0) total * w[it] / sumW else total.toDouble() / n }
        val share = LongArray(n) { floor(ideal[it]).toLong() }
        var rest = total - share.sum()
        val byFraction = (0 until n).sortedByDescending { ideal[it] - share[it] }
        var k = 0
        while (rest > 0) { share[byFraction[k++ % n]]++; rest-- }
        // Phòng thủ: sai số float cực hiếm gặp nhưng bảo vệ để không out-of-bound
        k = 0
        while (rest < 0) {
            val idx = byFraction[n - 1 - (k++ % n)]
            if (share[idx] > 0) { share[idx]--; rest++ }
        }
        return share.toList()
    }
}

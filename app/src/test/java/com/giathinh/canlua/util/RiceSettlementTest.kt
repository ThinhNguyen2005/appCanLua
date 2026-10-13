package com.giathinh.canlua.util

import com.giathinh.canlua.util.RiceSettlement.Input
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests cho [RiceSettlement].
 *
 * Chạy bằng: ./gradlew :app:testDebugUnitTest --tests "*.RiceSettlementTest"
 *
 * Bài test ngẫu nhiên 5.000 phiếu sẽ phát hiện mọi vi phạm bất biến nếu
 * ai đó sửa công thức mà quên cập nhật [RiceSettlement.Settlement.verify].
 */
class RiceSettlementTest {

    // ── Bài toán có đáp án tay ────────────────────────────────────────────

    @Test
    fun knownExample_matchesHandCalculation() {
        // 1000 kg thô, bì 2 × 10 = 20 kg, tạp chất 10 kg, ẩm 18% → chuẩn 14%
        // gross = 1000 - 20 - 10 = 970 kg
        // net   = 970 × (100-18)/(100-14) = 970 × 82/86 ≈ 924.8837... kg → 92488 units (0.01 kg)
        // amount = 92488 × 7000 / 100 = 6_474_160 đ
        // remaining = 6_474_160 - 5_000_000 = 1_474_160 đ
        val s = RiceSettlement.compute(
            Input(
                entryWeightsKg = listOf(500.0, 500.0),
                bagWeightKg    = 10.0,
                impurityValue  = 10.0,
                moisturePercent = 18.0,
                pricePerKg     = 7000.0,
                paidVnd        = 5_000_000L
            )
        )
        assertEquals("totalNetUnits", 92488L, s.totalNetUnits)
        assertEquals("totalAmountVnd", 6_474_160L, s.totalAmountVnd)
        assertEquals("remainingVnd", 1_474_160L, s.remainingVnd)
        assertEquals("overpaidVnd", 0L, s.overpaidVnd)
        assertEquals("verify", emptyList<String>(), s.verify())
    }

    @Test
    fun twoDecimalPlaces_precisionPreservedWithoutLoss() {
        // 2 dòng có 2 chữ số thập phân: 50.25 kg + 49.75 kg = 100.00 kg
        // UNIT = 100.0: rawUnits = [5025, 4975], totalRawUnits = 10000
        val s = RiceSettlement.compute(
            Input(
                entryWeightsKg = listOf(50.25, 49.75),
                pricePerKg = 8000.0
            )
        )
        assertEquals("rawUnits[0]", 5025L, s.rawUnits[0])
        assertEquals("rawUnits[1]", 4975L, s.rawUnits[1])
        assertEquals("totalRawUnits", 10000L, s.totalRawUnits)
        assertEquals("kg[0]", 50.25, s.kg(s.rawUnits[0]), 0.001)
        assertEquals("kg[1]", 49.75, s.kg(s.rawUnits[1]), 0.001)
        assertEquals("totalAmountVnd", 800_000L, s.totalAmountVnd)
        assertEquals("verify", emptyList<String>(), s.verify())
    }

    // ── Bug #1 cũ: Σ dòng ≠ phiếu (do làm tròn từng dòng) ───────────────

    @Test
    fun rowsSumToTotal_whereOldLogicDrifted() {
        // 3 dòng × 33.3 kg, ẩm 18%.
        // 33.3 × 3 = 99.9 kg → 9990 units.
        // factor = 82/86 → 9990 × 82/86 = 9525.3488... → 9525 units.
        val s = RiceSettlement.compute(
            Input(entryWeightsKg = listOf(33.3, 33.3, 33.3), moisturePercent = 18.0, pricePerKg = 7000.0)
        )
        assertEquals("totalNetUnits", 9525L, s.totalNetUnits)
        assertEquals("Σ netUnits == totalNetUnits", s.totalNetUnits, s.netUnits.sum())
        assertEquals("verify", emptyList<String>(), s.verify())
    }

    // ── Bug #2 cũ: trả dư bị xoá sổ ─────────────────────────────────────

    @Test
    fun overpayment_isTrackedNotLost() {
        // 100 kg, giá 7.000, thành tiền 700.000. Đã trả 800.000 → trả dư 100.000
        val s = RiceSettlement.compute(
            Input(entryWeightsKg = listOf(100.0), pricePerKg = 7000.0, paidVnd = 800_000L)
        )
        assertEquals("totalAmountVnd", 700_000L, s.totalAmountVnd)
        assertEquals("remainingVnd", 0L, s.remainingVnd)
        assertEquals("overpaidVnd", 100_000L, s.overpaidVnd)
        assertEquals("verify", emptyList<String>(), s.verify())
    }

    // ── Cảnh báo nhưng vẫn nhất quán ─────────────────────────────────────

    @Test
    fun bagBiggerThanRaw_warnsAndStaysConsistent() {
        val s = RiceSettlement.compute(Input(entryWeightsKg = listOf(5.0), bagWeightKg = 20.0))
        assertTrue("should warn", s.warnings.isNotEmpty())
        assertEquals("net == 0", 0L, s.totalNetUnits)
        assertEquals("verify", emptyList<String>(), s.verify())
    }

    @Test
    fun impurityBiggerThanAfterBag_warnsAndStaysConsistent() {
        // 100 kg, bì 10 kg, tạp chất 200 kg → tạp chất bị clamp về 90 kg
        val s = RiceSettlement.compute(
            Input(entryWeightsKg = listOf(100.0), bagWeightKg = 10.0, impurityValue = 200.0)
        )
        assertTrue("should warn", s.warnings.any { it.contains("Tạp chất") })
        assertEquals("net == 0", 0L, s.totalNetUnits)
        assertEquals("verify", emptyList<String>(), s.verify())
    }

    @Test
    fun noMoistureInput_warnsButKeepsWeight() {
        // Không nhập ẩm → factor = 1, KL giữ nguyên
        val s = RiceSettlement.compute(Input(entryWeightsKg = listOf(100.0), moisturePercent = 0.0))
        assertTrue("should warn", s.warnings.any { it.contains("Chưa nhập độ ẩm") })
        assertEquals("net == raw", s.totalRawUnits, s.totalNetUnits)
        assertEquals("verify", emptyList<String>(), s.verify())
    }

    // ── Phiếu rỗng / dòng âm ─────────────────────────────────────────────

    @Test
    fun emptyCard_isSafe() {
        val s = RiceSettlement.compute(Input(entryWeightsKg = emptyList(), paidVnd = 1_000L))
        assertEquals("amount == 0", 0L, s.totalAmountVnd)
        assertEquals("overpaid == paid", 1_000L, s.overpaidVnd)
        assertEquals("verify", emptyList<String>(), s.verify())
    }

    @Test
    fun negativeWeightEntries_areFiltered() {
        // Dòng âm bị lọc; chỉ dòng 100.0 được tính (100.00 kg = 10000 units)
        val s = RiceSettlement.compute(Input(entryWeightsKg = listOf(-5.0, 0.0, 100.0)))
        assertEquals("rawUnits count", 1, s.rawUnits.size)
        assertEquals("totalRawUnits", 10000L, s.totalRawUnits)
        assertEquals("verify", emptyList<String>(), s.verify())
    }

    // ── Phương pháp mẫu bao bì ────────────────────────────────────────────

    @Test
    fun bagSamplingMethod_computesCorrectly() {
        // 5 bao mẫu cân ra 7.5 kg → đơn vị 1.5 kg/bao × 10 bao = 15 kg bì (1500 units)
        val s = RiceSettlement.compute(
            Input(
                entryWeightsKg          = List(10) { 50.0 },  // 10 dòng × 50 kg = 500 kg
                bagMethodIsSampling     = true,
                bagSampleCount          = 5,
                bagSampleTotalWeightKg  = 7.5,
                moisturePercent         = 14.0  // chuẩn → factor = 1
            )
        )
        assertEquals("totalBagUnits", 1500L, s.totalBagUnits)  // 15 kg × 100 = 1500 units
        assertEquals("verify", emptyList<String>(), s.verify())
    }

    // ── Tạp chất theo % ───────────────────────────────────────────────────

    @Test
    fun impurityAsPercent_computesCorrectly() {
        // 100 kg thô, không bì, tạp chất 5% → 5 kg tạp chất
        val s = RiceSettlement.compute(
            Input(
                entryWeightsKg  = listOf(100.0),
                impurityValue   = 5.0,
                impurityIsPercent = true,
                moisturePercent = 14.0
            )
        )
        assertEquals("totalImpurityUnits", 500L, s.totalImpurityUnits)  // 5 kg × 100
        assertEquals("totalNetUnits", 9500L, s.totalNetUnits)             // 95 kg × 100
        assertEquals("verify", emptyList<String>(), s.verify())
    }

    // ── Allocate internal ─────────────────────────────────────────────────

    @Test
    fun allocate_sumsToTotal_always() {
        // 10 units chia cho 3 phần bằng nhau: [4, 3, 3] hoặc [3, 4, 3] v.v.
        val result = RiceSettlement.allocate(10L, listOf(1.0, 1.0, 1.0))
        assertEquals("sum == total", 10L, result.sum())
        assertTrue("all non-negative", result.all { it >= 0 })
    }

    @Test
    fun allocate_zeroTotal_returnsAllZeros() {
        val result = RiceSettlement.allocate(0L, listOf(1.0, 2.0, 3.0))
        assertEquals(listOf(0L, 0L, 0L), result)
    }

    @Test
    fun allocate_unevenWeights_sumsExactly() {
        // 100 units chia theo tỷ lệ [1, 2, 7] → [10, 20, 70]
        val result = RiceSettlement.allocate(100L, listOf(1.0, 2.0, 7.0))
        assertEquals("sum", 100L, result.sum())
        assertEquals("[0]", 10L, result[0])
        assertEquals("[1]", 20L, result[1])
        assertEquals("[2]", 70L, result[2])
    }

    // ── Bài ngẫu nhiên 5.000 phiếu ───────────────────────────────────────

    @Test
    fun invariants_holdForRandomInputs() {
        val rnd = Random(42)
        repeat(5_000) { idx ->
            val n = rnd.nextInt(0, 60)
            val s = RiceSettlement.compute(
                Input(
                    entryWeightsKg         = List(n) { rnd.nextDouble(-5.0, 120.0) },
                    bagWeightKg            = rnd.nextDouble(0.0, 3.0),
                    bagMethodIsSampling    = rnd.nextBoolean(),
                    bagSampleCount         = rnd.nextInt(0, 6),
                    bagSampleTotalWeightKg = rnd.nextDouble(0.0, 15.0),
                    impurityValue          = rnd.nextDouble(0.0, 40.0),
                    impurityIsPercent      = rnd.nextBoolean(),
                    moisturePercent        = rnd.nextDouble(-1.0, 35.0),
                    addWeightWhenDry       = rnd.nextBoolean(),
                    pricePerKg             = rnd.nextDouble(0.0, 12_000.0),
                    paidVnd                = rnd.nextLong(0, 5_000_000),
                    depositVnd             = rnd.nextLong(0, 1_000_000)
                )
            )
            val violations = s.verify()
            assertEquals("violations at idx=$idx n=$n", emptyList<String>(), violations)
        }
    }
}

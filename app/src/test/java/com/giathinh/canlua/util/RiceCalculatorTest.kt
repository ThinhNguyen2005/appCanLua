package com.giathinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests cho [RiceCalculator] — bảo vệ công thức tính tiền cân lúa.
 *
 * Chuẩn ngành lúa gạo:
 *  - Quy đổi khối lượng theo công thức chuẩn: W_std = (W_raw - bao - tạp) × (100 - moisture) / (100 - standardMoisture)
 *  - Hỗ trợ cả hai chế độ trừ tạp chất: kg cố định (`isPercent = false`) và tỷ lệ % (`isPercent = true`)
 */
class RiceCalculatorTest {

    private val eps = 0.01

    // === calcStandardWeight ===

    @Test
    fun `calcStandardWeight handles standard, high, invalid, custom standard, and boundary weights`() {
        data class Case(
            val raw: Double,
            val moisture: Double,
            val standardMoisture: Double = 14.0,
            val expected: Double
        )
        listOf(
            Case(raw = 1000.0, moisture = 14.0, expected = 1000.0),
            Case(raw = 1000.0, moisture = 14.1, expected = 998.84),
            Case(raw = 1000.0, moisture = 20.0, expected = 930.23),
            Case(raw = 1000.0, moisture = 25.0, expected = 872.09),
            Case(raw = 1000.0, moisture = 35.0, expected = 755.81),
            Case(raw = 1000.0, moisture = 99.9, expected = 1.16),
            Case(raw = 1000.0, moisture = 14.0, standardMoisture = 13.0, expected = 988.51),
            Case(raw = 1000.0, moisture = 0.0, expected = 1000.0),
            Case(raw = 1000.0, moisture = 100.0, expected = 1000.0),
            Case(raw = 500.0, moisture = -0.001, expected = 500.0),
            Case(raw = 1000.0, moisture = 150.0, expected = 1000.0),
            Case(raw = 0.0, moisture = 20.0, expected = 0.0)
        ).forEach { (raw, moisture, stdMoisture, expected) ->
            val actual = RiceCalculator.calcStandardWeight(
                rawWeight = raw,
                moisturePercent = moisture,
                standardMoisture = stdMoisture
            )
            assertEquals(
                "raw=$raw moisture=$moisture std=$stdMoisture",
                expected,
                actual,
                eps
            )
        }
    }

    @Test
    fun `calcStandardWeight negative raw weight propagates through formula`() {
        val result = RiceCalculator.calcStandardWeight(-100.0, 20.0)
        assertTrue("Kết quả phải âm cho raw âm", result < 0)
    }

    // === calcNetWeight ===

    @Test
    fun `calcNetWeight subtracts bag and impurity then converts moisture`() {
        data class Case(
            val raw: Double,
            val bag: Double,
            val impurity: Double,
            val moisture: Double,
            val expected: Double
        )
        listOf(
            Case(raw = 1000.0, bag = 50.0, impurity = 10.0, moisture = 20.0, expected = 874.42),
            Case(raw = 1000.0, bag = 50.0, impurity = 10.0, moisture = 14.0, expected = 940.0),
            Case(raw = 1000.0, bag = 0.0, impurity = 0.0, moisture = 20.0, expected = 930.23),
            Case(raw = 1000.0, bag = 0.0, impurity = 0.0, moisture = 22.0, expected = 906.98),
            Case(raw = 250.0, bag = 7.5, impurity = 2.0, moisture = 14.0, expected = 240.5),
            Case(raw = 5000.0, bag = 50.0, impurity = 20.0, moisture = 18.0, expected = 4700.70),
            Case(raw = 100.0, bag = 60.0, impurity = 40.0, moisture = 20.0, expected = 0.0),
            Case(raw = 100.0, bag = 80.0, impurity = 50.0, moisture = 20.0, expected = 0.0),
            Case(raw = 0.0, bag = 0.0, impurity = 0.0, moisture = 20.0, expected = 0.0)
        ).forEach { (raw, bag, impurity, moisture, expected) ->
            assertEquals(
                "raw=$raw bag=$bag impurity=$impurity moisture=$moisture",
                expected,
                RiceCalculator.calcNetWeight(raw, bag, impurity, moisture),
                eps
            )
        }
    }

    // === calcTotalBagWeight ===

    @Test
    fun `calcTotalBagWeight handles unit, sampling, zero-count, and fallback cases`() {
        data class Case(
            val bagCount: Int,
            val bagWeight: Double,
            val methodIsSampling: Boolean,
            val sampleCount: Int,
            val sampleTotalWeight: Double,
            val expected: Double
        )
        listOf(
            Case(100, 0.5, false, 0, 0.0, 50.0),
            Case(0, 0.5, false, 0, 0.0, 0.0),
            Case(100, 0.0, true, 10, 5.0, 50.0),
            Case(50, 0.0, true, 1, 0.45, 22.5),
            // methodIsSampling=true nhưng sampleCount <= 0 → fallback về bagCount × bagWeight
            Case(100, 0.5, true, 0, 5.0, 50.0)
        ).forEach { (count, bagW, sampling, sampleCount, sampleTotal, expected) ->
            assertEquals(
                "count=$count sampling=$sampling sampleCount=$sampleCount",
                expected,
                RiceCalculator.calcTotalBagWeight(count, bagW, sampling, sampleCount, sampleTotal),
                eps
            )
        }
    }

    // === calcTotalImpurity ===

    @Test
    fun `calcTotalImpurity handles both percent mode and fixed kg mode`() {
        data class Case(
            val rawAfterBag: Double,
            val impurityValue: Double,
            val isPercent: Boolean,
            val expected: Double
        )
        listOf(
            Case(rawAfterBag = 1000.0, impurityValue = 2.0, isPercent = true, expected = 20.0),
            Case(rawAfterBag = 950.0, impurityValue = 2.0, isPercent = true, expected = 19.0),
            Case(rawAfterBag = -100.0, impurityValue = 5.0, isPercent = true, expected = 0.0),
            Case(rawAfterBag = 1000.0, impurityValue = 2.0, isPercent = false, expected = 2.0),
            Case(rawAfterBag = 500.0, impurityValue = 10.0, isPercent = false, expected = 10.0),
            Case(rawAfterBag = 1000.0, impurityValue = -5.0, isPercent = false, expected = 0.0),
            Case(rawAfterBag = 1000.0, impurityValue = -5.0, isPercent = true, expected = 0.0),
            Case(rawAfterBag = 1000.0, impurityValue = 0.0, isPercent = false, expected = 0.0)
        ).forEach { (rawAfterBag, value, isPercent, expected) ->
            assertEquals(
                "rawAfterBag=$rawAfterBag value=$value isPercent=$isPercent",
                expected,
                RiceCalculator.calcTotalImpurity(rawAfterBag, value, isPercent),
                eps
            )
        }
    }

    // === calcTotalAmount & calcRemainingAmount ===

    @Test
    fun `calcTotalAmount computes net times price including zero and negative guards`() {
        listOf(
            Triple(874.42, 7000.0, 6_120_940.0),
            Triple(240.5, 8500.0, 2_044_250.0),
            Triple(4700.0, 8500.0, 39_950_000.0),
            Triple(0.0, 7000.0, 0.0),
            Triple(1000.0, 0.0, 0.0)
        ).forEach { (net, price, expected) ->
            assertEquals(expected, RiceCalculator.calcTotalAmount(net, price), 1.0)
        }
        assertTrue(
            "Giá âm → tổng tiền âm (caller phải block trước)",
            RiceCalculator.calcTotalAmount(netWeight = 100.0, pricePerKg = -1000.0) < 0
        )
    }

    @Test
    fun `calcRemainingAmount handles deposit, paid, zero, and overpaid`() {
        data class Case(val total: Double, val paid: Double, val deposit: Double, val expected: Double)
        listOf(
            Case(10_000_000.0, 3_000_000.0, 2_000_000.0, 5_000_000.0),
            Case(5_000_000.0, 2_000_000.0, 0.0, 3_000_000.0),
            Case(10_000_000.0, 10_000_000.0, 0.0, 0.0),
            Case(10_000_000.0, 0.0, 1_000_000.0, 9_000_000.0),
            Case(0.0, 0.0, 0.0, 0.0),
            Case(1_000_000.0, 800_000.0, 500_000.0, -300_000.0)
        ).forEach { (total, paid, deposit, expected) ->
            assertEquals(expected, RiceCalculator.calcRemainingAmount(total, paid, deposit), eps)
        }
    }

    // === calcNetWeightWithModes & Kịch bản tích hợp ===

    /**
     * Regression test phát hiện bug cũ ở Repository:
     * Công thức `totalRaw - totalRaw × moist/100` cho ra số THẤP HƠN nhiều
     * so với chuẩn ngành, làm nông dân bị thiệt.
     */
    @Test
    fun `regression test - new formula gives more weight than buggy formula at 22 percent`() {
        val correct = RiceCalculator.calcNetWeight(1000.0, 0.0, 0.0, 22.0)
        val buggy = 1000.0 * (100.0 - 22.0) / 100.0  // = 780
        assertTrue(
            "Công thức chuẩn ($correct) phải lớn hơn công thức buggy ($buggy)",
            correct > buggy
        )
        assertTrue("Chênh lệch phải > 100kg/tấn", correct - buggy > 100)
    }

    @Test
    fun `calcNetWeightWithModes supports percent impurity and bag sampling`() {
        // 1000 kg thô, không bì, tạp chất 2% (= 20 kg), ẩm chuẩn 14% → 980 kg
        val netPercentImp = RiceCalculator.calcNetWeightWithModes(
            totalRaw = 1000.0,
            bagCount = 0,
            bagWeight = 0.0,
            bagMethodIsSampling = false,
            bagSampleCount = 0,
            bagSampleTotalWeight = 0.0,
            impurityValue = 2.0,
            impurityIsPercent = true,
            moisturePercent = 14.0
        )
        assertEquals(980.0, netPercentImp, eps)

        // 1000 kg thô, 100 bao với mẫu 10 bao = 5 kg → tổng bì 50 kg, không tạp, ẩm 14% → 950 kg
        val netSampling = RiceCalculator.calcNetWeightWithModes(
            totalRaw = 1000.0,
            bagCount = 100,
            bagWeight = 0.0,
            bagMethodIsSampling = true,
            bagSampleCount = 10,
            bagSampleTotalWeight = 5.0,
            impurityValue = 0.0,
            impurityIsPercent = false,
            moisturePercent = 14.0
        )
        assertEquals(950.0, netSampling, eps)
    }

    @Test
    fun `calcNetWeightWithModes percent impurity - global and per-entry net match`() {
        // raw 1000, 100 bao × 0.5kg = tổng bao 50kg → rawAfterBag = 950
        // tạp 2% trên rawAfterBag = 950 × 2% = 19.0 kg → gross = 950 - 19 = 931 kg
        // @14% moisture → netWeight = 931.0 kg
        val totalRaw = 1000.0
        val bagCount = 100
        val bagWeight = 0.5
        val impurityPercent = 2.0
        val moisturePercent = 14.0

        val totalBag = RiceCalculator.calcTotalBagWeight(bagCount, bagWeight, false, 0, 0.0)
        val singleBagWeight = totalBag / bagCount

        val totalImpurity = RiceCalculator.calcTotalImpurity(
            rawAfterBag = totalRaw - totalBag,
            impurityValue = impurityPercent,
            isPercent = true
        )
        assertEquals(19.0, totalImpurity, eps)
        val singleImpurityWeight = totalImpurity / bagCount // 0.19 kg/bao

        val globalNetWeight = RiceCalculator.calcNetWeightWithModes(
            totalRaw = totalRaw,
            bagCount = bagCount,
            bagWeight = bagWeight,
            bagMethodIsSampling = false,
            bagSampleCount = 0,
            bagSampleTotalWeight = 0.0,
            impurityValue = impurityPercent,
            impurityIsPercent = true,
            moisturePercent = moisturePercent
        )
        assertEquals(931.0, globalNetWeight, eps)

        var sumNetWeight = 0.0
        repeat(bagCount) {
            sumNetWeight += RiceCalculator.calcNetWeight(
                rawWeight = totalRaw / bagCount,
                bagWeight = singleBagWeight,
                impurityWeight = singleImpurityWeight,
                moisturePercent = moisturePercent
            )
        }
        assertEquals(
            "Per-entry sum phải khớp global (tolerance = 100 × rounding unit)",
            globalNetWeight,
            sumNetWeight,
            5.0
        )
    }

    @Test
    fun `integration - Dong Thap rice 28 percent moisture 10 ton truck`() {
        // Xe tải 10 tấn: 10000kg thô; 200 bao, mẫu 10 bao = 5.2kg → bì 104kg
        // rawAfterBag = 9896kg; tạp 30kg (cố định) → gross = 9866kg; ẩm 28% → 9866 × 72/86 = 8259.91kg
        val net = RiceCalculator.calcNetWeightWithModes(
            totalRaw = 10_000.0,
            bagCount = 200,
            bagWeight = 0.0,
            bagMethodIsSampling = true,
            bagSampleCount = 10,
            bagSampleTotalWeight = 5.2,
            impurityValue = 30.0,
            impurityIsPercent = false,
            moisturePercent = 28.0
        )
        assertEquals(8259.91, net, eps)

        val total = RiceCalculator.calcTotalAmount(net, 8000.0)
        assertEquals(66_079_280.0, total, 200.0)
    }
}

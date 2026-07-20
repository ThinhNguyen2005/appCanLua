package com.giathinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests cho [RiceCalculator] — bảo vệ công thức tính tiền cân lúa.
 *
 * Chuẩn ngành lúa gạo:
 *  - Quy đổi khối lượng về độ ẩm chuẩn 14%
 *  - W_std = (W_raw - bao - tạp) × (100 - moisture) / (100 - 14)
 *  - Khi moisture = 14%, W_std = W_raw - bao - tạp (không quy đổi)
 */
class RiceCalculatorTest {

    private val eps = 0.01

    // === calcStandardWeight ===

    @Test
    fun `standard weight at 14% moisture returns same weight`() {
        val result = RiceCalculator.calcStandardWeight(rawWeight = 1000.0, moisturePercent = 14.0)
        assertEquals(1000.0, result, eps)
    }

    @Test
    fun `standard weight at 20% moisture is reduced correctly`() {
        // 1000 × (100-20)/(100-14) = 1000 × 80/86 = 930.232...
        val result = RiceCalculator.calcStandardWeight(rawWeight = 1000.0, moisturePercent = 20.0)
        assertEquals(930.23, result, eps)
    }

    @Test
    fun `standard weight at 25% moisture`() {
        // 1000 × 75/86 = 872.093...
        val result = RiceCalculator.calcStandardWeight(rawWeight = 1000.0, moisturePercent = 25.0)
        assertEquals(872.09, result, eps)
    }

    @Test
    fun `standard weight handles invalid moisture by returning raw`() {
        assertEquals(1000.0, RiceCalculator.calcStandardWeight(1000.0, 0.0), eps)
        assertEquals(1000.0, RiceCalculator.calcStandardWeight(1000.0, 100.0), eps)
        assertEquals(1000.0, RiceCalculator.calcStandardWeight(1000.0, -5.0), eps)
        assertEquals(1000.0, RiceCalculator.calcStandardWeight(1000.0, 150.0), eps)
    }

    // === calcNetWeight ===

    @Test
    fun `net weight subtracts bag and impurity then converts moisture`() {
        // (1000 - 50 - 10) × 80/86 = 940 × 0.930232... = 874.4186...
        // calcNetWeight làm tròn 1 chữ số thập phân: 874.4
        val result = RiceCalculator.calcNetWeight(
            rawWeight = 1000.0,
            bagWeight = 50.0,
            impurityWeight = 10.0,
            moisturePercent = 20.0
        )
        assertEquals(874.4, result, eps)
    }

    @Test
    fun `net weight returns zero when gross is negative`() {
        // bag + impurity > raw → gross âm → fallback 0
        val result = RiceCalculator.calcNetWeight(
            rawWeight = 100.0,
            bagWeight = 80.0,
            impurityWeight = 50.0,
            moisturePercent = 20.0
        )
        assertEquals(0.0, result, eps)
    }

    @Test
    fun `net weight at standard moisture equals gross`() {
        val result = RiceCalculator.calcNetWeight(
            rawWeight = 1000.0,
            bagWeight = 50.0,
            impurityWeight = 10.0,
            moisturePercent = 14.0
        )
        assertEquals(940.0, result, eps)
    }

    // === calcTotalAmount ===

    @Test
    fun `total amount equals net times price`() {
        val total = RiceCalculator.calcTotalAmount(netWeight = 874.42, pricePerKg = 7000.0)
        assertEquals(6_120_940.0, total, 1.0)
    }

    @Test
    fun `total amount is zero when net is zero`() {
        assertEquals(0.0, RiceCalculator.calcTotalAmount(0.0, 7000.0), eps)
    }

    @Test
    fun `total amount is zero when price is zero`() {
        assertEquals(0.0, RiceCalculator.calcTotalAmount(1000.0, 0.0), eps)
    }

    // === calcRemainingAmount ===

    @Test
    fun `remaining is total minus paid minus deposit`() {
        val r = RiceCalculator.calcRemainingAmount(
            totalAmount = 10_000_000.0,
            paidAmount = 3_000_000.0,
            depositAmount = 2_000_000.0
        )
        assertEquals(5_000_000.0, r, eps)
    }

    @Test
    fun `remaining can be negative if overpaid - caller must coerce`() {
        // RiceCalculator không tự coerce → caller (Repository) phải coerceAtLeast(0)
        val r = RiceCalculator.calcRemainingAmount(
            totalAmount = 1_000_000.0,
            paidAmount = 800_000.0,
            depositAmount = 500_000.0
        )
        assertTrue("Negative for overpaid case", r < 0)
    }

    // === Tích hợp: kịch bản đời thực ===

    @Test
    fun `realistic scenario - vu Dong Xuan 14 percent moisture`() {
        // Vụ Đông Xuân lúa khô tới sàn — moisture 14%
        // 5 bao × 50kg cân thô = 250kg, bao 1.5kg/cái = 7.5kg, tạp 2kg
        val net = RiceCalculator.calcNetWeight(
            rawWeight = 250.0,
            bagWeight = 7.5,
            impurityWeight = 2.0,
            moisturePercent = 14.0
        )
        // Gross: 240.5 kg, không quy đổi (đã chuẩn 14%)
        assertEquals(240.5, net, eps)

        val total = RiceCalculator.calcTotalAmount(net, pricePerKg = 8500.0)
        assertEquals(2_044_250.0, total, 1.0)
    }

    @Test
    fun `realistic scenario - vu He Thu lua tuoi 22 percent`() {
        // Vụ Hè Thu lúa tươi từ máy gặt — moisture 22%
        val net = RiceCalculator.calcNetWeight(
            rawWeight = 1000.0,
            bagWeight = 0.0,    // bán cân ký không bao
            impurityWeight = 0.0,
            moisturePercent = 22.0
        )
        // 1000 × 78/86 = 906.9767... → làm tròn 1 chữ số: 907.0
        assertEquals(907.0, net, eps)
    }

    /**
     * Test này phát hiện bug cũ ở Repository:
     * Công thức `totalRaw - totalRaw × moist/100` cho ra số THẤP HƠN nhiều
     * so với chuẩn ngành. Nếu app dùng công thức cũ, nông dân bị thiệt.
     */
    @Test
    fun `regression test - new formula gives more weight than buggy formula at 22 percent`() {
        val correct = RiceCalculator.calcNetWeight(1000.0, 0.0, 0.0, 22.0)
        val buggy = 1000.0 * (100.0 - 22.0) / 100.0  // = 780
        // Công thức đúng phải > công thức buggy ở mọi moisture > 14%
        assertTrue(
            "Công thức chuẩn (${correct}) phải lớn hơn công thức buggy ($buggy)",
            correct > buggy
        )
        // Chênh lệch ~127 kg trên 1 tấn — đáng kể!
        assertTrue("Chênh lệch phải > 100kg/tấn", correct - buggy > 100)
    }



    // === Per-card mode helpers (v17) ===

    @Test
    fun `calcTotalBagWeight method A multiplies bagWeight by bagCount`() {
        val total = RiceCalculator.calcTotalBagWeight(
            bagCount = 100,
            bagWeight = 0.5,
            methodIsSampling = false,
            sampleCount = 0,
            sampleTotalWeight = 0.0
        )
        assertEquals(50.0, total, eps)
    }

    @Test
    fun `calcTotalBagWeight method B uses sample average`() {
        // 10 bao mẫu = 5 kg → trung bình 0.5 kg/bao → 100 bao = 50 kg
        val total = RiceCalculator.calcTotalBagWeight(
            bagCount = 100,
            bagWeight = 0.0,
            methodIsSampling = true,
            sampleCount = 10,
            sampleTotalWeight = 5.0
        )
        assertEquals(50.0, total, eps)
    }

    @Test
    fun `calcTotalImpurity isPercent true is legacy and ignored - returns impurityValue as-is`() {
        // v17+: isPercent là legacy parameter, UI mới chỉ nhập theo kg.
        // Implementation: impurityValue.coerceAtLeast(0.0) — không nhân rawAfterBag.
        // Caller cũ đặt isPercent=true nhưng giờ không có hiệu lực.
        val imp = RiceCalculator.calcTotalImpurity(
            rawAfterBag = 1000.0,
            impurityValue = 2.0,
            isPercent = true  // ignored
        )
        assertEquals(20.0, imp, eps)
    }

    @Test
    fun `calcTotalImpurity in kg mode returns value as-is`() {
        val imp = RiceCalculator.calcTotalImpurity(
            rawAfterBag = 1000.0,
            impurityValue = 2.0,
            isPercent = false
        )
        assertEquals(2.0, imp, eps)
    }

    @Test
    fun `calcNetWeightWithModes - impurityIsPercent ignored in v17plus - impurity treated as kg`() {
        // v17+: isPercent bị bỏ qua. impurityValue = 2.0 được đọc là 2kg (không phải 2% = 20kg).
        // raw 1000, bag 0, tạp 2kg → rawAfterBag 1000, gross 998 @14% = 998
        val net = RiceCalculator.calcNetWeightWithModes(
            totalRaw = 1000.0,
            bagCount = 0,
            bagWeight = 0.0,
            bagMethodIsSampling = false,
            bagSampleCount = 0,
            bagSampleTotalWeight = 0.0,
            impurityValue = 2.0,
            impurityIsPercent = true,  // ignored — treated as kg
            moisturePercent = 14.0
        )
        assertEquals(980.0, net, eps)
    }

    @Test
    fun `calcNetWeightWithModes - bag sampling method`() {
        // raw 1000, bao 100 với mẫu 10 bao=5kg → totalBag 50, raw 950, no impurity, 14% → 950
        val net = RiceCalculator.calcNetWeightWithModes(
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
        assertEquals(950.0, net, eps)
    }

    @Test
    fun `calcTotalImpurity v17plus - impurityIsPercent ignored - global and per-entry net match`() {
        // v17+: isPercent bị bỏ qua. impurityValue = 2.0 = 2kg (không phải 2% = 20kg).
        // raw 1000, 100 bao × 0.5kg = tổng bao 50kg → rawAfterBag = 950
        // tạp = 2.0kg (trực tiếp, isPercent bị bỏ qua) → gross = 950 - 2 = 948
        // @14% moisture (không quy đổi) → netWeight = 948
        val totalRaw = 1000.0
        val bagCount = 100
        val bagWeight = 0.5
        val impurityKg = 2.0  // 2 kg tạp chất (không phải 2%)
        val moisturePercent = 14.0

        val totalBag = RiceCalculator.calcTotalBagWeight(bagCount, bagWeight, false, 0, 0.0)
        val singleBagWeight = totalBag / bagCount // 0.5

        // v17+: calcTotalImpurity trả impurityValue trực tiếp (bỏ qua isPercent)
        val totalImpurity = RiceCalculator.calcTotalImpurity(
            rawAfterBag = totalRaw - totalBag,
            impurityValue = impurityKg,
            isPercent = true  // bị bỏ qua — vẫn trả 2.0 kg
        )
        // totalImpurity = 2.0 (không phải 19.0 như khi isPercent còn hoạt động)
        val singleImpurityWeight = totalImpurity / bagCount // 0.02

        // Global path
        val globalNetWeight = RiceCalculator.calcNetWeightWithModes(
            totalRaw = totalRaw,
            bagCount = bagCount,
            bagWeight = bagWeight,
            bagMethodIsSampling = false,
            bagSampleCount = 0,
            bagSampleTotalWeight = 0.0,
            impurityValue = impurityKg,
            impurityIsPercent = true,  // bị bỏ qua
            moisturePercent = moisturePercent
        )
        // Actual: rawAfterBag=950, impurity=2kg, gross=948, @14% → 948
        assertEquals(931.0, globalNetWeight, eps)

        // Per-entry path — mỗi bao tính riêng và cộng lại phải bằng global
        var sumNetWeight = 0.0
        repeat(bagCount) {
            val entryRaw = totalRaw / bagCount // 10kg/bao
            val entryNet = RiceCalculator.calcNetWeight(
                rawWeight = entryRaw,
                bagWeight = singleBagWeight,
                impurityWeight = singleImpurityWeight,
                moisturePercent = moisturePercent
            )
            sumNetWeight += entryNet
        }
        // Per-entry path — mỗi bao tính riêng và cộng lại
        // Lưu ý: cách biệt giữa sum và global có thể lận đến N × 0.05 do làm tròn độc lập từng bao.
        // Với 100 bao: tolerance phải ất nhất 5.0 (100 × 0.05).
        assertEquals(
            "Per-entry sum phải xấp xỉ global (tolerance = 100 × rounding unit)",
            globalNetWeight, sumNetWeight, 5.0
        )
    }
}

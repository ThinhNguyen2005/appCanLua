package com.giathinh.canlua.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests bổ sung cho [RiceCalculator] — các edge case bị bỏ qua,
 * và tests ghi lại các hành vi đã thay đổi giữa các version (legacy behavior).
 *
 * ⚠️ BUG #2 DOCUMENTATION:
 * Test trong RiceCalculatorTest.kt dòng 237-245 (`calcTotalImpurity in percent mode`)
 * và dòng 257-272 (`calcNetWeightWithModes - impurity percent at 14 moisture`)
 * kỳ vọng behavior "isPercent=true → nhân với rawAfterBag".
 * Nhưng implementation hiện tại đã thay thành: `return impurityValue.coerceAtLeast(0.0)`
 * (bỏ qua isPercent hoàn toàn, vì UI mới không còn dùng percent mode).
 *
 * Hai test đó trong file gốc SẼ FAIL với implementation hiện tại.
 * File này ghi lại behavior ĐÚNG HIỆN TẠI và thêm edge case bị bỏ qua.
 */
class RiceCalculatorEdgeCaseTest {

    private val eps = 0.01

    // === calcTotalImpurity — Behavior hiện tại (v17+): bỏ qua isPercent ===

    @Test
    fun `calcTotalImpurity current behavior - isPercent true is ignored returns value as-is`() {
        // BUG #2: Test gốc expect 20.0 nhưng implementation hiện tại trả 2.0.
        // Test này ghi lại behavior THỰC TẾ hiện tại.
        val imp = RiceCalculator.calcTotalImpurity(
            rawAfterBag = 1000.0,
            impurityValue = 2.0,
            isPercent = true  // bị bỏ qua — legacy parameter
        )
        // Implementation: impurityValue.coerceAtLeast(0.0) = 2.0, bất kể isPercent
        assertEquals(
            "isPercent bị bỏ qua trong v17+ — luôn trả impurityValue trực tiếp",
            2.0, imp, eps
        )
    }

    @Test
    fun `calcTotalImpurity legacy percent param with isPercent=false also returns value`() {
        val imp = RiceCalculator.calcTotalImpurity(
            rawAfterBag = 500.0,
            impurityValue = 10.0,
            isPercent = false
        )
        assertEquals(10.0, imp, eps)
    }

    @Test
    fun `calcTotalImpurity coerces negative to zero`() {
        val imp = RiceCalculator.calcTotalImpurity(
            rawAfterBag = 1000.0,
            impurityValue = -5.0,
            isPercent = false
        )
        assertEquals(0.0, imp, eps)
    }

    @Test
    fun `calcTotalImpurity zero returns zero`() {
        assertEquals(0.0, RiceCalculator.calcTotalImpurity(1000.0, 0.0, false), eps)
    }

    // === calcNetWeightWithModes — behavior hiện tại với isPercent bị bỏ qua ===

    @Test
    fun `calcNetWeightWithModes actual behavior with impurityValue 2 and moisture 14`() {
        // BUG #2: Test gốc expect 980.0 (2% × 1000 = 20 → 1000-20=980)
        // Nhưng implementation: impurity = 2.0 (bỏ qua isPercent) → gross = 998 → net = 998
        val net = RiceCalculator.calcNetWeightWithModes(
            totalRaw = 1000.0,
            bagCount = 0,
            bagWeight = 0.0,
            bagMethodIsSampling = false,
            bagSampleCount = 0,
            bagSampleTotalWeight = 0.0,
            impurityValue = 2.0,
            impurityIsPercent = true, // bị bỏ qua
            moisturePercent = 14.0
        )
        // Actual: 1000 - 0 (bag) - 2 (impurity treated as kg) = 998 @14% = 998
        assertEquals(
            "isPercent bị bỏ qua: impurity = 2kg không phải 2% = 20kg",
            998.0, net, eps
        )
    }

    // === calcStandardWeight — edge cases chưa được test ===

    @Test
    fun `calcStandardWeight custom standard moisture 13`() {
        // moisture=14, standard=13 → weight nhẹ hơn vì 14% > 13%
        // W_std = 1000 × (100-14) / (100-13) = 1000 × 86/87 = 988.50...
        val result = RiceCalculator.calcStandardWeight(
            rawWeight = 1000.0,
            moisturePercent = 14.0,
            standardMoisture = 13.0
        )
        assertEquals(988.50, result, eps)
    }

    @Test
    fun `calcStandardWeight moisture just above standard 14 point 1`() {
        // 14.1% → W_std = 1000 × (100-14.1)/(100-14) = 1000 × 85.9/86 = 998.83...
        val result = RiceCalculator.calcStandardWeight(
            rawWeight = 1000.0,
            moisturePercent = 14.1
        )
        assertEquals(998.83, result, eps)
    }

    @Test
    fun `calcStandardWeight very high moisture 35 percent`() {
        // Lúa vừa gặt, chưa phơi: 35% moisture (hiếm nhưng hợp lệ)
        // 1000 × 65/86 = 755.81...
        val result = RiceCalculator.calcStandardWeight(
            rawWeight = 1000.0,
            moisturePercent = 35.0
        )
        assertEquals(755.81, result, eps)
    }

    @Test
    fun `calcStandardWeight zero raw weight`() {
        assertEquals(0.0, RiceCalculator.calcStandardWeight(0.0, 20.0), eps)
    }

    @Test
    fun `calcStandardWeight negative raw weight propagates through formula`() {
        // Không block negative raw — caller phải guard coerceAtLeast
        val result = RiceCalculator.calcStandardWeight(-100.0, 20.0)
        assertTrue("Kết quả phải âm cho raw âm", result < 0)
    }

    @Test
    fun `calcStandardWeight moisture slightly below zero boundary`() {
        // moisture = -0.001 → invalid → return rawWeight
        assertEquals(500.0, RiceCalculator.calcStandardWeight(500.0, -0.001), eps)
    }

    @Test
    fun `calcStandardWeight moisture 99_9 near upper boundary`() {
        // 99.9% là cực kỳ ướt nhưng vẫn valid
        // 1000 × (100 - 99.9) / (100 - 14) = 1000 × 0.1/86 = 1.16...
        val result = RiceCalculator.calcStandardWeight(1000.0, 99.9)
        assertTrue("Kết quả phải rất nhỏ với moisture 99.9%", result < 2.0)
        assertTrue("Kết quả phải dương", result > 0.0)
    }

    // === calcNetWeight — edge cases ===

    @Test
    fun `calcNetWeight with exactly zero gross returns 0 not negative`() {
        // bag + impurity = rawWeight → gross = 0 → clamp 0
        val result = RiceCalculator.calcNetWeight(
            rawWeight = 100.0,
            bagWeight = 60.0,
            impurityWeight = 40.0,
            moisturePercent = 20.0
        )
        assertEquals(0.0, result, eps)
    }

    @Test
    fun `calcNetWeight result is rounded to 1 decimal`() {
        // 1000 × (100-20)/(100-14) = 930.232... → round(930.232 × 10) / 10 = 930.2
        val result = RiceCalculator.calcNetWeight(
            rawWeight = 1000.0,
            bagWeight = 0.0,
            impurityWeight = 0.0,
            moisturePercent = 20.0
        )
        // 930.23... rounded to 1 decimal = 930.2
        assertEquals(930.2, result, eps)
    }

    @Test
    fun `calcNetWeight large realistic load 5 tons at 18 moisture`() {
        // 5 tấn = 5000 kg thô, bao 50 kg (100 bao × 0.5 kg), tạp 20 kg
        // gross = 5000 - 50 - 20 = 4930 kg
        // W_std = 4930 × (100-18)/(100-14) = 4930 × 82/86 = 4700.697...
        // Làm tròn 1 chữ số: 4700.7
        val result = RiceCalculator.calcNetWeight(
            rawWeight = 5000.0,
            bagWeight = 50.0,
            impurityWeight = 20.0,
            moisturePercent = 18.0
        )
        assertEquals(4700.7, result, eps)
    }

    // === calcRemainingAmount — edge cases ===

    @Test
    fun `calcRemainingAmount with zero deposit`() {
        val r = RiceCalculator.calcRemainingAmount(
            totalAmount = 5_000_000.0,
            paidAmount = 2_000_000.0,
            depositAmount = 0.0
        )
        assertEquals(3_000_000.0, r, eps)
    }

    @Test
    fun `calcRemainingAmount all paid no deposit - result is zero`() {
        val r = RiceCalculator.calcRemainingAmount(
            totalAmount = 10_000_000.0,
            paidAmount = 10_000_000.0,
            depositAmount = 0.0
        )
        assertEquals(0.0, r, eps)
    }

    @Test
    fun `calcRemainingAmount with only deposit paid`() {
        val r = RiceCalculator.calcRemainingAmount(
            totalAmount = 10_000_000.0,
            paidAmount = 0.0,
            depositAmount = 1_000_000.0
        )
        assertEquals(9_000_000.0, r, eps)
    }

    @Test
    fun `calcRemainingAmount all zero returns zero`() {
        assertEquals(0.0, RiceCalculator.calcRemainingAmount(0.0, 0.0, 0.0), eps)
    }

    // === calcTotalAmount — edge cases ===

    @Test
    fun `calcTotalAmount with negative price - guard responsibility on caller`() {
        // RiceCalculator không block giá âm — caller phải validate input
        val result = RiceCalculator.calcTotalAmount(netWeight = 100.0, pricePerKg = -1000.0)
        assertTrue("Giá âm → tổng tiền âm (caller phải block trước)", result < 0)
    }

    @Test
    fun `calcTotalAmount large realistic scenario 5 tons price 8500`() {
        // 4700 kg (sau quy đổi moisture) × 8500 đ/kg = 39_950_000
        val result = RiceCalculator.calcTotalAmount(netWeight = 4700.0, pricePerKg = 8500.0)
        assertEquals(39_950_000.0, result, 1.0)
    }

    // === calcTotalBagWeight — edge cases ===

    @Test
    fun `calcTotalBagWeight method B fallback when sampleCount is 0`() {
        // methodIsSampling=true nhưng sampleCount=0 → fallback về method A
        val total = RiceCalculator.calcTotalBagWeight(
            bagCount = 100,
            bagWeight = 0.5,
            methodIsSampling = true, // bật sampling
            sampleCount = 0,         // nhưng sample count = 0 → fallback
            sampleTotalWeight = 5.0
        )
        // Fallback: 100 × 0.5 = 50
        assertEquals(50.0, total, 0.01)
    }

    @Test
    fun `calcTotalBagWeight method A with zero bag count`() {
        val total = RiceCalculator.calcTotalBagWeight(
            bagCount = 0,
            bagWeight = 0.5,
            methodIsSampling = false,
            sampleCount = 0,
            sampleTotalWeight = 0.0
        )
        assertEquals(0.0, total, 0.01)
    }

    @Test
    fun `calcTotalBagWeight method B with 1 sample bag`() {
        // 1 bao mẫu = 0.45kg → average 0.45/1 → 50 bao × 0.45 = 22.5kg
        val total = RiceCalculator.calcTotalBagWeight(
            bagCount = 50,
            bagWeight = 0.0,
            methodIsSampling = true,
            sampleCount = 1,
            sampleTotalWeight = 0.45
        )
        assertEquals(22.5, total, 0.01)
    }

    // === Full integration: realistic Mekong Delta scenario ===

    @Test
    fun `integration - Dong Thap rice 28 percent moisture 10 ton truck`() {
        // Vụ Hè Thu ở Đồng Tháp, lúa tươi 28% moisture
        // Xe tải 10 tấn: 10000kg thô
        // 200 bao, cân mẫu 10 bao = 5.2kg → avg 0.52kg/bao → tổng bao 104kg
        // Tạp chất = 30kg (dùng kg trực tiếp)
        // moisture = 28%: W_std = rawGross × (100-28)/(100-14) = rawGross × 72/86

        val net = RiceCalculator.calcNetWeightWithModes(
            totalRaw = 10_000.0,
            bagCount = 200,
            bagWeight = 0.0,       // dùng sampling
            bagMethodIsSampling = true,
            bagSampleCount = 10,
            bagSampleTotalWeight = 5.2,   // 10 bao mẫu = 5.2 kg
            impurityValue = 30.0,         // 30 kg tạp chất
            impurityIsPercent = false,
            moisturePercent = 28.0
        )

        // totalBag = (5.2/10) × 200 = 104 kg
        // rawAfterBag = 10000 - 104 = 9896 kg
        // impurity = 30 kg (bỏ qua isPercent)
        // gross = 9896 - 30 = 9866 kg
        // W_std = 9866 × 72/86 = 9866 × 72 / 86 = 710352/86 = 8259.906976...
        // Làm tròn 1 chữ số: 8259.9

        assertEquals(8259.9, net, eps)

        // Tiền: 8259.9 × 8000 đ/kg = 66_079_200
        val total = RiceCalculator.calcTotalAmount(net, 8000.0)
        assertEquals(66_079_200.0, total, 200.0) // sai số ±200đ do làm tròn
    }
}

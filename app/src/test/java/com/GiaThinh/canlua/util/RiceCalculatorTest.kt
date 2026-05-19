package com.GiaThinh.canlua.util

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
        // (1000 - 50 - 10) × 80/86 = 940 × 80/86 = 874.418...
        val result = RiceCalculator.calcNetWeight(
            rawWeight = 1000.0,
            bagWeight = 50.0,
            impurityWeight = 10.0,
            moisturePercent = 20.0
        )
        assertEquals(874.42, result, eps)
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
        // 1000 × 78/86 = 906.976...
        assertEquals(906.98, net, eps)
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
}

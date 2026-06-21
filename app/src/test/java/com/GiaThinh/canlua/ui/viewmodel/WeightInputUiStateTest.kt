package com.giathinh.canlua.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests cho [WeightInputUiState] — đặc biệt các computed property.
 *
 * ⚠️ BUG DOCUMENTATION (#1 — Critical):
 * [WeightInputUiState.netWeight] hiện tại = raw - bag - impurity,
 * KHÔNG quy đổi theo độ ẩm. Điều này gây ra việc UI hiển thị số sai
 * với thực tế tính tiền (khoảng 93 kg/tấn ở moisture 22%).
 *
 * Các test trong group "BUG_DEMO" minh chứng rõ hành vi sai hiện tại.
 * Khi bug được fix, các test đó cần cập nhật expected value.
 */
class WeightInputUiStateTest {

    // === totalWeight ===

    @Test
    fun `totalWeight parses valid double string`() {
        val state = WeightInputUiState(currentWeight = "123.5")
        assertEquals(123.5, state.totalWeight, 0.0)
    }

    @Test
    fun `totalWeight returns 0 for empty string`() {
        val state = WeightInputUiState(currentWeight = "")
        assertEquals(0.0, state.totalWeight, 0.0)
    }

    @Test
    fun `totalWeight returns 0 for non-numeric string`() {
        val state = WeightInputUiState(currentWeight = "abc")
        assertEquals(0.0, state.totalWeight, 0.0)
    }

    @Test
    fun `totalWeight parses integer string correctly`() {
        val state = WeightInputUiState(currentWeight = "1000")
        assertEquals(1000.0, state.totalWeight, 0.0)
    }

    @Test
    fun `totalWeight handles leading dot - dot-only is invalid`() {
        // ".5" is valid double → 0.5
        val state = WeightInputUiState(currentWeight = ".5")
        assertEquals(0.5, state.totalWeight, 0.0)
    }

    // === bagWeightValue ===

    @Test
    fun `bagWeightValue parses valid string`() {
        val state = WeightInputUiState(bagWeight = "2.5")
        assertEquals(2.5, state.bagWeightValue, 0.0)
    }

    @Test
    fun `bagWeightValue returns 0 for empty string`() {
        val state = WeightInputUiState(bagWeight = "")
        assertEquals(0.0, state.bagWeightValue, 0.0)
    }

    @Test
    fun `bagWeightValue default is 0`() {
        // Default state bagWeight = "0" → 0.0
        assertEquals(0.0, WeightInputUiState().bagWeightValue, 0.0)
    }

    // === impurityWeightValue ===

    @Test
    fun `impurityWeightValue parses valid string`() {
        val state = WeightInputUiState(impurityWeight = "5.0")
        assertEquals(5.0, state.impurityWeightValue, 0.0)
    }

    @Test
    fun `impurityWeightValue returns 0 for invalid string`() {
        val state = WeightInputUiState(impurityWeight = "N/A")
        assertEquals(0.0, state.impurityWeightValue, 0.0)
    }

    // === netWeight — current behavior (no moisture conversion) ===

    @Test
    fun `netWeight equals totalWeight minus bag minus impurity`() {
        val state = WeightInputUiState(
            currentWeight = "100.0",
            bagWeight = "5.0",
            impurityWeight = "2.0"
        )
        // Current behavior: 100 - 5 - 2 = 93 (no moisture conversion)
        assertEquals(93.0, state.netWeight, 0.0)
    }

    @Test
    fun `netWeight with no deductions equals totalWeight`() {
        val state = WeightInputUiState(
            currentWeight = "500.0",
            bagWeight = "0",
            impurityWeight = "0"
        )
        assertEquals(500.0, state.netWeight, 0.0)
    }

    @Test
    fun `netWeight is zero when bag plus impurity exceed total - calcNetWeight coerces`() {
        // calcNetWeight coerces gross = totalWeight - bag - impurity to >= 0 before conversion.
        // UI không cần guard thêm — RiceCalculator đã xử lý.
        val state = WeightInputUiState(
            currentWeight = "10.0",
            bagWeight = "8.0",
            impurityWeight = "5.0",
            moisturePercent = 0.0
        )
        assertEquals(0.0, state.netWeight, 0.001)
    }

    @Test
    fun `netWeight zero when all fields empty - defaults to 0`() {
        val state = WeightInputUiState(
            currentWeight = "",
            bagWeight = "0",
            impurityWeight = "0"
        )
        assertEquals(0.0, state.netWeight, 0.0)
    }

    // === netWeight — moisture conversion APPLIED (Bug #1 fixed) ===

    @Test
    fun `netWeight applies moisture conversion via RiceCalculator`() {
        // moisture 22%: 1000 × 78/86 = 906.976... → làm tròn 1 chữ số = 907.0
        val state = WeightInputUiState(
            currentWeight = "1000.0",
            bagWeight = "0",
            impurityWeight = "0",
            moisturePercent = 22.0
        )
        assertEquals(907.0, state.netWeight, 0.1)
    }

    @Test
    fun `netWeight at zero moisture is raw minus deductions without conversion`() {
        // moisture = 0.0 → calcStandardWeight trả thẳng gross (không quy đổi)
        val state = WeightInputUiState(
            currentWeight = "100.0",
            bagWeight = "5.0",
            impurityWeight = "2.0",
            moisturePercent = 0.0
        )
        assertEquals(93.0, state.netWeight, 0.01)
    }

    @Test
    fun `netWeight at standard 14 percent moisture is no-op`() {
        // moisture = 14% (tiêu chuẩn) → (100-14)/(100-14) = 1 → không có thay đổi
        val state = WeightInputUiState(
            currentWeight = "500.0",
            bagWeight = "0",
            impurityWeight = "0",
            moisturePercent = 14.0
        )
        assertEquals(500.0, state.netWeight, 0.01)
    }

    // === isLocked ===

    @Test
    fun `isLocked default is false`() {
        assertEquals(false, WeightInputUiState().isLocked)
    }

    @Test
    fun `copy toggles isLocked correctly`() {
        val state = WeightInputUiState(isLocked = false)
        val toggled = state.copy(isLocked = !state.isLocked)
        assertEquals(true, toggled.isLocked)
    }

    // === Edge cases: large values ===

    @Test
    fun `totalWeight handles large realistic truck load`() {
        // Xe tải lớn ở đồng bằng có thể 15-20 tấn
        val state = WeightInputUiState(currentWeight = "20000.5")
        assertEquals(20000.5, state.totalWeight, 0.0)
    }

    @Test
    fun `netWeight handles realistic rice weighing scenario`() {
        // 100 bao, mỗi bao 50kg gross, bao 0.5kg, tạp 0.2kg/bao
        // (Đây test UiState parse — không phải test công thức ngành)
        val state = WeightInputUiState(
            currentWeight = "5000.0",   // 100 bao × 50kg
            bagWeight = "50.0",          // 100 × 0.5kg
            impurityWeight = "20.0"     // 100 × 0.2kg
        )
        assertEquals(4930.0, state.netWeight, 0.001)
    }
}

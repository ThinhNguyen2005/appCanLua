package com.giathinh.canlua.domain

import com.giathinh.canlua.data.model.Card
import com.giathinh.canlua.data.model.WeightEntry
import com.giathinh.canlua.util.RiceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Regression cho T02 — các lỗi #1 (preview lệch), #2 (tỷ lệ bì), #7 (validation).
 * Viết trước khi nối vào UI/Repository để khóa hành vi đúng.
 */
class WeighDomainTest {

    private fun card(
        bagWeight: Double = 0.0,
        pricePerKg: Double = 8000.0,
        moisture: Double = 14.0
    ) = Card(
        id = 1L,
        name = "Test",
        date = Date(0),
        bagWeight = bagWeight,
        pricePerKg = pricePerKg,
        moisturePercent = moisture
    )

    // === #1 Preview phải khớp quyết toán (trọng lượng thật, không bao trung bình) ===

    @Test
    fun `preview 101 bags matches settlement exactly`() {
        val entries = (List(100) { 50.0 } + listOf(50.5)).mapIndexed { i, w ->
            WeightEntry(id = i + 1L, cardId = 1L, weight = w)
        }
        val settlement = CardSettlement.preview(card(), entries)
        assertEquals(5050.5, settlement.totalNetKg, 0.0001)
        assertEquals(40_404_000L, settlement.totalAmountVnd)
    }

    @Test
    fun `old average-based preview was wrong - documents the regression`() {
        // Logic cũ dựng bao trung bình → làm tròn từng bao → lệch 0.5kg / 4000đ.
        val oldPreview = RiceCalculator.calcNetWeightWithModes(
            totalRaw = 5050.5, bagCount = 101, bagWeight = 0.0,
            bagMethodIsSampling = false, bagSampleCount = 0, bagSampleTotalWeight = 0.0,
            impurityValue = 0.0, impurityIsPercent = false, moisturePercent = 14.0
        )
        assertEquals("Logic cũ cho 5050.0 (sai)", 5050.0, oldPreview, 0.0001)
    }

    // === #2 Chuẩn hóa tỷ lệ bì ===

    @Test
    fun `non-sampling 10 bags per kg gives unit 0_1 and 8kg for 80 bags`() {
        val rule = WeighDefaultsResolver.normalizeBagRule(
            isSampling = false, sampleCount = 10, sampleTotalWeight = 999.0,
            fallbackBagWeight = 0.125
        )
        assertEquals(0.1, rule.bagWeight, 0.0001)
        assertEquals(8.0, rule.bagWeight * 80, 0.0001)
    }

    @Test
    fun `sampling 10 bags 1_2kg gives 9_6kg for 80 bags`() {
        val rule = WeighDefaultsResolver.normalizeBagRule(
            isSampling = true, sampleCount = 10, sampleTotalWeight = 1.2,
            fallbackBagWeight = 0.125
        )
        assertEquals(0.12, rule.bagWeight, 0.0001)
        assertEquals(9.6, rule.bagWeight * 80, 0.0001)
    }

    @Test
    fun `legacy count zero keeps fallback bagWeight`() {
        val rule = WeighDefaultsResolver.normalizeBagRule(
            isSampling = false, sampleCount = 0, sampleTotalWeight = 0.0,
            fallbackBagWeight = 0.125
        )
        assertEquals(0.125, rule.bagWeight, 0.0001)
    }

    // === #7 Parser và validation ===

    @Test
    fun `parse accepts comma and dot decimal equivalently`() {
        assertEquals(18.5, (WeighValidation.parseNumber("18,5") as WeighValidation.ParsedNumber.Value).value, 0.0001)
        assertEquals(18.5, (WeighValidation.parseNumber(" 18.5 ") as WeighValidation.ParsedNumber.Value).value, 0.0001)
    }

    @Test
    fun `parse distinguishes blank from invalid`() {
        assertTrue(WeighValidation.parseNumber("   ") is WeighValidation.ParsedNumber.Blank)
        assertTrue(WeighValidation.parseNumber(null) is WeighValidation.ParsedNumber.Blank)
        assertTrue(WeighValidation.parseNumber("1.2,3") is WeighValidation.ParsedNumber.Invalid)
        assertTrue(WeighValidation.parseNumber("1.2.3") is WeighValidation.ParsedNumber.Invalid)
        assertTrue(WeighValidation.parseNumber("abc") is WeighValidation.ParsedNumber.Invalid)
    }

    @Test
    fun `parse rejects NaN and Infinity`() {
        assertTrue(WeighValidation.parseNumber("NaN") is WeighValidation.ParsedNumber.Invalid)
        assertTrue(WeighValidation.parseNumber("Infinity") is WeighValidation.ParsedNumber.Invalid)
        assertNull(WeighValidation.parseOrNull("NaN"))
    }

    @Test
    fun `weight bounds 200 valid 200_01 invalid`() {
        assertTrue(WeighValidation.isValidWeight(200.0))
        assertFalse(WeighValidation.isValidWeight(200.01))
        assertFalse(WeighValidation.isValidWeight(0.0))
        assertFalse(WeighValidation.isValidWeight(-1.0))
        assertFalse(WeighValidation.isValidWeight(Double.NaN))
    }

    @Test
    fun `moisture bounds 0 valid 100 invalid`() {
        assertTrue(WeighValidation.isValidMoisture(0.0))
        assertTrue(WeighValidation.isValidMoisture(99.9))
        assertFalse(WeighValidation.isValidMoisture(100.0))
        assertFalse(WeighValidation.isValidMoisture(-0.1))
    }

    @Test
    fun `impurity percent capped at 100 kg non-negative`() {
        assertTrue(WeighValidation.isValidImpurity(2.0, isPercent = true))
        assertFalse(WeighValidation.isValidImpurity(101.0, isPercent = true))
        assertTrue(WeighValidation.isValidImpurity(500.0, isPercent = false))
        assertFalse(WeighValidation.isValidImpurity(-1.0, isPercent = false))
    }

    @Test
    fun `payment must be positive money non-negative`() {
        assertTrue(WeighValidation.isValidPositivePayment(1000.0))
        assertFalse(WeighValidation.isValidPositivePayment(0.0))
        assertTrue(WeighValidation.isValidNonNegativeMoney(0.0))
        assertFalse(WeighValidation.isValidNonNegativeMoney(-5.0))
    }
}
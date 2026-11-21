package com.giathinh.canlua.ui.component.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeighOptionsValidatorTest {

    @Test
    fun sanitizeDecimalInput_keepsSingleDotAndDigitsOnly() {
        assertEquals("12.5", sanitizeDecimalInput("12.5"))
        assertEquals("12.5", sanitizeDecimalInput("12,5"))
        assertEquals("1.23", sanitizeDecimalInput("1..2,3"))
        assertEquals("450", sanitizeDecimalInput(" 45a0 kg "))
    }

    @Test
    fun validateBagTare_singleBagMode_acceptsPositiveCount() {
        val result = validateBagTare(
            isSampling = false,
            bagsPerKgText = "8",
            sampleCountText = "",
            sampleWeightText = ""
        )

        assertTrue(result.isValid)
        assertEquals(8, result.sampleCount)
        assertEquals(0.0, result.sampleTotalWeight, 0.0)
        assertFalse(result.countError)
        assertFalse(result.weightError)
    }

    @Test
    fun validateBagTare_singleBagMode_rejectsBlankZeroOrNonNumeric() {
        listOf("", "0", "abc").forEach { invalid ->
            val result = validateBagTare(
                isSampling = false,
                bagsPerKgText = invalid,
                sampleCountText = "",
                sampleWeightText = ""
            )

            assertFalse(result.isValid)
            assertTrue(result.countError)
            assertFalse(result.weightError)
        }
    }

    @Test
    fun validateBagTare_samplingMode_acceptsPositiveCountAndWeight() {
        val result = validateBagTare(
            isSampling = true,
            bagsPerKgText = "8",
            sampleCountText = "10",
            sampleWeightText = "1.25"
        )

        assertTrue(result.isValid)
        assertEquals(10, result.sampleCount)
        assertEquals(1.25, result.sampleTotalWeight, 0.001)
        assertFalse(result.countError)
        assertFalse(result.weightError)
    }

    @Test
    fun validateBagTare_samplingMode_rejectsZeroCountOrWeightOrBlankBoth() {
        val zeroCount = validateBagTare(
            isSampling = true,
            bagsPerKgText = "8",
            sampleCountText = "0",
            sampleWeightText = "1.25"
        )
        assertFalse(zeroCount.isValid)
        assertTrue(zeroCount.countError)
        assertFalse(zeroCount.weightError)

        val zeroWeight = validateBagTare(
            isSampling = true,
            bagsPerKgText = "8",
            sampleCountText = "10",
            sampleWeightText = "0"
        )
        assertFalse(zeroWeight.isValid)
        assertFalse(zeroWeight.countError)
        assertTrue(zeroWeight.weightError)

        val blankWeight = validateBagTare(
            isSampling = true,
            bagsPerKgText = "8",
            sampleCountText = "10",
            sampleWeightText = ""
        )
        assertFalse(blankWeight.isValid)
        assertFalse(blankWeight.countError)
        assertTrue(blankWeight.weightError)

        val blankBoth = validateBagTare(
            isSampling = true,
            bagsPerKgText = "8",
            sampleCountText = "",
            sampleWeightText = ""
        )
        assertFalse(blankBoth.isValid)
        assertTrue(blankBoth.countError)
        assertTrue(blankBoth.weightError)
    }
}

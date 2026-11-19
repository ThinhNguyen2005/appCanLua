package com.giathinh.canlua.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleReadingStabilizerTest {

    private var fakeNowMs = 0L
    private val stabilizer = ScaleReadingStabilizer(clock = ScaleClock { fakeNowMs })

    @Test
    fun `auto capture waits full 700ms within 0_05kg tolerance before capturing`() {
        fakeNowMs = 1_000L
        assertNull(stabilizer.onReading(ScaleReading(52.10, null, "52.10"), ScaleCaptureMode.AUTO_CAPTURE).capturedWeightKg)

        // Fluctuates by 0.03 kg at 1400 ms -> still under 700 ms window
        fakeNowMs = 1_400L
        val mid = stabilizer.onReading(ScaleReading(52.13, null, "52.13"), ScaleCaptureMode.AUTO_CAPTURE)
        assertFalse(mid.isStable)
        assertNull(mid.capturedWeightKg)

        // At 1699 ms (span = 699 ms < 700 ms) -> still must NOT capture
        fakeNowMs = 1_699L
        assertNull(stabilizer.onReading(ScaleReading(52.11, null, "52.11"), ScaleCaptureMode.AUTO_CAPTURE).capturedWeightKg)

        // At 1700 ms (span = 700 ms >= 700 ms, spread 0.03 <= 0.05) -> captures
        fakeNowMs = 1_700L
        val captured = stabilizer.onReading(ScaleReading(52.12, null, "52.12"), ScaleCaptureMode.AUTO_CAPTURE)
        assertTrue(captured.isStable)
        assertTrue(captured.isWaitingForZero)
        assertEquals(52.12, captured.capturedWeightKg ?: 0.0, 0.001)
    }

    @Test
    fun `auto capture rejects unstable oscillation and weights below 5kg or above 200kg`() {
        val rejected = listOf(
            ScaleReading(0.0, true, "0.0"),
            ScaleReading(4.99, true, "4.99"),
            ScaleReading(205.0, true, "205.0"),
            ScaleReading(52.0, false, "US 52.0")
        )
        rejected.forEach { reading ->
            fakeNowMs += 1_000L
            val out = stabilizer.onReading(reading, ScaleCaptureMode.AUTO_CAPTURE)
            assertFalse("Expected unstable for $reading", out.isStable)
            assertNull("Expected no capture for $reading", out.capturedWeightKg)
        }

        // Sudden jump resets 700ms timer so old readings don't count toward new plateau
        fakeNowMs = 10_000L
        assertNull(stabilizer.onReading(ScaleReading(50.0, null, "50.0"), ScaleCaptureMode.AUTO_CAPTURE).capturedWeightKg)
        fakeNowMs = 10_500L
        assertNull(stabilizer.onReading(ScaleReading(52.0, null, "52.0"), ScaleCaptureMode.AUTO_CAPTURE).capturedWeightKg)
        fakeNowMs = 10_800L // Only 300ms since 52.0 arrived
        assertNull(stabilizer.onReading(ScaleReading(52.0, null, "52.0"), ScaleCaptureMode.AUTO_CAPTURE).capturedWeightKg)
        fakeNowMs = 11_200L // Now 700ms since 52.0 arrived
        val stable = stabilizer.onReading(ScaleReading(52.0, null, "52.0"), ScaleCaptureMode.AUTO_CAPTURE)
        assertEquals(52.0, stable.capturedWeightKg ?: 0.0, 0.001)
    }

    @Test
    fun `waiting for zero prevents duplicate capture until scale drops below 2kg then captures second bag`() {
        // Bag 1 stabilized via hardware ST flag at 52.1 kg -> auto captures and locks in WAITING_FOR_ZERO
        val bag1 = stabilizer.onReading(ScaleReading(52.1, true, "ST 52.1"), ScaleCaptureMode.AUTO_CAPTURE)
        assertEquals(52.1, bag1.capturedWeightKg ?: 0.0, 0.001)
        assertTrue(bag1.isWaitingForZero)

        // Still holding 52.1 -> 25.0 -> 2.0 (>= 2.0 kg threshold): MUST stay locked, never duplicate
        listOf(52.1, 52.1, 25.0, 2.0).forEach { weight ->
            fakeNowMs += 1_000L
            val held = stabilizer.onReading(ScaleReading(weight, true, "ST $weight"), ScaleCaptureMode.AUTO_CAPTURE)
            assertTrue("Should stay waiting for zero at $weight kg", held.isWaitingForZero)
            assertNull("Must not re-capture at $weight kg", held.capturedWeightKg)
        }

        // Scale drops < 2.0 kg -> unlocks ready for bag 2
        val zeroed = stabilizer.onReading(ScaleReading(1.8, false, "US 1.8"), ScaleCaptureMode.AUTO_CAPTURE)
        assertFalse(zeroed.isWaitingForZero)
        assertNull(zeroed.capturedWeightKg)

        // Bag 2 placed at 48.5 kg -> captures bag 2
        val bag2 = stabilizer.onReading(ScaleReading(48.5, true, "ST 48.5"), ScaleCaptureMode.AUTO_CAPTURE)
        assertEquals(48.5, bag2.capturedWeightKg ?: 0.0, 0.001)
        assertTrue(bag2.isWaitingForZero)
    }

    @Test
    fun `semi auto tracks stability without auto capturing`() {
        val out = stabilizer.onReading(ScaleReading(51.4, true, "ST 51.4"), ScaleCaptureMode.SEMI_AUTO)
        assertEquals(51.4, out.liveWeightKg, 0.001)
        assertTrue(out.isStable)
        assertFalse(out.isWaitingForZero)
        assertNull(out.capturedWeightKg)
    }
}

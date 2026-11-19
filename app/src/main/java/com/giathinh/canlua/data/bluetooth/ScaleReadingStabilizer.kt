package com.giathinh.canlua.data.bluetooth

import com.giathinh.canlua.domain.WeighValidation

/**
 * Monotonic or wall-clock time provider extracted for deterministic unit testing.
 */
fun interface ScaleClock {
    fun nowMs(): Long

    companion object {
        val SYSTEM: ScaleClock = ScaleClock { System.currentTimeMillis() }
    }
}

/**
 * Snapshot emitted by [ScaleReadingStabilizer] after processing a reading.
 */
data class StabilizerOutput(
    val liveWeightKg: Double,
    val isStable: Boolean,
    val isWaitingForZero: Boolean,
    val capturedWeightKg: Double?
)

/**
 * Filters jittery raw scale readings, detects true bag stability (`isStable == true` or sustained
 * readings within `0.05 kg` over `700 ms`), and enforces zero-return hysteresis in [ScaleCaptureMode.AUTO_CAPTURE]
 * so the same bag is never recorded twice.
 */
class ScaleReadingStabilizer(
    private val clock: ScaleClock = ScaleClock.SYSTEM
) {
    private data class TimedSample(
        val weightKg: Double,
        val timestampMs: Long
    )

    private val recentSamples = ArrayDeque<TimedSample>()
    private var isWaitingForZero = false

    fun reset() {
        recentSamples.clear()
        isWaitingForZero = false
    }

    fun clearPendingWindow() {
        recentSamples.clear()
    }

    fun onReading(reading: ScaleReading, mode: ScaleCaptureMode): StabilizerOutput {
        val nowMs = clock.nowMs()
        val weight = reading.weightKg

        if (isWaitingForZero && mode == ScaleCaptureMode.SEMI_AUTO) {
            isWaitingForZero = false
            recentSamples.clear()
        }

        if (isWaitingForZero) {
            return handleWaitingForZero(weight)
        }

        if (weight < MIN_CAPTURE_WEIGHT_KG || reading.isStable == false || weight > WeighValidation.MAX_BAG_KG) {
            recentSamples.clear()
            return StabilizerOutput(
                liveWeightKg = weight,
                isStable = false,
                isWaitingForZero = false,
                capturedWeightKg = null
            )
        }

        val stableNow = evaluateStability(weight, reading.isStable, nowMs)
        if (!stableNow) {
            return StabilizerOutput(
                liveWeightKg = weight,
                isStable = false,
                isWaitingForZero = false,
                capturedWeightKg = null
            )
        }

        return if (mode == ScaleCaptureMode.AUTO_CAPTURE) {
            isWaitingForZero = true
            recentSamples.clear()
            StabilizerOutput(
                liveWeightKg = weight,
                isStable = true,
                isWaitingForZero = true,
                capturedWeightKg = weight
            )
        } else {
            StabilizerOutput(
                liveWeightKg = weight,
                isStable = true,
                isWaitingForZero = false,
                capturedWeightKg = null
            )
        }
    }

    private fun handleWaitingForZero(weight: Double): StabilizerOutput {
        if (weight < ZERO_RETURN_THRESHOLD_KG) {
            isWaitingForZero = false
            recentSamples.clear()
        }
        return StabilizerOutput(
            liveWeightKg = weight,
            isStable = false,
            isWaitingForZero = isWaitingForZero,
            capturedWeightKg = null
        )
    }

    private fun evaluateStability(weight: Double, explicitStable: Boolean?, nowMs: Long): Boolean {
        if (explicitStable == true) {
            return true
        }
        recentSamples.addLast(TimedSample(weight, nowMs))
        while (recentSamples.isNotEmpty() && !isWindowWithinTolerance()) {
            recentSamples.removeFirst()
        }
        if (recentSamples.size < 2) return false
        val spanMs = recentSamples.last().timestampMs - recentSamples.first().timestampMs
        return spanMs >= STABILITY_WINDOW_MS
    }

    private fun isWindowWithinTolerance(): Boolean {
        val min = recentSamples.minOf { it.weightKg }
        val max = recentSamples.maxOf { it.weightKg }
        return (max - min) <= STABILITY_TOLERANCE_KG
    }

    companion object {
        const val MIN_CAPTURE_WEIGHT_KG = 5.0
        const val ZERO_RETURN_THRESHOLD_KG = 2.0
        const val STABILITY_WINDOW_MS = 700L
        const val STABILITY_TOLERANCE_KG = 0.05
    }
}

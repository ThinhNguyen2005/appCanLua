package com.giathinh.canlua.data.bluetooth

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-only mock weighing scale stream simulating realistic bag weighing cycles:
 * `0.0 -> 18.4 -> 54.2 -> 52.1 -> 52.1 -> 52.1 (stable) -> 0.0`, followed by subsequent cycles
 * so UI, Auto-Capture, Semi-Auto, hysteresis, and TTS can be verified without physical hardware.
 */
@Singleton
class MockScaleClient @Inject constructor(
    private val parser: ScaleProtocolParser
) {
    fun observeSimulatedReadings(): Flow<ScaleReading> = flow {
        val bagCycles = listOf(
            listOf(
                "ST,GS,+  0.00kg",
                "US,GS,+ 18.40kg",
                "US,GS,+ 54.20kg",
                "US,GS,+ 52.10kg",
                "US,GS,+ 52.10kg",
                "ST,GS,+ 52.10kg",
                "ST,GS,+  0.00kg"
            ),
            listOf(
                "US,GS,+ 22.60kg",
                "US,GS,+ 49.90kg",
                "US,GS,+ 48.50kg",
                "US,GS,+ 48.50kg",
                "ST,GS,+ 48.50kg",
                "ST,GS,+  0.00kg"
            ),
            listOf(
                "US,GS,+ 31.20kg",
                "US,GS,+ 55.00kg",
                "US,GS,+ 53.40kg",
                "US,GS,+ 53.40kg",
                "ST,GS,+ 53.40kg",
                "ST,GS,+  0.00kg"
            )
        )

        var cycleIdx = 0
        while (currentCoroutineContext().isActive) {
            val frames = bagCycles[cycleIdx % bagCycles.size]
            for (raw in frames) {
                if (!currentCoroutineContext().isActive) break
                parser.parse(raw)?.let { emit(it) }
                delay(SAMPLE_INTERVAL_MS)
            }
            delay(INTER_BAG_PAUSE_MS)
            cycleIdx++
        }
    }

    private companion object {
        const val SAMPLE_INTERVAL_MS = 450L
        const val INTER_BAG_PAUSE_MS = 1_200L
    }
}

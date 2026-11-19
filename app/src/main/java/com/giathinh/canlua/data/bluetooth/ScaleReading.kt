package com.giathinh.canlua.data.bluetooth

/**
 * Single weight sample decoded from an electronic scale serial stream.
 *
 * @param weightKg Weight reading in kilograms.
 * @param isStable Hardware stability flag when reported by the indicator (`ST` = true, `US`/`MO` = false),
 *                 or `null` when the frame carries no stability flag.
 * @param rawLine Trimmed raw frame line received from the scale.
 */
data class ScaleReading(
    val weightKg: Double,
    val isStable: Boolean?,
    val rawLine: String
)

/**
 * Paired Bluetooth Classic device candidate or debug mock scale shown in the connection sheet.
 */
data class PairedScaleDevice(
    val name: String,
    val address: String,
    val isMock: Boolean = false
)

/**
 * Bag capture behavior when connected to a scale.
 */
enum class ScaleCaptureMode {
    /** Updates live weight on UI; operator taps once to confirm/capture each bag. */
    SEMI_AUTO,

    /** Automatically records the bag once weight is valid and stable, then waits for zero-return. */
    AUTO_CAPTURE
}

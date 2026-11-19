package com.giathinh.canlua.data.bluetooth

import androidx.annotation.StringRes

/**
 * Connection lifecycle and live telemetry state of the Bluetooth/mock electronic scale.
 */
sealed interface ScaleConnectionState {
    data object Disconnected : ScaleConnectionState

    data class Connecting(
        val deviceName: String
    ) : ScaleConnectionState

    data class Connected(
        val deviceName: String,
        val liveWeightKg: Double = 0.0,
        val isStable: Boolean = false,
        val isWaitingForZero: Boolean = false,
        val isMock: Boolean = false
    ) : ScaleConnectionState

    data class Error(
        @param:StringRes val messageResId: Int
    ) : ScaleConnectionState
}

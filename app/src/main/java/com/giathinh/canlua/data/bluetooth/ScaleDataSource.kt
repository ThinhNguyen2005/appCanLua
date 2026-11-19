package com.giathinh.canlua.data.bluetooth

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow
import java.io.IOException

/**
 * Typed IO exception carrying a localized user-facing string resource for scale connection failures.
 */
class ScaleConnectionException(
    @param:StringRes val messageResId: Int,
    cause: Throwable? = null
) : IOException(cause)

/**
 * Abstraction for streaming weight readings from an external Bluetooth Classic SPP scale or fake in tests.
 */
interface ScaleDataSource {
    fun hasRequiredPermissions(): Boolean
    fun isBluetoothSupported(): Boolean
    fun isBluetoothEnabled(): Boolean
    fun getPairedDevices(): List<PairedScaleDevice>
    fun connect(device: PairedScaleDevice): Flow<ScaleReading>
    fun disconnect()
}

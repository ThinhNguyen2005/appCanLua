package com.giathinh.canlua.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.giathinh.canlua.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bluetooth Classic Serial Port Profile (RFCOMM SPP) client for reading continuous weight frames
 * from agricultural/industrial digital scale indicators.
 */
@Singleton
class BluetoothClassicScaleClient @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val parser: ScaleProtocolParser
) : ScaleDataSource {

    private val activeSocket = AtomicReference<BluetoothSocket?>(null)

    private fun bluetoothAdapter(): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    override fun isBluetoothSupported(): Boolean = bluetoothAdapter() != null

    override fun isBluetoothEnabled(): Boolean = bluetoothAdapter()?.isEnabled == true

    override fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
                hasPermission(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            hasPermission(Manifest.permission.BLUETOOTH) &&
                hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.BLUETOOTH)
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override fun getPairedDevices(): List<PairedScaleDevice> {
        if (!hasConnectPermission()) return emptyList()
        val adapter = bluetoothAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices
                .orEmpty()
                .map { device ->
                    PairedScaleDevice(
                        name = device.name?.takeIf { it.isNotBlank() } ?: device.address,
                        address = device.address,
                        isMock = false
                    )
                }
                .sortedBy { it.name.lowercase() }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(device: PairedScaleDevice): Flow<ScaleReading> = flow {
        val adapter = validateReadyAdapter()
        val remoteDevice = try {
            adapter.getRemoteDevice(device.address)
        } catch (e: IllegalArgumentException) {
            throw ScaleConnectionException(R.string.scale_error_device_not_found, e)
        }

        disconnect()
        val socket = openSppSocket(adapter, remoteDevice.address)
        activeSocket.set(socket)

        try {
            val stream = socket.inputStream
            emitParsedLines(stream) { reading -> emit(reading) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw ScaleConnectionException(R.string.scale_error_connection_lost, e)
        } finally {
            closeSocketSafely(socket)
            activeSocket.compareAndSet(socket, null)
        }
    }.flowOn(Dispatchers.IO)

    override fun disconnect() {
        val existing = activeSocket.getAndSet(null)
        closeSocketSafely(existing)
    }

    private fun validateReadyAdapter(): BluetoothAdapter {
        val adapter = bluetoothAdapter()
        val errorResId = when {
            !hasConnectPermission() -> R.string.scale_error_permission_required
            adapter == null -> R.string.scale_error_bluetooth_unsupported
            !adapter.isEnabled -> R.string.scale_error_bluetooth_disabled
            else -> null
        }
        if (errorResId != null) {
            throw ScaleConnectionException(errorResId)
        }
        return requireNotNull(adapter)
    }

    @SuppressLint("MissingPermission")
    private fun openSppSocket(adapter: BluetoothAdapter, address: String): BluetoothSocket {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            runCatching { adapter.cancelDiscovery() }
        }
        var socket: BluetoothSocket? = null
        return try {
            socket = adapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            socket
        } catch (e: SecurityException) {
            closeSocketSafely(socket)
            throw ScaleConnectionException(R.string.scale_error_permission_required, e)
        } catch (e: IOException) {
            closeSocketSafely(socket)
            throw ScaleConnectionException(R.string.scale_error_connection_failed, e)
        }
    }

    private suspend fun emitParsedLines(
        stream: InputStream,
        onReading: suspend (ScaleReading) -> Unit
    ) {
        val reader = InputStreamReader(stream, Charsets.US_ASCII)
        val lineBuffer = StringBuilder(INITIAL_LINE_CAPACITY)
        while (currentCoroutineContext().isActive) {
            val chInt = reader.read()
            if (chInt == -1) {
                throw ScaleConnectionException(R.string.scale_error_connection_lost)
            }
            val ch = chInt.toChar()
            if (ch == '\r' || ch == '\n') {
                if (lineBuffer.isNotEmpty()) {
                    val line = lineBuffer.toString()
                    lineBuffer.setLength(0)
                    parser.parse(line)?.let { onReading(it) }
                }
            } else if (lineBuffer.length < MAX_LINE_LENGTH) {
                lineBuffer.append(ch)
            }
        }
    }

    private fun closeSocketSafely(socket: BluetoothSocket?) {
        if (socket != null) {
            runCatching { socket.close() }
        }
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val INITIAL_LINE_CAPACITY = 64
        private const val MAX_LINE_LENGTH = 256
    }
}

package com.giathinh.canlua.ui.component.weight

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.BuildConfig
import com.giathinh.canlua.R
import com.giathinh.canlua.data.bluetooth.PairedScaleDevice
import com.giathinh.canlua.data.bluetooth.ScaleCaptureMode
import com.giathinh.canlua.data.bluetooth.ScaleConnectionState
import com.giathinh.canlua.domain.WeighValidation
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.util.HapticUtil
import java.util.Locale

private fun requiredBluetoothPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScaleSheet(
    connectionState: ScaleConnectionState,
    captureMode: ScaleCaptureMode,
    pairedDevices: List<PairedScaleDevice>,
    hasBluetoothPermission: Boolean,
    isBluetoothEnabled: Boolean,
    onRefreshDevices: () -> Unit,
    onSelectCaptureMode: (ScaleCaptureMode) -> Unit,
    onConnectDevice: (PairedScaleDevice) -> Unit,
    onConnectMockScale: (String) -> Unit,
    onDisconnect: () -> Unit,
    onCaptureNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val context = LocalContext.current
    val latestRefreshDevices by rememberUpdatedState(onRefreshDevices)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        latestRefreshDevices()
    }

    LaunchedEffect(Unit) {
        latestRefreshDevices()
        if (!hasBluetoothPermission) {
            permissionLauncher.launch(requiredBluetoothPermissions())
        }
    }

    val mockDeviceLabel = stringResource(R.string.scale_mock_device_name)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.scale_sheet_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.scale_sheet_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }

            if (connectionState !is ScaleConnectionState.Disconnected) {
                ActiveScaleCard(
                    connectionState = connectionState,
                    captureMode = captureMode,
                    onCaptureNow = {
                        HapticUtil.confirm(context)
                        onCaptureNow()
                    },
                    onDisconnect = {
                        HapticUtil.tick(context)
                        onDisconnect()
                    }
                )
            }

            CaptureModeSection(
                captureMode = captureMode,
                onSelectCaptureMode = { mode ->
                    HapticUtil.tick(context)
                    onSelectCaptureMode(mode)
                }
            )

            if (!hasBluetoothPermission) {
                PermissionPromptCard(
                    onRequestPermission = {
                        HapticUtil.tick(context)
                        permissionLauncher.launch(requiredBluetoothPermissions())
                    }
                )
            } else if (!isBluetoothEnabled) {
                BluetoothDisabledCard()
            }

            PairedDevicesSection(
                pairedDevices = pairedDevices,
                connectionState = connectionState,
                onRefreshDevices = {
                    HapticUtil.tick(context)
                    onRefreshDevices()
                },
                onConnectDevice = { device ->
                    HapticUtil.tick(context)
                    onConnectDevice(device)
                }
            )

            if (BuildConfig.DEBUG) {
                HorizontalDivider(color = AppColors.Divider)
                MockScaleDebugCard(
                    onUseMockScale = {
                        HapticUtil.confirm(context)
                        onConnectMockScale(mockDeviceLabel)
                    }
                )
            }
        }
    }
}

@Composable
private fun ActiveScaleCard(
    connectionState: ScaleConnectionState,
    captureMode: ScaleCaptureMode,
    onCaptureNow: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when (connectionState) {
            is ScaleConnectionState.Error -> AppColors.Error.copy(alpha = 0.08f)
            is ScaleConnectionState.Connected -> AppColors.GreenSurface
            else -> AppColors.SurfaceContainer
        },
        border = BorderStroke(
            width = 1.dp,
            color = when (connectionState) {
                is ScaleConnectionState.Error -> AppColors.Error.copy(alpha = 0.4f)
                is ScaleConnectionState.Connected -> AppColors.GreenPrimary.copy(alpha = 0.5f)
                else -> AppColors.Divider
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (connectionState) {
                is ScaleConnectionState.Connecting -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = AppColors.Warning
                        )
                        Text(
                            text = stringResource(R.string.scale_status_connecting, connectionState.deviceName),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AppColors.TextPrimary
                        )
                    }
                }

                is ScaleConnectionState.Connected -> {
                    ConnectedScaleBody(
                        state = connectionState,
                        captureMode = captureMode,
                        onCaptureNow = onCaptureNow
                    )
                }

                is ScaleConnectionState.Error -> {
                    Text(
                        text = stringResource(connectionState.messageResId),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = AppColors.Error
                    )
                }

                ScaleConnectionState.Disconnected -> Unit
            }

            OutlinedButton(
                onClick = onDisconnect,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.scale_disconnect_button),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ConnectedScaleBody(
    state: ScaleConnectionState.Connected,
    captureMode: ScaleCaptureMode,
    onCaptureNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weightStr = "%.1f".format(Locale.US, state.liveWeightKg).replace('.', ',')
    val canCapture = captureMode == ScaleCaptureMode.SEMI_AUTO &&
        WeighValidation.isValidWeight(state.liveWeightKg)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.BluetoothConnected,
                    contentDescription = null,
                    tint = AppColors.GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.scale_connected_device_label, state.deviceName),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stringResource(R.string.scale_status_live_weight, weightStr),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = AppColors.GreenPrimary
            )
        }

        if (state.isWaitingForZero) {
            Text(
                text = stringResource(R.string.scale_status_waiting_zero),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Warning
            )
        } else if (state.isStable) {
            Text(
                text = stringResource(R.string.scale_status_live_stable, weightStr),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.GreenPrimary
            )
        }

        if (captureMode == ScaleCaptureMode.SEMI_AUTO) {
            Button(
                onClick = onCaptureNow,
                enabled = canCapture,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.GreenPrimary,
                    contentColor = AppColors.OnGreenPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = if (canCapture) {
                        stringResource(R.string.scale_capture_button, weightStr)
                    } else {
                        stringResource(R.string.scale_capture_button_idle)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CaptureModeSection(
    captureMode: ScaleCaptureMode,
    onSelectCaptureMode: (ScaleCaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val segmentedColors = SegmentedButtonDefaults.colors(
        activeContainerColor = AppColors.GreenPrimary,
        activeContentColor = AppColors.OnGreenPrimary,
        activeBorderColor = AppColors.GreenPrimary,
        inactiveContainerColor = AppColors.SurfaceContainer,
        inactiveContentColor = AppColors.TextSecondary,
        inactiveBorderColor = AppColors.Divider
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.scale_mode_label),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.TextPrimary
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val modes = listOf(ScaleCaptureMode.SEMI_AUTO, ScaleCaptureMode.AUTO_CAPTURE)
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = captureMode == mode,
                    onClick = { onSelectCaptureMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    colors = segmentedColors,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = if (mode == ScaleCaptureMode.SEMI_AUTO) {
                            stringResource(R.string.scale_mode_semi_auto)
                        } else {
                            stringResource(R.string.scale_mode_auto_capture)
                        },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(
            text = if (captureMode == ScaleCaptureMode.SEMI_AUTO) {
                stringResource(R.string.scale_mode_semi_auto_desc)
            } else {
                stringResource(R.string.scale_mode_auto_desc)
            },
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun PermissionPromptCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppColors.Warning.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, AppColors.Warning.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.scale_sheet_permission_banner_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.scale_sheet_permission_banner_desc),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
            FilledTonalButton(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = stringResource(R.string.scale_sheet_grant_permission),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BluetoothDisabledCard(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppColors.SurfaceContainer,
        border = BorderStroke(1.dp, AppColors.Divider),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BluetoothDisabled,
                contentDescription = null,
                tint = AppColors.Warning
            )
            Text(
                text = stringResource(R.string.scale_sheet_bt_off_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary
            )
        }
    }
}

@Composable
private fun PairedDevicesSection(
    pairedDevices: List<PairedScaleDevice>,
    connectionState: ScaleConnectionState,
    onRefreshDevices: () -> Unit,
    onConnectDevice: (PairedScaleDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDeviceName = (connectionState as? ScaleConnectionState.Connected)?.deviceName
        ?: (connectionState as? ScaleConnectionState.Connecting)?.deviceName

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.scale_bonded_devices_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AppColors.TextPrimary
            )
            TextButton(
                onClick = onRefreshDevices,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.scale_refresh_devices))
            }
        }

        if (pairedDevices.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppColors.SurfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.scale_bonded_devices_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            pairedDevices.forEach { device ->
                val isSelected = activeDeviceName == device.name &&
                    connectionState !is ScaleConnectionState.Error
                val connectDesc = stringResource(R.string.scale_accessibility_connect_device, device.name)
                Surface(
                    onClick = { onConnectDevice(device) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) AppColors.GreenSurface else AppColors.CardBg,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) AppColors.GreenPrimary else AppColors.Divider
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = connectDesc
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) AppColors.GreenPrimary.copy(alpha = 0.15f)
                                    else AppColors.SurfaceContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isSelected) AppColors.GreenPrimary else AppColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = AppColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = device.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MockScaleDebugCard(
    onUseMockScale: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onUseMockScale,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.scale_debug_mock_button),
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = stringResource(R.string.scale_debug_mock_desc),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}
